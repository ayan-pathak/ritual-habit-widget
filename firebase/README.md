# Turning the cloud on

Ritual works with no account at all — every square lives on the device. This
is what to set up if you want an account to carry a practice to the next
phone, and nothing here is required for the app to run.

## In the Firebase console

1. **Create a project.**

2. **Add an Android app** with the package name `com.ayan.ritual`, and download
   `google-services.json`. Add an **iOS app** with the bundle id
   `com.ayan.ritual` for `GoogleService-Info.plist`.

   No SHA-1 fingerprint is needed. That is only for Google Sign-In, phone auth
   and Dynamic Links; Ritual signs in with email and password, which is the
   one method that behaves identically on both platforms.

3. **Authentication → Sign-in method → enable Email/Password.** Leave the
   passwordless link off; nothing uses it.

4. **Firestore Database → Create database**, in production mode. Then paste
   `firestore.rules` from this directory into **Rules** and publish.

   Do not leave it in test mode. Those rules let anyone read and write every
   account's rituals, and they expire after thirty days, at which point sync
   starts failing with permission errors that look like a bug in the app.

## So the config reaches a build

Both config files are gitignored, deliberately — they are per-project, and one
of them ends up in the hands of anyone who installs the APK. The builds here
pick them up from repository secrets instead:

| Secret | Contents |
| --- | --- |
| `GOOGLE_SERVICES_JSON` | the whole of `google-services.json`, pasted as-is |
| `GOOGLE_SERVICE_INFO_PLIST` | the whole of `GoogleService-Info.plist`, pasted as-is |

Set them under **Settings → Secrets and variables → Actions**. With a secret
present the workflow writes the file before building and sign-in is live in
that build; with no secret it is a no-op and the app behaves exactly as it
does now.

To build locally instead, drop `google-services.json` at `app/` and
`GoogleService-Info.plist` at `ios/Resources/`. Gradle applies the
google-services plugin only when it finds that file, and the iOS app calls
`FirebaseApp.configure()` only when the plist is in the bundle — so a checkout
without either still builds and still runs.

## What the app does once it is on

`Account` signs in and out. `CloudSync` mirrors one document per ritual to
`users/{uid}/habits/{id}` and merges what comes back by union: a day marked on
either device stays marked, because losing a kept day is worse than keeping a
day twice.

The sign-in screen appears on first launch only when there is a configuration
to sign in to. Without one it is skipped, since a sign-in nobody can complete
is a locked door.
