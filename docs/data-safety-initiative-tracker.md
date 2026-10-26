# Data safety answers – Initiative Tracker 2.0

Draft answers for Play Console > App content > Data safety (the menu may have moved).
Based on the code of versionCode 35 and Google's own disclosure pages for the
Google Mobile Ads SDK and Firebase (checked 26-10-2026).

## Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (AdMob and Firebase both use TLS) |
| Which of the following methods of account creation does your app support? | **My app does not allow users to create an account** (Table sync signs in anonymously: no name, email or password) |
| Do you provide a way for users to request that their data is deleted? | **Yes** – in the app (Stop session / Leave) and by email to rekijan.apps@gmail.com |

## Data types

Only tick the types below; leave everything else unticked.

| Data type | Collected | Shared | Ephemeral | Required / optional | Purposes |
|---|---|---|---|---|---|
| **Location > Approximate location** (IP address, AdMob) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security, and compliance |
| **App activity > App interactions** (AdMob: launches, taps, ad views) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security, and compliance |
| **App activity > Other user-generated content** (Table sync character sheets) | Yes | No | No | Optional | App functionality |
| **App info and performance > Diagnostics** (AdMob) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security, and compliance |
| **Device or other IDs** (advertising ID and app set ID for AdMob; anonymous Firebase ID; Play Integrity token for App Check) | Yes | Yes | No | Required | App functionality, Advertising or marketing, Analytics, Fraud prevention, security, and compliance |

Why these answers:

- **AdMob data counts as shared** because Google uses it for its own ad purposes. Mark it
  Required, since the free version always loads ads. Remove Ads stops them, but that is a
  purchase, not an opt-out.
- **Table sync data counts as collected but not shared.** The app sends it to its own Firebase
  project, and Firebase is a service provider. The GM seeing it is a transfer the player starts
  themselves, which Play exempts from "shared".
- **Optional:** sync only runs when someone starts or joins a session.
- **Character sheet fields sent:** name, initiative and bonus, hp, max hp, fast healing,
  regeneration, AC, saves, maneuvers, skills, attack routine, notes, and conditions (spell
  conditions carry the caster's name only).
- **Purchases are not declared.** Google Play handles payment, and the app only checks on the
  device which products you own. Nothing about purchases is sent to us.

## For you to decide or check

1. **No TTL policies (decided 26-10-2026):** TTL needs a billing-enabled (Blaze) project, and the
   project stays on Spark. So there is no fixed deletion deadline. Sync data is deleted when the GM
   stops the session, when a player leaves, when the same GM starts a new session after it
   expired, or on request by email. The privacy policy and the in-app text say so.
2. **Anonymous Firebase IDs:** the Firestore data expires, but the anonymous user in Firebase
   Authentication (just an ID, no personal data) stays until it is deleted. Either accept that
   (the policy says so and offers deletion by email), or turn on automatic cleanup of anonymous
   accounts if your Firebase plan offers it. Check the current Firebase Auth docs.
3. **Deletion by email:** to delete on request you need the person's anonymous ID or session code.
   The policy asks for the session code. Fine as is, or say no if you'd rather not offer this.
