import { onRequest, Request } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import type { Response } from "express";

initializeApp();

const openRouterApiKey = defineSecret("OPENROUTER_API_KEY");
const tmdbApiKey = defineSecret("TMDB_API_KEY");

/** Model is pinned server-side — the client can't request a different (possibly pricier) model. */
const MODEL = "openai/gpt-oss-120b";
const OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
const TMDB_ENDPOINT = "https://api.themoviedb.org/3";
const TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500";
const WIKIPEDIA_USER_AGENT = "SaveableApp/1.0 (https://github.com/rainyday/saveable-app)";

/** Calls per day for a non-premium user before they're rejected with 429 — shared by aiParse and enrichItem. */
const FREE_DAILY_LIMIT = 20;

/**
 * Verifies the caller's Firebase ID token and, for non-premium users, atomically checks & increments
 * their per-day Firestore call counter. On success returns their uid; on failure it writes the
 * appropriate error status/body to [res] itself and returns null, so the caller just needs to bail out.
 */
async function checkAuthAndQuota(req: Request, res: Response): Promise<string | null> {
  const authHeader = req.get("Authorization") ?? "";
  const bearerMatch = authHeader.match(/^Bearer (.+)$/);
  if (!bearerMatch) {
    res.status(401).json({ error: "Missing Authorization bearer token." });
    return null;
  }

  let uid: string;
  try {
    const decoded = await getAuth().verifyIdToken(bearerMatch[1]);
    uid = decoded.uid;
  } catch (err) {
    logger.warn("Rejected request with invalid ID token", err);
    res.status(401).json({ error: "Your sign-in has expired. Please sign in again." });
    return null;
  }

  const db = getFirestore();
  const userSnap = await db.collection("users").doc(uid).get();
  const isPremium = userSnap.exists && userSnap.data()?.isPremium === true;

  if (!isPremium) {
    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD, UTC
    const counterRef = db.collection("usage").doc(uid).collection("daily").doc(today);

    const allowed = await db.runTransaction(async (tx) => {
      const snap = await tx.get(counterRef);
      const current = (snap.data()?.count as number | undefined) ?? 0;
      if (current >= FREE_DAILY_LIMIT) return false;
      tx.set(
        counterRef,
        { count: current + 1, updatedAt: FieldValue.serverTimestamp() },
        { merge: true }
      );
      return true;
    });

    if (!allowed) {
      res.status(429).json({
        error: `Daily AI parsing limit (${FREE_DAILY_LIMIT}) reached. Upgrade to Premium for unlimited access.`,
      });
      return null;
    }
  }

  return uid;
}

/**
 * Authenticated proxy in front of OpenRouter's chat completions endpoint.
 *
 * The Android app builds the same system/user prompts it always has and posts them here with a
 * Firebase ID token (from Google Sign-In) as a bearer token instead of an OpenRouter API key. This
 * function verifies the token, enforces the free-tier daily quota (see [checkAuthAndQuota]), then
 * forwards the request to OpenRouter using the OPENROUTER_API_KEY secret, which never reaches the
 * client, and relays the response back verbatim.
 */
export const aiParse = onRequest(
  {
    secrets: [openRouterApiKey],
    region: "us-central1",
    // Cost/abuse safety net until App Check is added (see functions/README.md).
    maxInstances: 10,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed." });
      return;
    }

    const uid = await checkAuthAndQuota(req, res);
    if (!uid) return;

    const messages = req.body?.messages;
    if (!Array.isArray(messages) || messages.length === 0) {
      res.status(400).json({ error: "Request body must include a non-empty messages array." });
      return;
    }

    try {
      const upstream = await fetch(OPENROUTER_ENDPOINT, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${openRouterApiKey.value()}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: MODEL,
          temperature: req.body?.temperature ?? 0.2,
          response_format: req.body?.response_format ?? { type: "json_object" },
          messages,
        }),
      });

      const text = await upstream.text();
      res.status(upstream.status).set("Content-Type", "application/json").send(text);
    } catch (err) {
      logger.error("OpenRouter request failed", err);
      res.status(502).json({ error: "AI provider request failed. Try again shortly." });
    }
  }
);

interface Enrichment {
  imageUrl: string | null;
  description: string | null;
}

interface TmdbSearchResult {
  poster_path?: string | null;
  overview?: string | null;
}

interface TmdbSearchResponse {
  results?: TmdbSearchResult[];
}

/** Searches TMDb's `/search/movie` or `/search/tv` for [query] and returns its first result, if any. */
async function searchTmdb(mediaType: "movie" | "tv", query: string): Promise<Enrichment | null> {
  const url = `${TMDB_ENDPOINT}/search/${mediaType}?query=${encodeURIComponent(query)}&language=uk-UA&api_key=${tmdbApiKey.value()}`;
  const response = await fetch(url);
  if (!response.ok) return null;
  const data = (await response.json()) as TmdbSearchResponse;
  const first = data.results?.[0];
  if (!first) return null;
  return {
    imageUrl: first.poster_path ? `${TMDB_IMAGE_BASE}${first.poster_path}` : null,
    description: first.overview?.trim() || null,
  };
}

interface WikipediaSummary {
  type?: string;
  extract?: string;
  thumbnail?: { source?: string };
}

/** Fetches a Wikipedia page summary for [query], trying uk.wikipedia.org then falling back to en. */
async function searchWikipedia(query: string): Promise<Enrichment | null> {
  for (const lang of ["uk", "en"]) {
    const url = `https://${lang}.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(query)}`;
    try {
      const response = await fetch(url, { headers: { "User-Agent": WIKIPEDIA_USER_AGENT } });
      if (!response.ok) continue;
      const data = (await response.json()) as WikipediaSummary;
      if (data.type === "disambiguation") continue;
      const imageUrl = data.thumbnail?.source ?? null;
      const description = data.extract ? data.extract.slice(0, 300).trim() : null;
      if (imageUrl || description) return { imageUrl, description };
    } catch (err) {
      logger.warn(`Wikipedia lookup failed for "${query}" (${lang})`, err);
    }
  }
  return null;
}

/**
 * Looks up a real poster/photo and short description for an item's title in a real data source:
 * TMDb for "movie"/"tv", Wikipedia (uk, falling back to en) for anything else. If a movie/TV search
 * comes back empty, we also fall back to Wikipedia rather than returning nothing.
 *
 * Auth and quota are shared with [aiParse] via [checkAuthAndQuota] — this is not a separate,
 * unmetered surface.
 */
export const enrichItem = onRequest(
  {
    secrets: [tmdbApiKey],
    region: "us-central1",
    maxInstances: 10,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed." });
      return;
    }

    const uid = await checkAuthAndQuota(req, res);
    if (!uid) return;

    const entityType = req.body?.entityType;
    const query = typeof req.body?.query === "string" ? req.body.query.trim() : "";
    if (!query || !["movie", "tv", "general"].includes(entityType)) {
      res.status(400).json({ error: "Request body must include entityType (movie|tv|general) and a non-empty query." });
      return;
    }

    try {
      let result: Enrichment | null = null;
      if (entityType === "movie" || entityType === "tv") {
        result = await searchTmdb(entityType, query);
      }
      if (!result || (!result.imageUrl && !result.description)) {
        result = (await searchWikipedia(query)) ?? result;
      }
      res.status(200).json(result ?? { imageUrl: null, description: null });
    } catch (err) {
      logger.error("Entity enrichment lookup failed", err);
      res.status(502).json({ error: "Enrichment lookup failed. Try again shortly." });
    }
  }
);
