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

`render/Cat.kt`. Four drawn portraits, one drawing with a different face, replayed as `Path`s from the generated `render/MochiArt.kt`.

**Why not a `VectorDrawable`.** The drawing is built from shapes that abut rather than overlap, so each antialiases its own edge and the background shows through every boundary as a hairline. Covering it means growing each shape by half a pixel of stroke in its own colour, and it has to be half a *device* pixel: the seam is one device pixel wide however far the art is scaled. A `VectorDrawable`'s stroke is in viewport units and scales with the drawing, so a width that closes the seam at 30dp is a fat outline at 176px. Replaying the paths lets `Cat.draw` set `strokeWidth = 1f / scale` from the scale in hand. The adaptive icon is the one exception: it is a drawable and cannot do that, so `tools/mochi.py` bakes `ICON_SEAM` for the 48–192px an icon is actually drawn at.

`art/mochi/*.svg` is the source, and `tools/mochi.py` converts it. The four SVGs are one 2×2 sheet: identical geometry, differing only in the `viewBox` that windows onto a quadrant. The converter windows each quadrant out, drops the paths that fall outside it, and registers the four against each other, so **the head sits on the same coordinates in every mood** and the silhouette never shifts. One parse feeds both platforms: `MochiArt.kt` for Android, `ios/Shared/MochiArt.swift` for iOS, plus the adaptive-icon foreground, so the two can never draw different shapes. `--check` re-emits and compares, which is what CI runs. Edit the SVGs and regenerate; never hand-edit the generated files.

They are vectors rather than PNGs because he is drawn at sizes an order of magnitude apart — 30dp in the widget header, 176px on the story card — and a raster picked for one is wrong for the other.

`Cat` needs no loading: the art is embedded, and each mood is parsed on first draw and cached. Size him by height, `Cat.draw(canvas, left, top, height, mood)`, and `Cat.widthFor` gives the rest.

His mood is derived from streak state (`Cat.moodFor`), never chosen for decoration: awake when today is unmarked, pleased once marked, let down the morning after a break.

### Motion

`render/MochiMotion.kt` is the beat table and a clock, with no Compose and no `android.graphics` in it, so iOS can run the same timings against its own drawing. `MochiTile` advances it once a frame and moves a rasterised mood rather than redrawing seventy-five paths.

There is no rig and there will not be one without new art: nine of the paths carry head and chest as a single outline, so a head turn or an independent ear flick is not available. What is available is the whole portrait as one body, scaled from the bottom centre so a squash presses him onto the tile instead of shrinking him toward the middle of it.

A beat answers a change of state, exactly as the face does. `MochiTile` picks it from the mood it is handed: `MARK` when it turns pleased, `MISS` when it turns let down, `SETTLE` otherwise, and never on the first composition, so opening a screen does not start him hopping. `UNLOCK` is the one beat no mood implies, so the paywall passes its own `MochiMotion` and plays it. Breathing and blinking run underneath all of it, and stop when the device has animations turned off.

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
