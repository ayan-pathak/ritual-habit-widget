# Ritual

A daily habit tracker built around one idea: **a year is a grid of empty squares, and each day you keep a practice, one fills in.**

The home-screen widget is the product. The app exists to feed it.

---

## What it does

- **Create a ritual for anything** — name it, say when in the day it belongs, pick its colour.
- **Mark a day done** — from inside the app, or straight from the widget without opening anything.
- **A commit-grid of the whole year**: 53 week-columns × 7 weekday-rows on a flat colour block.
- **Squares left in the year**, on the widget, the home header and the detail screen.
- **Walk back through past years** — a habit outlives a calendar.
- **Share a streak to Instagram Stories** — a 1080×1920 card with the number, the grid and the cat.
- **Mochi**, a pixel cat whose mood is your streak.

## The look

Flat, warm, high-contrast. Cream ground, solid ink marks, chunky black pills, Archivo set tight. No gradients, no glow, no shadows.

**Marking today inverts the card** — colour block to ink, ink grid to the ritual's colour. That flip is the reward, and it makes an unmarked ritual obvious at a glance.

An earlier version of this app was dark and everything glowed. It looked good at 5 completed days and turned into an unreadable smear at 150, which is the density that actually matters. Flat marks cannot do that.

## Mochi

A 20×18 pixel cat. Only the eye and mouth rows change between moods, so the silhouette never shifts — that's what keeps a pixel mascot from looking redrawn each time.

| Mood | When |
| --- | --- |
| Awake | Today is still unmarked |
| Pleased | You marked today |
| Resting | A rest day you planned |
| Let down | The streak broke yesterday |

## Architecture

The interesting decision: **the widget and the app draw the same pixels from the same function.**

```
render/SlabRenderer.kt      the card: grid, header, tally, action pill
render/ShareCardRenderer.kt the 1080x1920 Instagram story card
render/Cat.kt               Mochi, as a bitmap
render/Fonts.kt, Palette.kt, GridGeo.kt
```

`SlabRenderer.render()` returns a `Bitmap`. The widget hands it to `RemoteViews.setImageViewBitmap`; the app hands it to a Compose `Image`. Neither can drift from the other, and the widget is free of RemoteViews' view-count limits — 365 cells as real views is not viable.

The widget's two hit targets are transparent panes laid over that bitmap: the field opens the app, the pill marks today in place.

Storage is a JSON blob in `SharedPreferences` — completions are epoch-days, one `Long` each, so a decade of daily practice is a few kilobytes.

## Building

Requires JDK 17 and Android SDK 35.

```bash
gradle assembleRelease
```

The APK lands in `app/build/outputs/apk/`. CI builds both variants on every push and uploads them as artifacts.

- `minSdk 26` (Android 8.0) — for `java.time` without desugaring.
- No Room, no Hilt, no Glance. Compose, `core-ktx`, and the platform Canvas.

Release is signed with the debug key — fine for sideloading, not for distribution.
