# Turning the cloud on

Ritual works with no account at all — every square lives on the device. This
is what to set up if you want an account to carry a practice to the next
phone, and nothing here is required for the app to run.

## In the Firebase console

1. **Create a project.**

2. **Add an Android app** with the package name `com.ayan.ritual`, and download
   `google-services.json`. Add an **iOS app** with the bundle id
   `com.ayan.ritual` for `GoogleService-Info.plist`.

3. **Register the signing fingerprint on the Android app.** Google Sign-In
   matches on package name plus fingerprint, and refuses the sign-in with a
   bare `ApiException: 10` if it does not recognise the one it sees.

   ```
   SHA-1  AD:A2:07:ED:1B:43:AC:2E:47:08:7E:72:C0:E5:4A:94:70:3E:95:10
   ```

   That is `keystore/ritual-dev.jks`, which every build here is signed with —
   checked in on purpose, because a runner generates a fresh debug keystore
   otherwise and the fingerprint would change on every build. It is a
   development key: it is not secret and must never sign a Play release. When
   the app does ship, Play App Signing issues its own fingerprint, and that one
   gets registered here too.

   Re-download `google-services.json` after adding it. The file carries the
   OAuth client the app reads its web client id from, and without the
   fingerprint that client is not in there.

4. **Authentication → Sign-in method.** Enable **Email/Password**, **Google**
   and **Apple**. Leave the passwordless email link off; nothing uses it.

   Apple also needs an Apple Developer account: a Services ID, a key, and the
   team id, all pasted into the Apple provider in Firebase. On Android it runs
   as a web flow through that Services ID, so it works there whether or not
   anyone has an iPhone.

5. **Firestore Database → Create database**, in production mode. Then paste
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
