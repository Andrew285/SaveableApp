import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

initializeApp();

const openRouterApiKey = defineSecret("OPENROUTER_API_KEY");

/** Model is pinned server-side — the client can't request a different (possibly pricier) model. */
const MODEL = "openai/gpt-oss-20b";
const OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

/** Calls per day for a non-premium user before they're rejected with 429. */
const FREE_DAILY_LIMIT = 20;

/**
 * Authenticated proxy in front of OpenRouter's chat completions endpoint.
 *
 * The Android app builds the same system/user prompts it always has and posts them here with a
 * Firebase ID token (from Google Sign-In) as a bearer token instead of an OpenRouter API key. This
 * function:
 *   1. Verifies the ID token to get a stable uid.
 *   2. Looks up users/{uid}.isPremium in Firestore (set by hand for now — no billing integration yet).
 *   3. Free users: atomically checks & increments a per-day Firestore counter, rejecting once they
 *      hit FREE_DAILY_LIMIT. Premium users skip this.
 *   4. Forwards the request to OpenRouter using the OPENROUTER_API_KEY secret, which never reaches
 *      the client, and relays the response back verbatim.
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

    const authHeader = req.get("Authorization") ?? "";
    const bearerMatch = authHeader.match(/^Bearer (.+)$/);
    if (!bearerMatch) {
      res.status(401).json({ error: "Missing Authorization bearer token." });
      return;
    }

    let uid: string;
    try {
      const decoded = await getAuth().verifyIdToken(bearerMatch[1]);
      uid = decoded.uid;
    } catch (err) {
      logger.warn("Rejected request with invalid ID token", err);
      res.status(401).json({ error: "Your sign-in has expired. Please sign in again." });
      return;
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
        return;
      }
    }

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
