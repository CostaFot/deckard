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
- `mascot/DeckardEdgeHandleView.kt` declares `setSystemGestureExclusionRects` so its left→right
  swipe
  isn't eaten by the Android 10+ system back gesture (exclusion budget is ~200dp/edge, so the tab is
  short).
- The edge tab carries **two gestures**: a left→right **swipe** summons via the a11y-tree read
  (`@AccessibilityScreenText`); a **long-press** (with a haptic tick) summons via the screenshot OCR
  read (`@OcrContentScreenText`). The service injects both readers and routes each gesture through
  `runDetection(reader)` → `readScreenAndJudge(reader)`. The two `pointerInput`s don't collide — a
  swipe
  moves past touch slop (cancelling the long-press), a hold stays put (no drag).
- Requires the draw-over-apps permission (checked in `onCreate`) and the accessibility service
  (for reading the screen). Started/stopped from `MainActivity`'s setup screen.

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

`logDebug { … }` (`com.markedusduplicate.logging`) is the standard logger. It plants Timber's
`DebugTree` (in `AppInitializer`, debug builds only), so the tag is the calling class's simple name
(e.g. `LlmEngine`, `DeckardOverlayService`). The native LiteRT runtime logs under `litert` /
`litert-lm`.

## Where it stands

The pivot + rename are done and the build is green (`:app:compileDebugKotlin`,
`:app:testDebugUnitTest`, `:app:lintDebug`). End to end today: summon Deckard → **a11y-tree screen
read** (swipe) **or screenshot OCR content-isolation** (long-press) → **Pangram detection** → the
bubble shows the verdict as a **report card** (`mascot/SlopReportCard`).

Done: the API→domain→UI wiring (`AiDetectorRepository` + `DetectSlopUseCase`, base URL/auth in
`NetworkModule`), the report-card UI, Deckard's look (see above), content isolation on the
screenshot path, and two per-app extractors (LinkedIn, X). His **voice** is not — only the thinking
state was rewritten.

Cleared out along the way: the JSONPlaceholder/Todo demo (repository, mapper, domain/API models,
service, the `jsonPlaceHolderRepository()` entry-point method, `NetworkModule`'s todo wiring,
`:work`'s `ExpeditedGetTodoWorker`, the dead strings), `drawable/cheems.jpg`, the duplicate template
theme under `ui/ui/theme/`, `:design`'s template teal (the `md_theme_*` colours and the
`Theme.Template` / `AppTheme` / splash XML styles that consumed them), and the custom `:lint` module
(it held only the Android Studio sample detector, and its Java/Kotlin JVM targets disagreed, which
broke `lintDebug` outright).

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
