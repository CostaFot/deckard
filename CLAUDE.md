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
a small setup screen (`MainActivity`) for granting permissions and starting/stopping the overlay.

> History: this started as a custom soft-keyboard (IME) and was pivoted to the slop detector. The
> keyboard, its predictive-suggestion engine (dictionary + n-gram learning), and the Room DB have
> been removed. Only the on-device LLM layer (`LlmEngine`) survives from that era, reused for OCR.
> If you find lingering keyboard references, they're stragglers worth cleaning up.

- applicationId / namespace: `com.markedusduplicate.deckard` (debug variant: `.debug`)
- Build variants: `debug` / `release` only (no product flavors)
- DI: Hilt. App class: `DeckardApplication` (`@HiltAndroidApp`)

## Module layout

- `app` — the slop detector (overlay + accessibility services) + the setup `MainActivity`
- `design` — theme/UI (`AppTheme`)
- `common`, `common-test`, `logging`, `work`, `auth`, `testing` — shared libs
  (namespaces stay `com.markedusduplicate.*`; only the app/template packages were renamed to
  deckard)
- `build-logic/convention` — Gradle convention plugins (`application.common`,
  `application.compose.common`, `hilt.common`, `library.common`, `library.compose.common`)

## Build / run

Gradle uses the Android Studio JBR — **`JAVA_HOME` must be set** or `./gradlew` fails with
"JAVA_HOME is not set". The user sets it themselves.

- Compile: `./gradlew :app:compileDebugKotlin`
- Build APK: `./gradlew :app:assembleDebug`
- Install: `./gradlew :app:installDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`

To use it after install, open the app and work through `MainActivity`'s setup screen: enable the
accessibility service (screen reading), grant draw-over-apps, then start Deckard. A left→right swipe
on the left-edge tab summons the mascot.

The on-device LLM is required for OCR (the in-use screen reader), but optional to
*launch*: with no model present, summoning Deckard reports it has no brain yet. To enable it,
`adb push` a `.litertlm` into `/sdcard/Android/data/<applicationId>/files/models/` (≈2.4–3.5 GB; the
`LlmEngine` loads the first `.litertlm` it finds there). For the **debug** build `<applicationId>`
is
`com.markedusduplicate.deckard.debug`, so the dir is
`/sdcard/Android/data/com.markedusduplicate.deckard.debug/files/models/` — create it with
`adb shell mkdir -p` and push to a full filename (a trailing-slash dest fails with "Is a directory"
if `models/` doesn't exist yet). `LlmEngine` warms up once per process and caches the loaded engine,
so after pushing a new model `am force-stop` (or reinstall) to reload it. Local `.litertlm` files
under `model/` are gitignored.

## Architecture notes (the non-obvious bits)

### Overlay service — `mascot/DeckardOverlayService.kt`

- A plain started `Service` (no Activity host) that hosts two `WindowManager` overlay windows: the
  draggable mascot (`mascot/DeckardComposeView.kt`, an emoji + speech bubble / verdict report card
  with an always-present X close button), and the always-present left-edge summon tab
  (`mascot/DeckardEdgeHandleView.kt`).
- An overlay service has no lifecycle/decor-view callbacks, so the service implements
  `LifecycleOwner` + `ViewModelStoreOwner` + `SavedStateRegistryOwner` itself, drives its own
  `LifecycleRegistry` to RESUMED, and sets the view-tree owners directly on each overlay view — all
  required for Compose to compose and recompose.
- `mascot/DeckardEdgeHandleView.kt` declares `setSystemGestureExclusionRects` so its left→right
  swipe
  isn't eaten by the Android 10+ system back gesture (exclusion budget is ~200dp/edge, so the tab is
  short).
- Requires the draw-over-apps permission (checked in `onCreate`) and the accessibility service
  (for reading the screen). Started/stopped from `MainActivity`'s setup screen.

### Screen reading — `slop/` + `accessibility/`

- `slop/ScreenTextReader` is the seam (returns `slop/ScreenReadResult`). Two impls behind Hilt
  qualifiers in `di/ScreenTextModule.kt`:
    - **`AccessibilityScreenTextReader`** (`@AccessibilityScreenText`, **in use**): pulls the
      visible
      on-screen text straight from the foreground app's a11y node tree via
      `accessibility/ScreenTextCapturer`. No model, no screenshot — returns in milliseconds. The
      chrome-trimming lives in the service (see below).
    - **`OcrScreenTextReader`** (`@OcrScreenText`, fallback): grabs a screenshot via
      `accessibility/ScreenshotCapturer` and asks `LlmEngine.generateWithImage` to transcribe it
      (`suggestion/llm/OcrPrompt`). A screenshot is the visible viewport only, so it captures just
      what the user sees. Accurate but slow (a vision inference per summon) and hard-requires a
      loaded
      model. Swap back by flipping the overlay's qualifier to `@OcrScreenText`.
- `accessibility/DeckardAccessibilityService` registers both on-demand bridges (only an
  `AccessibilityService` can `takeScreenshot` or read `rootInActiveWindow`): `ScreenshotCapturer`
  (JPEG for OCR) and `ScreenTextCapturer` (tree text). Its `captureScreenText` snapshots
  `rootInActiveWindow` into a framework-free `accessibility/tree/ScreenNode` (via
  `ScreenNodeSnapshot`)
  and dispatches by foreground package to a **per-app extractor**. The user must enable it under
  Settings → Accessibility; capture stays on-device.
- **Per-app extraction** — `accessibility/extract/`. Each app the user reads in lays its a11y tree
  out differently, so each gets a `ScreenContentExtractor` (pure function over a `ScreenNode`
  snapshot, so it unit-tests against captured trees — no Robolectric/mockk).
  `ScreenContentExtractors`
  picks the first extractor whose `handles(packageName)` is true, else `GenericContentExtractor`
  (viewport-clipped whole-tree / WebView-only walk — the unknown-app fallback). Extractors are
  contributed via Hilt `@IntoSet` in `di/ScreenTextExtractorsModule` (add an app = new class + one
  binding). Shared helpers (`viewport`, `collectVisibleText`, `find`/`findAll`, `mostVisible`) live
  in
  `accessibility/extract/NodeText.kt`.
    - **`LinkedInContentExtractor`** (`com.linkedin.android`): the feed is Jetpack Compose
      (`sdui:lazyColumn`, mostly bare `android.view.View`s; the whole post body is one full-width
      `TextView`), so it returns the **most-visible post** — the visible `TextView` inside the feed
      container with the largest viewport overlap. Author/timestamp and post-detail screens are
      TODO.
- **Discovery loop**: `ScreenNode.toDebugString()` is dumped to logcat on summon (debug only — the
  `logDebug` lambda is lazy) to design extractors against real trees. `adb shell uiautomator dump`
  is the alternative capture (and the only one on devices that **encrypt logcat**, e.g. some Honor
  devices).
- `slop/ContentExtractor` (+ `ContentExtractionPrompt`) is dormant: it would isolate the main
  post/article text from a noisy capture **verbatim** (the model selects which captured lines are
  content; it never rewrites them, which would bias detection toward "AI") before handing it to the
  detector.

### Slop detection — `slop/AiDetectorRepository.kt`

- The provider-agnostic boundary: callers hand it the on-screen text and get back a domain
  `slop/DomainSlopVerdict` (API → domain only, no UI knowledge). Backed by **Pangram**
  (`net/PangramService`, base URL + `x-api-key` auth interceptor in `di/NetworkModule.kt`, key from
  `BuildConfig.AI_DETECTOR_API_KEY`). Pangram is async: `detect()` does `POST /task` then polls
  `GET /task/{id}` until a terminal stage, mapping success via `slop/SlopVerdictMapper`
  (`ApiPangramDetection` → `DomainSlopVerdict`). Both repo and use case are main-safe
  (`withContext(io)`).
- `slop/DetectSlopUseCase` is the **domain → UI** seam the overlay calls (never the repository
  directly): `repository.detect(text).map { it.toUi() }` → `mascot/UiSlopVerdict`. To support the
  report card it requests a public dashboard link (`publicDashboardLink = true`) and the mapper
  derives `version`, `wordCount`, `analyzedText`, overall `confidence`, and `dominantLabel` (from
  the
  dominant `window`).
- The verdict renders as `mascot/SlopReportCard` — a faithful-core replica of Pangram's short report
  (header, excerpt card, `Canvas` composition gauge, label/confidence row, "View full analysis" /
  "Copy link" buttons) shown via `DeckardState.Verdict`. `isAi`/`aiLikelihood` derivations remain
  provisional.

### LiteRT-LM / on-device GPU (hard-won, easy to get wrong)

`suggestion/llm/LlmEngine.kt` wraps the LiteRT-LM `Engine`. The non-obvious constraints:

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

`logDebug { … }` (`com.markedusduplicate.logging`) is the standard logger. It plants Timber's
`DebugTree` (in `AppInitializer`, debug builds only), so the tag is the calling class's simple name
(e.g. `LlmEngine`, `DeckardOverlayService`). The native LiteRT runtime logs under `litert` /
`litert-lm`.

## Status & what's left (for the next session)

The pivot + rename are done and the build is green (`:app:compileDebugKotlin`,
`:app:testDebugUnitTest`). End to end today: summon Deckard → **a11y-tree screen read** → **Pangram
detection** → the bubble shows the verdict as a Pangram-style **report card** (
`mascot/SlopReportCard`).
Steps 1–2 below are done (API→domain→UI wiring via `AiDetectorRepository` + `DetectSlopUseCase`,
base
URL/auth in `NetworkModule`, and the report-card UI). Remaining, in rough priority:

3. **Deckard's voice.** The bubble copy is plain. Give him the weary-scholar persona: verdict lines
   like *"Slop, my son. (91.7%)"*, the confidence score deadpan, and his own catchphrase — keep the
   wise-elder archetype but avoid Blizzard's literal Deckard-Cain tells ("stay awhile and listen",
   robed-Horadrim imagery). Lands wherever the verdict is rendered (step 2).
4. **(Optional) Content isolation.** `slop/ContentExtractor` (+ `ContentExtractionPrompt`) is built
   but dormant — slot it in before detection so the model judges the main post, not chrome.
5. **Per-app extractors (`accessibility/extract/`).** The in-use a11y reader now dispatches by
   foreground package to a per-app `ScreenContentExtractor`. **LinkedIn** (`com.linkedin.android`)
   is
   done — most-visible post. Next, one extractor each for **X, Reddit, Substack, Medium** (capture
   the
   tree via `uiautomator dump`, add a class + `@IntoSet` binding + fixture test). LinkedIn polish:
   include author/timestamp, handle "…more" truncation, confirm post-detail screens. The OCR reader
   remains a fallback behind `@OcrScreenText`.

**Cruft worth deleting** (template/pivot leftovers, unused by the detector): the
JSONPlaceholder/Todo
demo — `domain/JsonPlaceHolderRepository`, `domain/TodoMapper`, `domain/model/DomainTodo`,
`net/JsonPlaceHolderService`, `net/model/ApiTodo`, the `jsonPlaceHolderRepository()` entry-point
method in `DeckardApplication`, `NetworkModule`'s todo wiring, `:work`'s `ExpeditedGetTodoWorker`,
and
the `get_todo` / `title_activity_second|third` strings. Also: `LlmEngine` + `OcrPrompt` still live
under `suggestion/llm/` (a vestigial keyboard-era package name) — consider moving them to `llm/`.
