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
on the left-edge tab summons the mascot (a11y-tree read); a **long-press** on the tab summons via
screenshot OCR content-isolation instead.

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
- The edge tab carries **two gestures**: a left→right **swipe** summons via the a11y-tree read
  (`@AccessibilityScreenText`); a **long-press** (with a haptic tick) summons via the screenshot OCR
  read (`@OcrContentScreenText`). The service injects both readers and routes each gesture through
  `runDetection(reader)` → `detectSlop(reader)`. The two `pointerInput`s don't collide — a swipe
  moves past touch slop (cancelling the long-press), a hold stays put (no drag).
- Requires the draw-over-apps permission (checked in `onCreate`) and the accessibility service
  (for reading the screen). Started/stopped from `MainActivity`'s setup screen.

### Screen reading — `slop/` + `accessibility/`

- `slop/ScreenTextReader` is the seam (returns `slop/ScreenReadResult`). Three impls behind Hilt
  qualifiers in `di/ScreenTextModule.kt`:
    - **`AccessibilityScreenTextReader`** (`@AccessibilityScreenText`, **in use — swipe**): pulls
      the
      visible
      on-screen text straight from the foreground app's a11y node tree via
      `accessibility/ScreenTextCapturer`. No model, no screenshot — returns in milliseconds. The
      chrome-trimming lives in the service (see below).
    - **`OcrContentScreenTextReader`** (`@OcrContentScreenText`, **in use — long-press**): grabs a
      screenshot via `accessibility/ScreenshotCapturer` and asks `LlmEngine.generateWithImage` to
      **isolate the single main post verbatim** out of it (`OcrPrompt.extractMainContent()`) —
      content
      isolation at the vision step, no per-app extractor needed. The verbatim rule is load-bearing:
      if
      the model rewrote the text it'd bias Pangram toward "AI". Slow (a vision inference per summon)
      and hard-requires a loaded model.
    - **`OcrScreenTextReader`** (`@OcrScreenText`, fallback): same screenshot path (the two OCR
      readers
      share one `ocrRead()`), but the prompt (`OcrPrompt.transcribe()`) dumps **all** the readable
      text
      rather than isolating one post. A screenshot is the visible viewport only, so it captures just
      what the user sees. Swap it onto a gesture by flipping a qualifier in the service.
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
      (`sdui:lazyColumn`, mostly bare `android.view.View`s with no ids). A post body is one large
      node but its class varies (`TextView` *or* clickable `Button`), so it matches on **content,
      not
      class** — any visible node carrying text. A collapsed post's visible `text` ends in "… more"
      while the full post sits in `contentDescription`, so it reads the **fuller of text /
      contentDescription**. Selection follows what the user centres: the content node under the
      **screen centre**, falling back to largest viewport overlap (so a big neighbour can't steal a
      centred post). Author/timestamp and comment/post-detail screens are TODO.
  - **`XContentExtractor`** (`com.twitter.android`): the hostile case. On the timeline a tweet
    exposes **no per-element text nodes** — X concatenates the whole card (name, `@handle`,
    "Verified", "Replying to …", body, "Reposted by …", timestamp, engagement counts) into the card
    node's single `contentDescription`, so the body is parsed *out of that one blob* with
    end-/start-anchored regexes: strip the trailing metrics/timestamp/"Reposted by …" and the
    leading byline (verified **and** unverified) + "Replying to …". Selection mirrors LinkedIn
    (centred card, else largest overlap); promoted cards ("Promoted.") are skipped. A **quote
    tweet** packs two authors into one desc and X embeds only a *truncated preview* of the quoted
    original, so we judge the quoter's **own comment** (the text after "Added"), falling back to the
    preview only when there's none. Detail screens reuse the same path. **This single-blob layout
    makes perfect extraction a long-tail game, so it's frozen at good-enough** (the common centred
    tweet, locked by fixture tests). Known gaps: absolute timestamps ("Jun 14") aren't stripped, and
    a display name containing a "." can defeat the byline strip.
- **Discovery loop**: on summon (debug builds only) `DeckardAccessibilityService.dumpTree` writes
  the
  active-window tree + every window's tree to `…/files/deckard_tree.txt`, which we `adb pull` and
  read. It's a file (not logcat) because **some devices encrypt logcat** (e.g. Honor) and because
  the
  file is the exact `ScreenNode` snapshot the extractor saw. `adb shell uiautomator dump` is a
  zero-code cross-check. See the runbook below.
- `slop/ContentExtractor` (+ `ContentExtractionPrompt`) is dormant: it would isolate the main
  post/article text from a noisy **a11y** capture **verbatim** (the model selects which captured
  lines
  are content; it never rewrites them, which would bias detection toward "AI") before handing it to
  the detector. The **long-press OCR path now does this same isolation at the vision step** (over a
  screenshot instead of a text capture) — see `OcrContentScreenTextReader` above.

### Adding / tuning a per-app extractor (runbook)

The repeatable loop for a new app (X, Reddit, …) or fixing an existing one. Needs a connected device
(`adb devices`) and a debug build.

1. **Install & enable.** `./gradlew :app:installDebug`. Grant overlay + accessibility (once per
   install) via adb — note this **overwrites** the enabled-a11y-services list, so re-enable any
   others (e.g. TalkBack) afterwards:
   ```
   PKG=com.markedusduplicate.deckard.debug
   SVC=$PKG/com.markedusduplicate.deckard.accessibility.DeckardAccessibilityService
   adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
   adb shell settings put secure enabled_accessibility_services $SVC
   adb shell settings put secure accessibility_enabled 1
   ```
   (Changing `accessibility_service_config.xml` only takes effect after the service re-binds —
   toggle it off/on by re-running the `settings put` lines.) Then open the app → **Start Deckard**.
2. **Capture.** On the device, navigate to the exact screen/state to debug (e.g. a post with
   "… more"), centre it, and **summon Deckard** (left-edge swipe). That writes the dump. Pull it:
   ```
   adb pull /sdcard/Android/data/$PKG/files/deckard_tree.txt
   ```
   The file has the **extracted** text (what the user got), the **active-window** tree (what the
   extractor saw), and **all windows** (reveals content in a separate window, e.g. a bottom sheet).
3. **Diagnose & write.** Read the dump: find the node holding the real content (check `class`, the
   `viewId`, `text` vs `desc` — LinkedIn hides the full post in `desc`), and why the extractor
   missed
   it. Write/adjust the `ScreenContentExtractor`; for a new app add the class + one
   `@Binds @IntoSet`
   in `di/ScreenTextExtractorsModule`.
4. **Lock it in with a test.** Hand-build a `ScreenNode` fixture from the dump (the `node(…)` helper
   in `app/src/test/.../accessibility/TestNodes.kt`) and assert the extractor's output. Run
   `./gradlew :app:testDebugUnitTest --tests "*<App>ContentExtractorTest"`.
5. **Verify on device.** Re-`installDebug`, summon on the same screen, confirm the verdict reads the
   right text. Iterate from step 2.

### Slop detection — `slop/AiDetectorRepository.kt`

- The provider-agnostic boundary: callers hand it the on-screen text and get back a domain
  `slop/DomainSlopVerdict` (API → domain only, no UI knowledge). Backed by **Pangram**
  (`net/PangramService`, base URL + `x-api-key` auth interceptor in `di/NetworkModule.kt`, key from
  `BuildConfig.AI_DETECTOR_API_KEY`). Pangram is async: `detect()` does `POST /task` then polls
  `GET /task/{id}` until a terminal stage, mapping success via `slop/SlopVerdictMapper`
  (`ApiPangramDetection` → `DomainSlopVerdict`). Both repo and use case are main-safe
  (`withContext(io)`).
- `slop/DetectSlopUseCase` is the **domain → UI** seam the overlay calls (never the repository
  directly): it returns a `slop/SlopCheck` (`Judged(mascot/UiSlopVerdict)` / `NotEnoughText` /
  `Failed`). It gates on `MIN_WORDS_TO_DETECT` (50, in `slop/WordCount.kt`) — text below the
  threshold returns `NotEnoughText` without hitting Pangram — then maps `repository.detect(text)` →
  `it.toUi()`. To support the
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
`:app:testDebugUnitTest`). End to end today: summon Deckard → **a11y-tree screen read** (swipe) **or
screenshot OCR content-isolation** (long-press) → **Pangram detection** → the bubble shows the
verdict
as a Pangram-style **report card** (`mascot/SlopReportCard`).
Steps 1–2 below are done (API→domain→UI wiring via `AiDetectorRepository` + `DetectSlopUseCase`,
base
URL/auth in `NetworkModule`, and the report-card UI). Remaining, in rough priority:

3. **Deckard's voice.** The bubble copy is plain. Give him the weary-scholar persona: verdict lines
   like *"Slop, my son. (91.7%)"*, the confidence score deadpan, and his own catchphrase — keep the
   wise-elder archetype but avoid Blizzard's literal Deckard-Cain tells ("stay awhile and listen",
   robed-Horadrim imagery). Lands wherever the verdict is rendered (step 2).
4. **Content isolation.** Done for the **screenshot** path: a **long-press** on the edge tab runs
   `OcrContentScreenTextReader` (`OcrPrompt.extractMainContent()`), which has the on-device model
   pick
   the main post out of the screenshot verbatim before detection — no per-app code (verified working
   on device). The text-only `slop/ContentExtractor` (+ `ContentExtractionPrompt`) stays dormant; it
   would do the same over a noisy a11y capture.
5. **Per-app extractors (`accessibility/extract/`).** The in-use a11y reader now dispatches by
   foreground package to a per-app `ScreenContentExtractor`. **LinkedIn** (`com.linkedin.android`)
   and **X** (`com.twitter.android`) are done — most-visible / centred post (see their bullets
   above; X's single-blob `contentDescription` is frozen at good-enough). Next, one extractor each
   for **Reddit, Substack, Medium** (capture the tree via `uiautomator dump`, add a class +
   `@IntoSet` binding + fixture test) — **Reddit** is the friendliest next target (native, id-rich
   tree, more like LinkedIn than X). LinkedIn polish: include author/timestamp, handle "…more"
   truncation, confirm post-detail screens. The OCR reader remains a fallback behind
   `@OcrScreenText`.

**Cruft worth deleting** (template/pivot leftovers, unused by the detector): the
JSONPlaceholder/Todo
demo — `domain/JsonPlaceHolderRepository`, `domain/TodoMapper`, `domain/model/DomainTodo`,
`net/JsonPlaceHolderService`, `net/model/ApiTodo`, the `jsonPlaceHolderRepository()` entry-point
method in `DeckardApplication`, `NetworkModule`'s todo wiring, `:work`'s `ExpeditedGetTodoWorker`,
and
the `get_todo` / `title_activity_second|third` strings. Also: `LlmEngine` + `OcrPrompt` still live
under `suggestion/llm/` (a vestigial keyboard-era package name) — consider moving them to `llm/`.
