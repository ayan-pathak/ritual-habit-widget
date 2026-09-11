# Getting the paywall working

The unlock is a real Google Play purchase, and Play Billing will not talk to an
app that did not come from Play. That is the whole of why a sideloaded APK
shows the paywall but can never complete it: `queryProductDetails` comes back
empty and the button has no price to offer. Nothing in the app is broken when
that happens — it is Play declining to serve an installer it does not know.

So the shortest path to seeing it work is to get a build onto an internal
testing track and install it from there.

## 1. A Play Console account

<https://play.google.com/console> — $25, once, and it takes a day or two to be
approved. Nothing below can start until that clears.

## 2. Create the app

**All apps → Create app.**

- App name: Ritual
- Default language, app or game, free or paid: free
- Accept the declarations

Then **Setup → App integrity** and make sure **Play App Signing** is on, which
it is by default for new apps. Google holds the key that signs what users
install; the key you sign uploads with is a separate thing and is only ever
seen by Play.

## 3. Make a bundle

Play takes an `.aab`, not an `.apk`, and rejects a second upload at a
versionCode it has already seen. The **Play bundle** workflow handles both:

**Actions → Play bundle → Run workflow**, with a version name like `1.0.1`.

It publishes the `.aab` as a release. The run number becomes the versionCode,
so it climbs by itself.

### The upload key

Without a key of your own the bundle is signed with `keystore/ritual-dev.jks`,
which is in this repository, which is public. That is fine for a first look and
wrong for anything that stays. To use your own, make one:

```
keytool -genkeypair -v -keystore upload.jks -storetype PKCS12 \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

It asks for a password twice and a few details that do not matter much. Then:

```
base64 -w0 upload.jks    # on macOS: base64 -i upload.jks
```

Add four repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `UPLOAD_KEYSTORE_BASE64` | the base64 above, one line |
| `UPLOAD_KEYSTORE_PASSWORD` | the password you chose |
| `UPLOAD_KEY_ALIAS` | `upload` |
| `UPLOAD_KEY_PASSWORD` | the same password, unless you set a different one |

Keep `upload.jks` somewhere safe. Losing it means asking Google to reset the
upload key, which is survivable but tedious.

## 4. Upload it

**Testing → Internal testing → Create new release.** Drop the `.aab` in, write
a release note, **Save**, then **Review release** and **Start rollout**.

Play will ask you to finish some declarations before it will publish, even to
internal testing: app content, data safety, content rating, target audience,
and a privacy policy URL. They are forms, not work.

## 5. Create the product

**Monetise → Products → In-app products → Create product.**

- **Product ID:** `ritual_unlimited` — exactly this. `billing/Unlock.kt` asks
  for it by name, and it cannot be changed after it is created.
- Name and description: whatever reads well
- **Price:** $4.99, the nearest standard tier to $5
- **Activate** it. A product left as a draft returns nothing.

The product will not appear to the app until a build carrying it is live on a
track. That is why this comes after the upload rather than before.

## 6. Add yourself as a tester

**Internal testing → Testers** — make a list with your Google account on it.

Then **Monetise → Licence testing**, and add the same account. Licence testers
see a real purchase flow that does not charge, and their purchases can be
refunded and repeated as often as you like.

Both lists take a few minutes to propagate.

## 7. Install from Play

The internal testing page has an **opt-in link**. Open it on the phone with the
tester account signed in, accept, and install from the Play listing it gives
you.

This is the step that actually matters. Installing the `.aab`'s APK by hand
puts you back where you started: Play Billing checks how the app arrived, and a
sideload is not an answer it accepts.

## What you should see

One ritual, free. Creating a second one opens the paywall with a real price on
the button. Buying it is a test purchase, and `Unlock.canCreate` starts
returning true.

## When it does not work

- **No price, button says nothing.** The build is not live on a track yet, or
  the product is still a draft, or the app was not installed from Play.
- **"This version of the application is not configured for billing."** The
  versionCode installed is not one Play knows. Upload the bundle you are
  actually running.
- **"The item you requested is not available for purchase."** The product ID
  does not match, or it was created less than a few minutes ago.
- **Charged for real.** The account is not on the licence testing list.

## A note on the price

`Unlock` reads the price out of the storefront and never hardcodes it, so
changing it in Play Console changes it in the app with no rebuild. The $4.99
above is only a starting point.
