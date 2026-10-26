# Privacy policy draft: Initiative Tracker 2.0

A standalone page in the same layout as https://rekijan.nl/privacypolicy-secretroller.php,
for example at https://rekijan.nl/privacypolicy-initiativetracker.php. Small edits to the
general policy are at the bottom.

Check before publishing: the **Firestore location** in section 3. Secret Roller says "in the
EU". Look up Initiative Tracker's location in Firebase (Firestore > Databases, the Location
column) and fill it in.

---

# Initiative Tracker privacy policy

Last updated: [date of publishing]

## 1. Who is responsible

Initiative Tracker is made by Rekijan, trading as Rekijan Apps. Questions about this policy or
your data: rekijan.apps@gmail.com.

## 2. Data that stays on your device

Your parties, characters, spells, round counters and settings are stored only on your device.
They don't leave it unless you use Table sync (see 3).

## 3. Data stored online (Table sync)

Table sync is optional. It lets a game master (GM) and players at the same table share
characters, and it only starts when you start or join a session.

- **Anonymous sign-in.** The app signs in through Firebase Authentication with a random ID: no
  name, e-mail address or phone number. Firebase also processes your IP address to prevent
  abuse.
- **App Check.** Firebase App Check with Google Play Integrity checks that requests come from
  the real app from Google Play.
- **A GM's session** stores the session code, when it was created, the GM's anonymous ID and
  when it expires.
- **A player's character.** When a player joins, the app sends their character's name,
  initiative, hit points, fast healing and regeneration, AC, saves, maneuvers, skills, attack
  routine, notes and conditions, plus when it was sent and when it expires. A spell condition
  only carries the name of the character who cast it.
- **The GM's copy.** The GM's app stores its own version of each player's character in the
  session, so the player's app can follow the GM's changes, such as hit points and conditions.
- **Who can read it.** The session's GM sees the characters sent to it. A player only sees
  their own character. I don't look at session contents.
- **Where it is stored.** In Google Cloud Firestore [in the EU / location].
- **The session code works like a password.** Only share it with your players.

## 4. Advertising

The free version shows a banner ad from Google AdMob. Google's ads software collects your IP
address (to estimate your general location), how you interact with ads, diagnostics, and device
identifiers such as the advertising ID.

- In the EEA, the UK and Switzerland, you are asked for consent first.
- In US states with their own privacy laws, you can opt out of the sale or sharing of your data
  for personalised ads.
- You can change either choice at any time: open the app menu and tap **Privacy Settings**.

Google's own privacy policy covers this data. Buying Remove Ads stops the app from loading ads.

## 5. Why

- To run Table sync sessions you ask for (providing the service).
- To show ads that pay for the app (your consent, where the law requires it).
- To protect the service against abuse (legitimate interest in security).

## 6. How long

- **Stopped sessions.** When the GM stops a session, the session and all characters in it are
  deleted right away.
- **Leaving players.** When a player leaves, the character they sent is deleted.
- **Expired sessions.** A session expires 30 days after the GM last used it; each time the GM
  opens it, this starts over. An expired session can't be joined or changed. It is deleted when
  that GM next starts a session.
- **Forgotten sessions.** A session that its GM never stops, and whose GM doesn't start a new
  one, stays stored until you ask me to delete it (see 9).
- **The anonymous ID** stays until it is deleted on request.
- **Play Integrity data** is kept for the fixed period set by Google.
- **On-device data** stays until you delete it or uninstall the app. Uninstalling does not delete
  data that was already sent to a session.

## 7. Sharing

I don't sell your data. Google is the only other party: as the host of sessions (Firebase) and
as the ad provider (AdMob). The app has no separate analytics or crash reporting.

## 8. Purchases and tips

Google Play handles all payments: Remove Ads, tips and party packs. I never receive your
payment details. The app checks with Google Play which products you own, so purchases survive a
reinstall.

## 9. Your rights

You can ask to see, correct or delete your data, or object to its use, by e-mailing
rekijan.apps@gmail.com. There are no accounts, so please include the session code and the
character's name. The quickest way to delete sync data is in the app: a GM taps Stop session, a
player taps Leave. You can withdraw ad consent in the app (see 4). You can also complain to the
Dutch data protection authority, the Autoriteit Persoonsgegevens.

## 10. Changes

If this policy changes, the date at the top changes too.

---

## Edits to the general policy (https://rekijan.nl/privacypolicy.php)

- Where it says Secret Roller has its own policy, add that **Initiative Tracker** has its own
  policy too, and link to the new page.
- Don't add Initiative Tracker to the general page's list of apps with ads: its own page covers
  that.
