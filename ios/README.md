# Ritual for iOS

A native port: SwiftUI for the app, WidgetKit for the home screen, and the same
renderer behind both.

## The shared half

`Shared/` is the port of the Kotlin that had no Android in it — the day maths,
the palette, the grid geometry, Mochi, and `SlabRenderer`. Both targets compile
it, so the app and the widget draw the same pixels from the same function, the
way the Android build does. Change how a card looks in `SlabRenderer.swift` and
both follow.

Completions are still epoch-days in a JSON blob, in the same shape Android
writes, so the two platforms could read each other's export unchanged.

## Building

```bash
brew install xcodegen
cd ios && ./bootstrap.sh && xcodegen generate
open Ritual.xcodeproj
```

`bootstrap.sh` copies Archivo out of the Android resources, so the one variable
TTF in the repository is the one both platforms draw with.

The `.xcodeproj` is generated from `project.yml` and deliberately not checked
in. CI (`.github/workflows/ios.yml`) does the same two steps on a macOS runner
and builds for the simulator on every push.

Needs Xcode 15.4+ and iOS 17 (App Intents drives the widget's mark button and
its ritual picker).

## Before it can run on a phone

Everything here builds unsigned for the simulator. A device build additionally
needs, in Xcode:

- a development team on both targets,
- the `group.com.ayan.ritual` App Group enabled on both (it is what the widget
  reads the rituals through),
- the in-app purchase `com.ayan.ritual.unlimited` created in App Store Connect
  as a non-consumable. `Ritual.storekit` stands in for it locally: pick it under
  Product → Scheme → Edit Scheme → Run → Options → StoreKit Configuration.

## The paywall

One ritual is free forever. A second one asks for a single payment and never
asks again. Marking a day, the widget, and the archive are never gated — a
lapsed or refunded purchase must not stop someone filling today's square, and
must never hide a year they already kept. `Unlock.swift` holds the whole of it.
