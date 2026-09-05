# aiParse / enrichItem Cloud Functions

`aiParse` is an authenticated proxy in front of OpenRouter (`openai/gpt-oss-120b`). `enrichItem` looks
up a real poster/photo and short description for an item's title — TMDb for movies/TV, Wikipedia for
everything else. Both verify the caller's Firebase ID token and share the same free-tier daily quota
via Firestore, then do their real work using a server-side secret. See `src/index.ts` for the logic.

## One-time project setup

1. Install the CLI (if you don't have it): `npm install -g firebase-tools`
2. `firebase login`
3. Create a Firebase project at https://console.firebase.google.com (or reuse an existing GCP project).
4. In the console: **Build → Firestore Database → Create database** (production mode, any region).
5. In the console: **Build → Authentication → Sign-in method → Google → Enable**. This also creates
   the OAuth "Web client" that the Android app's `default_web_client_id` resource needs.
6. **Project settings → Your apps → Add app → Android**, package name `com.rainyday.saveableapp`,
   plus your debug/release SHA-1 fingerprints (`./gradlew signingReport`). Download the generated
   `google-services.json` into the `app/` directory (already gitignored).
7. Upgrade the project to the **Blaze (pay-as-you-go)** plan — required for Cloud Functions v2 to
   make outbound network calls (to OpenRouter), even while usage stays inside the free tier.
8. From the repo root: `firebase use --add` and select your project — this writes `.firebaserc`.

## Secrets

Neither key is ever committed or built into the app. Set each once per project:

```
firebase functions:secrets:set OPENROUTER_API_KEY
firebase functions:secrets:set TMDB_API_KEY
```

(paste the key when prompted). Update the same way to rotate. `TMDB_API_KEY` is a free "API Key
(v3 auth)" from https://www.themoviedb.org/settings/api (requires a free TMDb account).

## Deploy

```
cd functions
npm install
npm run deploy
```

Also deploy the Firestore rules from the repo root: `firebase deploy --only firestore:rules`

After deploying, copy each function's URL (printed by the deploy command, format
`https://us-central1-<project-id>.cloudfunctions.net/<aiParse|enrichItem>`) into the matching
`AI_PARSE_ENDPOINT` / `AI_ENRICH_ENDPOINT` `buildConfigField` for that flavor in `app/build.gradle.kts`.

## Granting someone Premium (unlimited calls)

No payment integration exists yet — this is a manual flag. In the Firestore console, create/edit a
document at `users/{uid}` (their Firebase Auth uid, visible in Authentication → Users) with a
boolean field `isPremium: true`.

## Local testing

```
npm run build
firebase emulators:start --only functions,firestore
```

## Suggested follow-ups (not implemented here)

- **App Check** (Play Integrity provider): stops anyone from calling `aiParse` outside the real app,
  even without a login. Add `firebase-appcheck` on Android and enforce it on the function once
  enrolled — worth doing before wider release.
- **Budget alert**: set a Google Cloud budget alert on the project so a bug or abuse spike (before
  App Check is in place) surfaces as an email, not a bill surprise.
- **Real subscriptions**: replace the manual `isPremium` flag with Google Play Billing + server-side
  purchase verification when you're ready to actually sell Premium.
