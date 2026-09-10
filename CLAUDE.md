# Ritual — working notes

A daily habit tracker built around one idea: **a year is a grid of empty squares, and each day you keep a practice, one fills in.** The home-screen widget is the product; the app exists to feed it.

## Build

```bash
gradle assembleDebug     # or assembleRelease
```

Needs JDK 17 and Android SDK 35. `local.properties` is gitignored — on a machine without it, set `ANDROID_HOME` and Gradle will find the SDK. CI (`.github/workflows/android.yml`) builds both APKs on every push and uploads them as artifacts, so a cloud session can verify a change without a local SDK.

- `minSdk 26` (Android 8.0) — for `java.time` without desugaring.
- No Room, no Hilt, no Glance. Compose, `core-ktx`, and the platform Canvas.

## The one architectural decision

**The widget and the app draw the same pixels from the same function.**

`render/SlabRenderer.kt` returns a `Bitmap`. The widget hands it to `RemoteViews.setImageViewBitmap`; the app hands it to a Compose `Image`. Neither can drift from the other, and it sidesteps RemoteViews' view-count limit — 365 cells as real views is not viable.

If you change how a card looks, change it there once. Do not add a parallel Compose implementation of the grid.

## Design system

Flat, warm, high-contrast. No gradients, no glow, no shadow anywhere — an earlier version glowed and it smeared into unreadable blobs at high completion density.

| Token | Value | Use |
| --- | --- | --- |
| Cream | `#E7E3D4` | Page background |
| Paper | `#F4F2EA` | Inset surfaces |
| Ink | `#12120F` | Text, filled squares, primary pills |
| Lime | `#C9F73F` | Default ritual colour, Mochi's eyes |

Ritual colours: Lime, Red `#E5331C`, Sage `#9DB8A4`, Camel `#BC9F76`, Butter `#EDE55C`, Orange `#F26A1B`.

Type is **Archivo** — one variable TTF in `res/font/`, pinned to weights 500/600/800 by the `archivo_*.xml` font families so Canvas and Compose draw identical instances. Headings are always ExtraBold set tight (`-0.03em`).

**Marking today inverts the card** — colour block to ink, ink grid to the ritual's colour. That flip is the whole visual reward, and it makes an unmarked ritual obvious at a glance across the home screen.

## Mochi

`render/Cat.kt`. A 40×32 bitmap where **only the brow, eye and mouth rows change between moods** — the silhouette never shifts, which is what keeps a pixel mascot from looking redrawn each time.

The bitmap is generated: `tools/mochi.py` computes the silhouette (an ellipse biased low plus a gaussian bulge at cheek level, so the face is widest at the muzzle and the line curves *under* it into the chin) and places the face by hand. `--kotlin` and `--swift` print the grid section for each platform, `--png` a sheet of all four moods. Edit the cat there and paste, rather than moving pixels in the source.

His mood is derived from streak state (`Cat.moodFor`), never chosen for decoration: awake when today is unmarked, pleased once marked, let down the morning after a break.

## Sharing

`share/StoryShare.kt` renders a 1080×1920 card (`render/ShareCardRenderer.kt`) and hands it to Instagram's `ADD_TO_STORY` intent, falling back to the system share sheet when Instagram is absent or too old. Files go through a `FileProvider` rooted at `cacheDir/share`, cleared on every share.

## Storage

`data/HabitStore.kt` — a JSON blob in `SharedPreferences`. Completions are epoch-days, one `Long` each, so a decade of daily practice is a few kilobytes. The store is shared by the UI and the widget receiver; the receiver can start the process cold, so **every public entry point calls `ensureLoaded` first**.

## Gotchas

- `Fonts.load(context)` must run before any renderer draws text. `MainActivity`, `WidgetConfigActivity` and the widget provider all call it.
- The widget's tap targets are transparent panes in `res/layout/widget_ritual.xml` laid over the bitmap. If you move the action pill in `SlabRenderer.drawAction`, move `tap_action` to match.
- `Palette` colours are `val`, not `const val` — Kotlin will not accept `.toInt()` in a compile-time constant.

## Design canvases

`design2/` holds the current direction as Design Component artboards. `design/` is the abandoned dark "Obsidian" direction, kept only for reference — it is not what ships.
