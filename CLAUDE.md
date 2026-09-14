# Claude Instructions

- Do not ask for confirmation before taking actions. Proceed autonomously.
- Never amend commits; always create a new commit. Commit/push only when asked.

## What this is

**deckard** — an Android **AI-slop detector**. A floating "Deckard" mascot lives in a system
overlay over every app; summoning it reads the text on the current screen and (eventually) judges
whether that content is AI-generated "slop". Screen reading is on-device: a screenshot is
transcribed by Gemma via LiteRT-LM (OCR).

The UI is Jetpack Compose, but it's rendered into `WindowManager` overlay windows from a plain
`Service` (and driven by an `AccessibilityService`), **not** a normal Activity — the only Activity
is
a small setup screen (`MainActivity`) for granting permissions and starting/stopping the overlay,
with a settings screen behind it.

> History: this started as a custom soft-keyboard (IME) and was pivoted to the slop detector. The
> keyboard, its predictive-suggestion engine (dictionary + n-gram learning), and the Room DB have
> been removed. Only the on-device LLM layer (`LlmEngine`) survives from that era, reused for OCR.
> If you find lingering keyboard references, they're stragglers worth cleaning up.

- applicationId / namespace: `com.costafotiadis.deckard` (debug variant: `.debug`)
- Build variants: `debug` / `release` only (no product flavors)
- DI: Hilt. App class: `DeckardApplication` (`@HiltAndroidApp`)

## Module layout

- `app` — the slop detector (overlay + accessibility services) + the setup/settings `MainActivity`
- `design` — theme/UI (`AppTheme`)
- `textresource` — `TextResource`, a string that resolves at the draw site (see *Copy* below)
- `common`, `common-test`, `logging`, `auth`, `testing` — shared libs
  (one namespace root across the whole repo: the app is `com.costafotiadis.deckard`, each lib is
  `com.costafotiadis.<module>`)
- `build-logic/convention` — Gradle convention plugins (`application.common`,
  `application.compose.common`, `hilt.common`, `library.common`, `library.compose.common`)

## Build / run

The daemon's JDK is pinned by `gradle/gradle-daemon-jvm.properties` (Java 21, any vendor), so the
build does not depend on `JAVA_HOME`: the wrapper only needs *some* Java to launch, then finds a 21
among the installed JDKs or downloads one into `~/.gradle/jdks` through the foojay resolver in
`settings.gradle.kts`. `./gradlew --version` shows both the launcher JVM and the daemon criteria.
To move the pin, run `./gradlew updateDaemonJvm --jvm-version=N` and commit the regenerated file
rather than editing it by hand. The SDK comes from `sdk.dir` in `local.properties`.

- Compile: `./gradlew :app:compileDebugKotlin`
- Build APK: `./gradlew :app:assembleDebug`
- Install: `./gradlew :app:installDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`

**The checks CI runs, in the order it runs them** (`.github/workflows/ci.yml`, borrowed from
nowinandroid and wired through the convention plugins so every module gets them):
`./gradlew :build-logic:convention:check` (strict `validatePlugins` plus androidx's Gradle-plugin
lint on build-logic), `spotlessCheck` (ktlint, Android style, rules in `.editorconfig`, no licence
headers; `spotlessApply` fixes), `dependencyGuard` (the app's release runtime classpath against
`app/dependencies/releaseRuntimeClasspath.txt`; `dependencyGuardBaseline` refreshes it, and read
the diff before you do), `testDebugUnitTest`, `:app:assembleDebug :app:assembleRelease`,
`:app:lintRelease lint` (every library too, with `checkDependencies`), and
`:app:checkReleaseBadging` (aapt2's badging of the release APK against `app/release-badging.txt`,
so a permission or component arriving through a library shows in a diff; `:app:updateReleaseBadging`
refreshes it). Library modules enforce a `<module>_` resource prefix, and lint's `ResourceName` is
what fails when one is missing.

**`scripts/deckard` drives the debug build on a connected device** and is the fast way to see a
change working: `install [ai|assisted|human|mixed]` (build, install, re-grant, wait for the
accessibility service to bind), `grant`, `start` / `stop` / `dismiss`, `summon` (a hold on the
tab), `effect [name]` / `shutter [package]` (pick and fire the screenshot effect — see *The shutter*
below), `shot`, `log`. It is *On a device* below, automated — including both traps that make the
accessibility setting silently revert, and the rebind after every reinstall. Every wait polls for
the state it wants rather than sleeping a guessed number of seconds.

To use it after install, open the app and work through `MainActivity`'s setup screen: enable the
accessibility service (it takes the screenshot), grant draw-over-apps, then start Deckard. A
**long-press** on the left-edge tab summons the mascot: a screenshot, and the on-device model
isolates the main post out of it.

The on-device LLM is required for the read, but optional to
*launch*: with no model present, summoning Deckard reports it has no eyes yet. To enable it,
`adb push` a `.litertlm` into `/sdcard/Android/data/<applicationId>/files/models/` (≈2.4–3.5 GB; the
`LlmEngine` loads the first `.litertlm` it finds there). For the **debug** build `<applicationId>`
is
`com.costafotiadis.deckard.debug`, so the dir is
`/sdcard/Android/data/com.costafotiadis.deckard.debug/files/models/` — create it with
`adb shell mkdir -p` and push to a full filename (a trailing-slash dest fails with "Is a directory"
if `models/` doesn't exist yet). `LlmEngine` warms up once per process and caches the loaded engine,
so after pushing a new model `am force-stop` (or reinstall) to reload it. Local `.litertlm` files
under `model/` are gitignored.

### On a device

`scripts/deckard install` does this whole step. By hand it is `./gradlew :app:installDebug` plus
granting overlay + accessibility (once per install) via adb — note this **overwrites** the
enabled-a11y-services list, so re-enable any others (e.g. TalkBack) afterwards:

```
PKG=com.costafotiadis.deckard.debug
SVC=$PKG/com.costafotiadis.deckard.accessibility.DeckardAccessibilityService
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell appops set $PKG ACCESS_RESTRICTED_SETTINGS allow
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1   # launch FIRST, see below
adb shell "settings put secure enabled_accessibility_services $SVC"
adb shell "settings put secure accessibility_enabled 1"
```

Two traps, both of which make the setting **silently revert to `null`** on Android 14+ (verified
on API 37):

- Without `ACCESS_RESTRICTED_SETTINGS allow`, Enhanced Confirmation Mode reverts the write within
  a second or two. `adb shell dumpsys accessibility | grep "Bound services"` is the real check —
  `settings get` can still read back the value it is about to lose.
- **`am force-stop` on the package disables the service**, so enable it *after* the app is
  running, never before. To make `MainActivity` re-read the state, background and foreground it
  (`input keyevent KEYCODE_HOME`, then launch again) rather than force-stopping.

Changing `accessibility_service_config.xml` only takes effect after the service re-binds — toggle
it off/on by re-running the `settings put` lines. Then open the app → **Start Deckard**.

## Architecture notes (the non-obvious bits)

### Overlay service — `mascot/DeckardOverlayService.kt`

- A plain started `Service` (no Activity host) that hosts two `WindowManager` overlay windows: the
  draggable mascot (`mascot/DeckardComposeView.kt` — `mascot/DeckardMascot.kt`'s plated 🧙 with a
  tailed bubble or the verdict report card beneath him, and an always-present X close control riding
  the plate's corner), and the always-present left-edge summon tab
  (`mascot/DeckardEdgeHandleView.kt`).
- The overlay window is WRAP_CONTENT, so any shadow drawn outside the content is clipped square at
  the window edge. `DeckardComposeView` pads the root by `OVERLAY_SHADOW_ROOM` to give the mascot's
  and the card's shadows room inside the window; the service's x offset compensates for that padding.
- An overlay service has no lifecycle/decor-view callbacks, so the service implements
  `LifecycleOwner` + `ViewModelStoreOwner` + `SavedStateRegistryOwner` itself, drives its own
  `LifecycleRegistry` to RESUMED, and sets the view-tree owners directly on each overlay view — all
  required for Compose to compose and recompose.
- The edge tab (`mascot/DeckardEdgeHandleView.kt`) carries **one gesture**: a **long-press** (with
  a haptic tick) summons via the screenshot OCR read (`@OcrContentScreenText`), through
  `summon()` → `readScreenAndJudge()`. It no longer declares `setSystemGestureExclusionRects`: a
  hold never moves, so the Android 10+ back gesture has nothing to take, and a swipe on the tab is
  the app's back gesture like anywhere else on the edge. The mascot himself takes no tap — he
  would be in the picture a summon takes.
- Requires the draw-over-apps permission (checked in `onCreate`) and the accessibility service
  (for the screenshot). Started/stopped from `MainActivity`'s setup screen.

### The Activity's two screens — `ui/`

`MainActivity` is the only Activity and it hosts two destinations, not one: the **setup** checklist
(`ui/setup/SetupScreen.kt`) and the **settings** screen behind it (`ui/settings/SettingsScreen.kt`),
with `ui/DeckardApp.kt` holding the back stack and the display that renders the top of it. They are
split because they are different kinds of screen — setup is a list you work down once and never
return to, settings is a preference you come back to and change your mind about — and because the
settings screen is meant to grow a section at a time.

- **Navigation 3**, not `navigation-compose`. `Destination` is a sealed interface of
  `@Serializable data object`s implementing `NavKey`, and `rememberNavBackStack` saves the stack
  through kotlinx serialization rather than route strings, so a destination is an object you
  construct and a wrong one is a compile error. Both nav3 artifacts were already declared in
  `app/build.gradle.kts` by the template; `androidx.navigation.compose` is still declared there and
  is now dead (COS-246).
- The `entry<T> { }` builder inside `entryProvider { }` is a **member** of `EntryProviderScope`, not
  a top-level function — importing `androidx.navigation3.runtime.entry` does not resolve.
- The shutter choice is hoisted to `MainActivity`, which injects `ShutterEffectStore` and passes it
  down, because that store is a process-wide singleton the overlay service reads too: the settings
  screen is one of two things looking at the same value, not the place it lives.
- The page frame both screens are written on is `ui/component/PageScaffolding.kt` (`DeckardPage`,
  `SectionLabel`). It is a column rather than a Surface so a caller can draw over it — the settings
  screen plays an effect full-size on top of its own page.
- **`scripts/deckard` knows about the second screen.** A relaunch resumes the back stack where it
  was left, so `open_setup` presses back out of settings before anything taps the setup screen, and
  `open_settings` taps through to it before the `effect` verb can reach a row. The two are told
  apart by a line only the settings screen says (`WHEN HE TAKES THE PICTURE`) — the door's own
  title is on both.

### The shutter — `shutter/`

A read that photographs your screen says so, and every summon photographs it, so every summon
says so. (Shared text — `ShareTextActivity` — reads no screen and stays silent.)

- **The constraint that shapes it**: the effect must not be in the screenshot, which is of the whole
  display, overlay windows included. So the window goes up inside `ScreenTextReader.read`'s
  `onScreenCaptured` callback and never a moment earlier — the same seam `show(Thinking)` hangs off
  (see `a7613cc`). Ordering: long-press, haptic, shutter, effect + Deckard, inference, verdict.
- **The seam** — `ShutterPainter` is a painter over a rectangle: the size, where it is in the run
  (`ShutterFrame`), and one ink. Knowing nothing else is what lets the same code draw full-screen
  over another app and miniature in the settings screen's picker. `ShutterFrame.scale` is 1 at full
  size and a fraction in the miniature, so every dp shrinks with it — **the picker is not a mock-up
  of the effect, it is the effect**.
- **Four at once, on purpose** (`ShutterEffect`): `CropMarks`, `Bloom`, `Highlight`, `Stamp`, and
  `None`, because a comparison needs a baseline. All of them are ink (`onSurface`) at some alpha:
  there are no accent roles here and each `StampInk` means one specific verdict, so borrowing one
  would be a naming lie. Nothing blurs — the vision inference is on the same GPU, and an effect that
  stutters exactly when it is meant to reassure is a failed effect. `key` is what is persisted, so
  it outlives a renamed constant (`ShutterEffectTest`).
- **`ShutterSurface`** drives a run: Snap → Working (loops while `running`) → Release →
  `onFinished`. One `withFrameMillis` loop writes a frame that is read **only inside `drawBehind`**,
  so sixty frames a second invalidate the draw and recompose nothing. Two traps, both load-bearing:
    - `running` is read through a `State` inside `snapshotFlow`. A captured `Boolean` parameter is
      fixed at the composition that started the effect, so the run never hears that it is over and
      the window sits there until the safety release.
    - Working self-releases after 30s. A full-screen overlay that never leaves is worse than an
      effect that never plays.
- **The window** (`DeckardShutterView` + `DeckardOverlayService.shutterParams`): `MATCH_PARENT`,
  `FLAG_NOT_TOUCHABLE` so it cannot take a gesture from the app under it, and
  `FLAG_LAYOUT_IN_SCREEN` + `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` — without those last two it stops
  at the system bars, and an edge effect that misses the edges has nothing left to be. It is removed
  on `onFinished`, not when the run is closed: the release has to finish playing first.
- **The re-entrancy trap**: a second summon cancels the first, whose `finally` then runs *after* the
  second has opened its own shutter. Each run carries a token and only closes the shutter while it
  still owns it. Get this wrong and a full-screen window is left over every app the user opens.
- **Trying them** — the picker on the settings screen, with a live miniature per row; tapping a row
  selects it *and* plays it full-size over the Activity, so a variant costs no model and no Pangram
  call. Over a real app, which is the only place the edges read correctly,
  `scripts/deckard effect <name>` picks one and `scripts/deckard shutter [package]` fires it: the
  read that normally triggers one wants the 2.4–3.5GB `.litertlm`, so debug builds carry a
  runtime-registered broadcast receiver that just plays it.
- **Open**: the ink follows the *device* theme, not the app's. An effect reads over an app whose
  ground matches the device theme and washes out when they diverge — a light-themed phone over a
  dark app draws near-black on near-black. Whatever fixes that applies to all four, since all four
  are staying (COS-247).

### Deckard's look — `design/theme/` + `mascot/DeckardLook.kt`

The overlay draws on top of arbitrary apps, so it can't inherit its surroundings — it carries its
own register, and the setup screen wears it too so the app and the overlay read as one product. That
register **is** the Material scheme rather than a palette sitting beside one: `design/theme/Color.kt`
holds the paint and `AppTheme` (`design/theme/Theme.kt`) maps it onto the M3 roles, so stock
components inherit it and nothing has to be hand-plumbed at the call site.

- **The mapping**: paper → `surface`/`background`, ink → `onSurface`/`primary`, inkMuted →
  `onSurfaceVariant`/`outline`, hairline → `surfaceVariant`/`outlineVariant`. Read them through
  `MaterialTheme.colorScheme`. **`ink` and `paper` invert with the theme**, which is what makes the
  mascot's plate work: it's drawn in `onSurface`, so it's near-black over a light app and near-white
  over a dark one, in contrast either way. There are no accent roles — `primary` is ink, because the
  one filled control in the product is drawn in it.
- **`StampInks`** (`ai` / `assisted` / `human`) is the one thing that can't be a scheme role: M3 has
  a single `error` and no success or warning counterpart, so a three-way verdict has nowhere honest
  to sit and borrowing `error`/`tertiary`/`secondary` would be a naming lie. `AppTheme` provides it
  off the **same `useDarkTheme` parameter** as the scheme, so the two can never disagree; reach it
  as `MaterialTheme.stampInks`.
- Type (`mascot/DeckardLook.kt`): a heavy, tight, upper-case sans for the stamp, plain sans for body,
  and **monospace for the machine's readings** (word count, model version, confidence) — those are
  data, so they're set as data. Actions are never monospace.
- `SpeechBubbleShape` — the tailed shape worn by both the bubble and the report card, so whatever
  Deckard is showing points back at him instead of floating beside him.
- Everything carries a 1dp `outlineVariant` border as well as a shadow: on a dark device the card's
  paper and the app behind it are both near-black and a shadow doesn't read, so the edge does the
  work.
- Launcher icon (`res/drawable-v24/ic_deckard_launcher_*`, adaptive + monochrome): the same idea as
  the card — a page with a verdict stamped across it, on the ink ground. The app theme
  (`Theme.Deckard`) is DayNight so the window behind a dark composition isn't white.

### Copy — `app/res/values/strings.xml` + `:textresource`

Every word the product says is a string resource. Nothing is a Kotlin literal, with three
deliberate exceptions: **Pangram's own text** (`headline`, `confidence`, `prediction`,
`analyzedText`) is server-authored and rendered verbatim; the wire tokens in `SlopVerdictMapper`
(`"AI"`/`"Mixed"`/`"Human"`) are protocol, not copy; and glyph ornament (`"01"`, `"✓"`, `"—"`, the
`%` after the human share, the vendor name `PANGRAM`) is not language.

The resources group by where they're said: `voice_*`, `setup_*`, `card_*`, `stamp_*`. The shutter
picker's row names are `settings_shutter_*` and belong to the settings screen, not to Deckard —
they name an effect, they are not something he says, so they never go through `DeckardVoice`.

- **`mascot/DeckardVoice.kt`** decides *which* line each fact gets; `strings.xml` decides *what* the
  line is. The `when`s are exhaustive over the sealed types, so an unwritten line is a compile error
  rather than a blank bubble.
- It returns a **`TextResource`**, not a `String`, because most of what Deckard says is decided away
  from the UI — by `DeckardOverlayService`, by `ShareTextActivity` — and those have no business
  resolving copy. Whoever draws it resolves it: `asString()` in a composition,
  `asString(context)` anywhere else. `ShareTextActivity` is the case that earns the type: a plain
  `Activity` with no composition that still has to speak.
- **`:textresource`** is that type: a `fun interface` over `Resources` with `raw` and `simple`
  factories, vendored and trimmed from
  [dkmarkell/textresource](https://github.com/dkmarkell/textresource) (MIT — see
  `THIRD_PARTY_NOTICES.md`) rather than depended on, because the core is one file. Factory instances
  are backed by private data classes so they have **value equality** — safe in `DeckardState`, which
  rides a `StateFlow`. A SAM instance has reference equality only.
- It resolves against **`Resources`**, not `Context`, which is the seam Compose's own
  `stringResource()` reads. That makes the composable extension one line over `LocalResources` —
  the local that invalidates its readers on a configuration change, where `LocalContext` (a
  `staticCompositionLocalOf`) does not.
- Two traps, both load-bearing:
    - `android.nonFinalResIds=true` in `gradle.properties`, so `R.string.*` is not a compile-time
      constant — the voice's lines cannot be `const val`.
    - `app_name` is a per-build-type `resValue` ("Deckard Debug" on debug), so the setup screen's
      wordmark is its own `setup_wordmark` and must never point at `@string/app_name`.
- The one formatted string is `voice_too_thin` (`%1$d`, `MIN_WORDS_TO_DETECT`). Note that routing
  args through `TextResource.simple(resId, vararg)` hides them from lint's `StringFormatMatches`,
  so a mismatch there is a runtime `IllegalFormatException`, not a build failure.

### Screen reading — `slop/` + `accessibility/`

- `slop/ScreenTextReader` is the seam (returns `slop/ScreenReadResult`). Two impls behind Hilt
  qualifiers in `di/ScreenTextModule.kt`, both over one screenshot path (they share `ocrRead()`):
    - **`OcrContentScreenTextReader`** (`@OcrContentScreenText`, **in use**): grabs a screenshot
      via `accessibility/ScreenshotCapturer` and asks `LlmEngine.generateWithImage` to **isolate
      the single main post verbatim** out of it (`OcrPrompt.extractMainContent()`) — content
      isolation at the vision step, no per-app knowledge needed. The verbatim rule is load-bearing:
      if the model rewrote the text it'd bias Pangram toward "AI". Slow (a vision inference per
      summon) and hard-requires a loaded model.
    - **`OcrScreenTextReader`** (`@OcrScreenText`, fallback): same screenshot, but the prompt
      (`OcrPrompt.transcribe()`) dumps **all** the readable text rather than isolating one post. A
      screenshot is the visible viewport only, so it captures just what the user sees. Nothing
      injects it today; swap it in by flipping the qualifier in the service.
- `accessibility/DeckardAccessibilityService` exists for one call only an `AccessibilityService`
  can make, `takeScreenshot`, and registers `ScreenshotCapturer`'s handler for it (JPEG,
  downscaled). It reads no window content and listens to no events, and
  `accessibility_service_config.xml` declares only `canTakeScreenshot`. The user must enable it
  under Settings → Accessibility; capture stays on-device.
- **The accessibility-tree reader is gone** (COS-249, 2026-09-14). It read the foreground app's
  a11y tree through per-app extractors (LinkedIn, X, a generic viewport walk) — fast and free, but
  every app was a fresh reverse-engineering job against a tree that changes when the app ships, and
  a wrong read hands the detector chrome, which is worse than no read. The last commit that has it
  is tagged **`a11y-reader`**, kept for the post (COS-225). Do not bring it back; do not add a
  per-app extractor. If a screen reads badly, it is the OCR prompt's problem, not a per-app one.

### Slop detection — `slop/AiDetectorRepository.kt`

- The provider-agnostic boundary: callers hand it the on-screen text and get back a domain
  `slop/DomainSlopVerdict` (API → domain only, no UI knowledge). Backed by **Pangram**
  (`net/PangramService`, base URL + `x-api-key` auth interceptor in `di/NetworkModule.kt`, key from
  `BuildConfig.AI_DETECTOR_API_KEY`). Pangram is async: `detect()` does `POST /task` then polls
  `GET /task/{id}` until a terminal stage, mapping success via `slop/SlopVerdictMapper`
  (`ApiPangramDetection` → `DomainSlopVerdict`). Both repo and use case are main-safe
  (`withContext(io)`).
- **The detector is pinned to `pangram-4`**, sent as `model` on every `POST /task`. Sending no model
  is not an error — Pangram just answers with its default, an older detector (`version` 3.3.2 against
  pangram-4's 4.0) — so the app and the site (`blog/scripts/pangram.mjs`, which pins the same model)
  would judge the same passage with two different detectors and disagree. `detect()` checks
  `GET /models` once per process before the first submission, so a key without access fails by name
  rather than with an opaque HTTP error mid-detection. `SlopDetectionContractTest`'s fixtures are
  real pangram-4 payloads and assert `version` 4.0; `AiDetectorRepositoryTest` asserts the request
  carries the model at all, which is the regression nothing else would catch.
- **Seeing a verdict without spending a Pangram call** (Pangram bills ~5¢ per 100 words):
  `./gradlew :app:installDebug -PmockVerdict=ai|assisted|human|mixed` stamps a canned verdict
  instead of calling out. The property lands in `BuildConfig.MOCK_VERDICT` — `off` in `defaultConfig`
  and overridden only in the **debug** build type, so a release build ignores the flag entirely.
  `AiDetectorRepository.mockedDetection` returns an *API* payload, not a domain verdict, so the real
  `SlopVerdictMapper` still derives the label and the mock can't disagree with the app. `mixed` is
  the one that shows all three inks (a single-label verdict hides `SlopReportCard`'s composition bar
  by design). Without a key in `local.properties` the real path fails at the network and Deckard says
  he couldn't reach the oracle, which is a fine check of the bubble but never reaches the card.
- `slop/DetectSlopUseCase` is the **domain → UI** seam the overlay calls (never the repository
  directly): it returns a `slop/SlopCheck` (`Judged(mascot/UiSlopVerdict)` / `NotEnoughText` /
  `Failed`). It gates on `MIN_WORDS_TO_DETECT` (50, in `slop/WordCount.kt`) — text below the
  threshold returns `NotEnoughText` without hitting Pangram (50 is Pangram's own floor, the same one
  the site's `scripts/pangram.mjs` enforces) — then maps `repository.detect(text)` → `it.toUi()`. To
  support the report card it requests a public dashboard link (`publicDashboardLink = true`) and the
  mapper derives `version`, `wordCount`, `analyzedText`, overall `confidence` (from the longest
  `window`), and the three-way `label`.
- **The verdict is three-way, not a boolean.** `slop/SlopLabel` (`AI` / `ASSISTED` / `HUMAN`) is
  read straight off Pangram's own overall call — `prediction_short` is `AI` / `Mixed` / `Human`,
  and `Mixed` is the middle. It rides through `DomainSlopVerdict.label` → `UiSlopVerdict.label` and
  picks the ink via `mascot/forLabel`; a response missing `prediction_short` falls back to the
  largest of the three fractions. Collapsing this to `isAi` early was the old bug: `StampInks.assisted`
  was unreachable and a half-written passage was stamped as wholly machine-written.
- The verdict renders as `mascot/SlopReportCard`, shown via `DeckardState.Verdict`: a marked-up
  document rather than a reproduction of Pangram's dashboard — the judgement lands as a **stamp**
  (a ruled box tilted off the grid), the judged passage sits under it behind a side rule, and the
  machine's readings run along the bottom in monospace. The stamp pairs Pangram's own `headline`
  phrase ("Mostly Human Written", "Lightly AI-Assisted") with **the human share, always**, labelled
  as such — the same pairing the site's Pangram badge uses (`blog/src/lib/pangram.ts`), and the
  reason the word and the figure can't contradict each other. The three-part composition bar appears
  only when the text is actually a mixture; under a single-label verdict the stamp already says it.

### LiteRT-LM / on-device GPU (hard-won, easy to get wrong)

`llm/LlmEngine.kt` wraps the LiteRT-LM `Engine`. The non-obvious constraints:

- **GPU needs `<uses-native-library>` in `AndroidManifest.xml`.** The GPU delegate (ML Drift) is
  OpenCL; on Android 12+ the app can't `dlopen` the vendor `libOpenCL.so` unless declared
  (`libOpenCL.so`, `libvndksupport.so`, `libcdsprpc.so`, `libedgetpu_litert.so`, all
  `required="false"`). Without it, GPU init fails with an opaque `INTERNAL` error and silently
  falls back to CPU (~10× slower). This was the single hardest bug in the project.
- **Pin `litertlm = "0.11.0"`** — 0.12.0 regressed GPU for these Gemma builds. (0.11.0 matches AI
  Edge Gallery, the reference app we diffed against.)
- **Warm-up runs in `@ApplicationCoroutineScope`.** Engine init takes seconds and `engineOrNull()`
  is non-blocking (returns null until ready), so callers degrade gracefully while it loads instead
  of
  blocking.
- `EngineConfig`: set `maxNumTokens` (KV-cache size), `cacheDir = null` (the GPU weight cache lands
  next to the model in external storage), and for the multimodal Gemma on GPU set
  `visionBackend = GPU`, `audioBackend = CPU`. Backend order tried GPU → CPU → NPU (NPU isn't
  supported by these Gemma `.litertlm` builds).

## Testing

`CoroutinesTestRule(eager = …)`: `eager = true` (default) → `UnconfinedTestDispatcher` (runs
immediately; for hot flows / fire-and-forget launches); `eager = false` → `StandardTestDispatcher`
(manual virtual-clock; for asserting debounce timing).

Code comments: KDoc on classes and public functions only, no inline narration inside function
bodies.

## Logging

`logDebug { … }` (`com.costafotiadis.logging`) is the standard logger. It plants Timber's
`DebugTree` (in `AppInitializer`, debug builds only), so the tag is the calling class's simple name
(e.g. `LlmEngine`, `DeckardOverlayService`). The native LiteRT runtime logs under `litert` /
`litert-lm`.

## Where it stands

The pivot + rename are done and the build is green (`:app:compileDebugKotlin`,
`:app:testDebugUnitTest`, `:app:lintDebug`). End to end today: summon Deckard (a long-press on
the edge tab) → **screenshot OCR content-isolation** (announced with the chosen `shutter/` effect)
→ **Pangram detection** → the bubble shows the verdict as a **report card**
(`mascot/SlopReportCard`).

Done: the API→domain→UI wiring (`AiDetectorRepository` + `DetectSlopUseCase`, base URL/auth in
`NetworkModule`), the report-card UI, Deckard's look (see above), content isolation on the
screenshot path, the four screenshot effects and the picker
that chooses between them (see *The shutter* above), the settings
screen the picker now lives on and the Navigation 3 back stack behind it (see *The Activity's two
screens* above), and every word the app says now living in `strings.xml` behind `:textresource`
(see *Copy* above).

Cleared out along the way: the JSONPlaceholder/Todo demo (repository, mapper, domain/API models,
service, the `jsonPlaceHolderRepository()` entry-point method, `NetworkModule`'s todo wiring,
`:work`'s `ExpeditedGetTodoWorker`, the dead strings), `drawable/cheems.jpg`, the duplicate template
theme under `ui/ui/theme/`, `:design`'s template teal (the `md_theme_*` colours and the
`Theme.Template` / `AppTheme` / splash XML styles that consumed them), the custom `:lint` module
(it held only the Android Studio sample detector, and its Java/Kotlin JVM targets disagreed, which
broke `lintDebug` outright), and the per-app language plumbing (`ProdLocaleManager` and its Hilt
module, the `app_language`/`app_region` strings and every `values-en`/`values-en-rAU`/`values-it`
dir, `locales_config.xml` with the manifest's `localeConfig` and `AppLocalesMetadataHolderService`,
and the `localeFilters` — activity-lifecycle machinery reading two strings into a `StateFlow`
nothing ever collected, in a product whose only surface is an overlay with no Activity at all).

**What's left is on the board, not in this file.** See below.

## Board

This repo is the Linear project **deckard** on Costa's public board
(https://www.costafotiadis.com/board/). The `board` skill has the commands. Issues for this repo
carry that project and one area label (`android`).

**All future work lives there, not in this file.** No TODO sections, no roadmaps, no "next session"
lists on disk — a plan is an issue's description, a roadmap is issues in the project. Follow-ups (a
deferred fix, a check that waits on something external, a TODO you were about to write down) become
an issue before the session ends, and the closing message names it. A note in this file is not a
substitute.
