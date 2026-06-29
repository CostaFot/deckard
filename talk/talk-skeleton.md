# Conference Talk — Skeleton (working draft)

**Format:** 15–20 min + Q&A · **Audience:** Android devs · **Project:** Deckard (on-device
AI-slop detector)

> Status: working draft, problem-first reframe. The talk opens by selling a *personal
> problem* (AI slop everywhere, can't tell on your phone), establishes that the detection is
> the *easy* part (an API — Pangram), then spends its time on the two parts that are actually
> interesting to Android devs: **reading the text off the screen reliably** and **running a
> multimodal LLM on the phone**. Overlay/ViewModel plumbing stays a short aside. Revise
> freely.
>
> **This draft is intentionally maximal.** Timings are omitted on purpose — the goal right now
> is to *gather everything* worth saying, then trim. The **Topic reservoir** at the bottom is
> the full catalog of talk-worthy material (tagged ⭐ core / ✅ strong / 🔹 deep cut); the
> running order above it is the current best narrative, pulling from that catalog. Expect it to
> be too much for one talk — that's the point for now.

---

## Title

**Working title:**

> ## "Fighting AI slop with anti-slop"

**Possible subtitles** (to set the dev expectation — the body is the Android engineering):

- *"…the on-device Android engineering behind a slop detector"*
- *"…reading any app's screen with an on-device LLM"*
- *"…where the AI is the easy part"*

**Alternates kept on the table:**

- *"The Accessibility Tree Lied to Me"*
- *"Pixels over Trees: On-Device Screen Understanding on Android"*

Note: the working title sells the *premise* (slop vs. anti-slop). The body is mostly the
*how* (reading the screen + on-device LLM). A subtitle bridges the two so the abstract
doesn't feel like a bait-and-switch — *"where the AI is the easy part"* leans into exactly
the pivot the talk makes.

---

## Synopsis (conference-program abstract)

> You're scrolling LinkedIn on your phone and you hit a post that might be a real person — or
> might be pasted out of ChatGPT. You can't tell. AI "slop" is flooding our feeds, and on a
> phone you have no signal for what's authentic. **Deckard** is my answer: a floating mascot
> that lives over every app, reads whatever's on screen, and tells you whether it's
> AI-generated slop.
>
> The detection itself is the easy part — it's an API call (Pangram: give it 50+ words, get a
> verdict). This talk is about the *hard* parts, the ones that are actually interesting to
> Android devs: **getting the text off the screen** of an app you don't control, and
> **running a multi-gigabyte multimodal LLM on the phone** to do it reliably. I'll show why
> the "correct" approach — the accessibility tree — betrayed me (including the regex horror X
> forced on me), why screenshotting the screen and letting an on-device LLM read the pixels
> was dramatically more reliable, and the gotchas of getting that model onto the GPU —
> including the four lines of XML that were the hardest bug in the project, and the hardware
> floor that means this only works on a recent phone. And I'll make the case for *why local at
> all*: this app holds god-mode permissions, and the only reason that's acceptable is that the
> model reading your screen never sends a thing to the cloud — which is also why almost nobody
> ships something like this. Finally, for the Compose-curious: it all runs in an overlay with
> **no Activity in sight**, which means confronting what Compose actually needs to function — the
> lifecycle, ViewModel-store, and saved-state owners an Activity normally hands you for free —
> and the newer APIs that finally make Compose a true first-class citizen anywhere: the `retain`
> runtime API, `rememberViewModelStoreOwner`, and Navigation 3.

---

## The spine (the through-lines that tie it together)

> **1. Detection is a solved problem you can buy — one API call.** The talk is everything
> *around* it: reading the screen reliably, and running the model that makes that possible.
> Those are the Android problems.
>
> **2.** The "right" way to read the screen is the accessibility tree. The way that actually
> *works* is to screenshot it and let an on-device LLM read the pixels.
>
> **3. Why local at all? Privacy is the architecture, not a feature.** This app holds god-mode
> permissions; the only reason that's acceptable is that the model reading your screen never
> sends a thing to the cloud. That's also why almost nobody ships this — and why on-device
> models only just made it possible.
>
> **4. Why system-wide? Rely on nobody, work everywhere — that's the future.** Not one more app
> with a built-in chatbot, but a single privileged agent that reads *any* app and depends on no
> vendor's cooperation. A Chrome extension does this *in the browser*; Android gives no easy
> cross-app hook, so the accessibility / screenshot / overlay stack is the only way to get
> there — and the payoff is something that scales past any one app or browser.

There are **two kinds of fragility** in tension, and naming both keeps the talk honest:

- The accessibility tree is fragile **across apps** (depends on each app's developer).
- The screenshot + LLM path is fragile **across devices** (needs a powerful, recent phone).

> *"I traded app-fragility for device-fragility."*

---

## Talk skeleton (running order — timings deferred)

> Timings intentionally omitted. This is the current best *narrative order*; the full set of
> material to draw from is in the **Topic reservoir** at the bottom.

### 0. Cold open — the problem, shown live

> 🎬 **Assets:**
> - 🎞️🔥 `[A-01]` **GIF/video — the hero demo:** scroll a slop feed → swipe → verdict card →
    > long-press → on-device read → report card. The single most important asset; record clean,
    > loop it. Have it as a fallback even if you demo live.
> - 📊 `[A-02]` **DIAGRAM (optional):** one-glance architecture — a11y service (eyes) → overlay
    > Service (mascot) → on-device LLM + Pangram (brain). Only if the demo needs framing.

Don't open on an architecture diagram. Open by *being the user*.

- On the mirrored phone, scroll a LinkedIn / X feed. Land on a post that smells AI-ish. Voice
  the doubt out loud: *"Did a person write this, or did they paste it out of ChatGPT? I
  genuinely can't tell anymore — and it's everywhere."*
- *"So I built a thing that tells me."* **Swipe** the edge → 🧙 → verdict card.
  **Long-press** → same answer, but this time a model *on the phone* read the screen first
  (screenshot → on-device OCR) before asking for the verdict.
- Transition: *"That's the whole product. The rest of this talk is the two interesting
  problems hiding inside it — and almost none of it is the AI part."*

### 1. The problem + the off-the-shelf brain

> 🎬 **Assets:**
> - 🖼️ `[A-03]` **IMAGE — "Can you tell which is AI?":** a grid of real LinkedIn/X posts, one or
    > two AI-generated. Run it as an audience quiz, then reveal. Sells the problem viscerally.
> - 📊 `[A-04]` **DIAGRAM — Pangram in one picture:** text (≥50 words) → `POST /task` → poll →
    > verdict. Make the point that this half is *someone else's box*.
> - 💻 `[A-05]` **SNIPPET (optional):** the 50-word gate — `DetectSlopUseCase` /
    > `MIN_WORDS_TO_DETECT` returning `NotEnoughText` before the API is ever called.

Sell the problem, then hand the talk to the Android devs.

- **Generalize the itch (1 min):** AI slop is flooding feeds; on a phone you have no
  authenticity signal. A few example posts. Make them feel it.
- **The seed of the solution — Pangram (45 sec):** an API. Give it ≥50 words of text, it
  returns a verdict (AI-generated vs human). That's the "anti-slop." `AiDetectorRepository`
  wraps it; both screen-read paths end here.
- 🎯 **THE PIVOT — say it explicitly to the room (45 sec):** *"So detection is solved. It's
  one HTTPS call to someone else's GPU. If this were an AI talk, it'd be over. But you're
  Android devs — and the interesting problems are the two things Pangram **can't** do for me:*
    1. *It needs the text — and getting the text off whatever app you're staring at is
       genuinely hard.*
    2. *It needs 50+ words of the **right** text — the post, not the nav bar.*
       *Both of those are on-device Android problems. That's the talk."*
- 🔒 **Plant for the close (one line now):** *"And there's a third constraint that shapes the
  whole design — whatever reads your screen can't be allowed to phone home. I'll pay that off
  at the end. Hold the thought."*

### 2. Pillar B — getting the text off the screen

> 🎬 **Assets (by act):**
> - **Act 1** — 💻 `[A-06]` SNIPPET `AccessibilityScreenTextReader` / `captureScreenText` (read
    > `rootInActiveWindow` → walk the tree). 📊 `[A-07]` DIAGRAM a stylized a11y node tree
    > (labelled boxes) — "structured, free, fast."
> - **Act 2** — 🖼️ `[A-08]` IMAGE LinkedIn dump excerpt: highlight the body hiding in
    > `contentDescription` and the "… more" truncation in `text`. 💻🔥 `[A-09]` **SNIPPET (HERO)**
    > `XContentExtractor.kt` regex wall (`TRAILING_METRICS`, `LEADING_BYLINE`, `QUOTE_LEAD`,
    > `QUOTER_COMMENT`). 🖼️ `[A-10]` IMAGE the raw X `contentDescription` blob, annotated to show
    > name / `@handle` / body / metrics all fused into one string.
> - **Act 3** — 💻🔥 `[A-11]` **SNIPPET (HERO)** `OcrPrompt.extractMainContent()` shown
    > **side-by-side** with the A-09 regex wall — "N parsers → 1 prompt." 🎞️ `[A-12]` GIF
    > long-press on an app you never wrote an extractor for → it isolates the post correctly.

The heart of the talk. A lived-experience arc in three acts.

**Act 1 — The "correct" approach: the accessibility tree**

- The obvious choice: fast, free, structured, no model. An `AccessibilityService` reads
  `rootInActiveWindow`, you walk the node tree, you get text. Milliseconds.
- *"This is what I built first. And it works… on the apps that do accessibility well."*

**Act 2 — …but you're at the mercy of every app's developer**

- The catch: the tree is only as good as the app author made it. You don't control X or
  LinkedIn, and most apps treat a11y as an afterthought.
- **Evidence slide #1 — LinkedIn:** the full post hides in `contentDescription`, not `text`;
  a collapsed post shows "…more" in `text` and the real content in `contentDescription`. You
  write app-specific logic just to *find the body*.
- 🔥 **Evidence slide #2 — X, the horror exhibit:** on the timeline X exposes **no
  per-element text**. The *entire* tweet — name, `@handle`, "Verified", "Replying to…",
  body, "Reposted by…", timestamp, *and* the like/repost/view counts — is concatenated into
  **one `contentDescription` string**. To get the body you parse it back apart with a stack
  of anchored regexes. Put `XContentExtractor.kt` on screen (`TRAILING_METRICS`,
  `LEADING_BYLINE`, `QUOTE_LEAD`, `QUOTER_COMMENT`). Read the comment aloud: *"This
  single-blob layout makes perfect extraction a long-tail game, so it's frozen at
  good-enough."*
- Punchline: *"I was writing a brittle, per-app regex parser for every single app. That's
  not a feature roadmap — that's a treadmill."*

**Act 3 — The fix: screenshot it, let the LLM read the pixels**

- Flip to the long-press path: `OcrContentScreenTextReader` → screenshot →
  `LlmEngine.generateWithImage` → text → off to Pangram.
- **The elegance slide:** the *entire* per-app extraction problem collapses into **one
  prompt** — `OcrPrompt.extractMainContent()`: "find the one main post, copy its body
  **verbatim**, leave out chrome/handles/counts." Show it next to the X regex wall. One
  prompt vs N hand-tuned parsers. No per-app code. Works on apps you've never seen.
- 🆕 **Why it actually works — it's comprehension, not OCR:** the real unlock isn't the
  prompt, it's that modern on-device vision models *understand the screen layout*. Given a
  bit of guidance, the model itself picks out which block is the post you're reading and
  separates it from the chrome — and it won't fuse two unrelated chunks of text into one. It
  does the "which text matters" reasoning I was hand-coding with centre-of-screen heuristics
  and regexes. **And this is recent** — a local model couldn't be trusted to do this a year
  ago. (The code keeps the dumb version for contrast: `OcrPrompt.transcribe()` just dumps all
  the text; the in-use `extractMainContent()` asks the model to *select* the post.)
- Sharp nuance: the prompt forces **verbatim** — if the model rewrites the text it biases the
  detector toward "AI." The LLM is a *reader*, not a summarizer. (`OcrPrompt.kt:21-25`)
- Cliffhanger into Pillar A: *"The fix was to have an LLM read the screen. On the phone.
  Which raises an obvious question…"*

### 3. Pillar A — can you even run an LLM on a phone?

> 🎬 **Assets:**
> - 💻🔥 `[A-13]` **SNIPPET (HERO)** the four `<uses-native-library>` lines
    > (`AndroidManifest.xml`). Title the slide "the hardest bug in the project."
> - 💻 `[A-14]` SNIPPET `LlmEngine.initialize()` — the backend fallback loop (GPU → CPU → NPU).
> - 💻 `[A-15]` SNIPPET `EngineConfig` (`visionBackend = GPU`, `audioBackend = CPU`,
    > `maxNumTokens`, `cacheDir = null`).
> - 📊 `[A-16]` CHART GPU vs CPU inference time — a bar showing the ~10× gap.
> - 🖼️ `[A-17]` IMAGE the hardware floor — flagship ✅ vs ~2-year-old phone ❌ (falls to CPU,
    > unusably slow).

The technical climax. The on-device LLM is revealed here as *what makes the fix possible*.
`suggestion/llm/LlmEngine.kt`.

- **Yes — and here's the shape of it:** a 2.4–3.5 GB multimodal Gemma, takes the *screenshot*
  as input, runs entirely on-device — the screen never leaves the phone to be read. Callback
  to Act 3: this is the class of model now good enough to *comprehend* the screen, not just
  transcribe it — which is the whole reason the screenshot path beats per-app parsing.
- **How it loads:** LiteRT-LM `Engine` + `EngineConfig`; model `adb push`ed to external
  storage, loaded lazily, warmed up on an app-scoped coroutine so callers degrade gracefully
  — `engineOrNull()` returns `null` while loading and never blocks the summon.
  (`LlmEngine.kt:40-48`)
- **The backend fallback:** `initialize()` tries **GPU → CPU → NPU**, keeping the first that
  survives. (`LlmEngine.kt:115-119`, `:87-113`)
- 🔥 **War-story slide — "the single hardest bug was four lines of XML."** GPU is OpenCL; on
  Android 12+ the app can't `dlopen` the vendor `libOpenCL.so` unless it's declared. Without
  it → opaque `INTERNAL` error → silent fall back to CPU → ~10× slower. Show the four
  `<uses-native-library required="false">` lines. (`AndroidManifest.xml:22-34`)
- **Two more real lessons:** pin `litertlm 0.11.0` (0.12.0 regressed GPU for these builds);
  for multimodal set `visionBackend = GPU, audioBackend = CPU`.
- 🔧 **The hardware floor — "same symptom, two causes, only one is your fault":**
    - *Cause 1 (your fault, fixable):* missing `<uses-native-library>` → CPU fallback. Four
      lines of XML fixes it.
    - *Cause 2 (not your fault, not fixable):* the phone is just too old. Even with the manifest
      perfect, a ~2-year-old device has no usable GPU delegate / not enough muscle, so
      `initialize()` walks GPU → **CPU** and lands there — where a 3 GB Gemma is *unusably*
      slow (CPU throttled to ≤4 threads, `cpuThreads()`). The same fallback that saves you on a
      flagship betrays you on an old phone.
    - Land it: *"A big on-device LLM isn't a 'works on Android' feature — it's a 'works on a
      recent, powerful phone' feature."*
- 🧩 **Synthesis slide — the full trade-off (both pillars land here):**

  | | Accessibility tree | Screenshot + on-device LLM |
    |---|---|---|
  | Speed | Milliseconds | A vision inference per summon (slow) |
  | **Device requirement** | **Any phone** | **Recent, powerful phone — hard floor** |
  | Reliability | At the mercy of each app | Reads what the user sees, any app |
  | Per-app code | One brittle extractor *per app* | One prompt, zero per-app code |
  | Sees | Whole feed (off-screen too) | Only the visible viewport |

  *"The a11y tree is fragile across apps. The LLM is fragile across devices. I traded
  app-fragility for device-fragility — and for my problem, that was the right trade."*

### 4. For the Android devs — making Compose a first-class citizen (no Activity)

> 🎬 **Assets:**
> - 💻 `[A-18]` SNIPPET the Service implementing the three owners + `attachOwners()` (the
    > `setViewTree*Owner` trifecta) — *what Compose needs under the hood.*
    `DeckardOverlayService.kt`.
> - 🖼️ `[A-19]` IMAGE (fun) — "An Activity is just three registries in a trench coat."
> - 💻 `[A-20]` SNIPPET `rememberViewModelStoreOwner` — an independent composable owning its own
    > ViewModel store (`ComponentViewModelScope.kt`).
> - 💻 `[A-25]` SNIPPET the **`retain` API** — `RetainedViewModel` + `rememberRetainedViewModel { }`
    > (a ViewModel that works anywhere). *Source: old keyboard build, `retain/`.*
> - 💻 `[A-26]` SNIPPET **Navigation 3** — `NavDisplay` + `entryProvider` + the `RetainDecorator`
    > that scopes retain per nav entry. *Source: old keyboard build, `KeyboardNavHost.kt`.*
> - 📊 `[A-27]` DIAGRAM before/after — "hand-rolled owners + custom factory" vs "retain +
    > `rememberViewModelStoreOwner` + Nav 3."
> - 🖼️🔥 `[A-29]` **SELLING SLIDE** — a composable subtree that owns its own ViewModel + DI, the
    > rest of the tree greyed out / untouched. Frame it as "the Fragment-shaped hole, filled" — a
    > self-contained island dropped into a busy screen. The "what's in it for me" visual.

Not just color — for an Android-dev crowd this is a genuine draw. Keep it as tight or as deep as
time allows, but crisp enough that the privacy close still peaks.

- **What Compose secretly needs from an Activity.** Render Compose via `AbstractComposeView`
  outside an Activity and it won't compose — it crashes / never recomposes until it finds three
  owners off the view tree that an Activity hands you for free: `LifecycleOwner`,
  `ViewModelStoreOwner`, `SavedStateRegistryOwner` (plus a lifecycle driven to RESUMED). *The
  Activity was quietly doing three jobs.* deckard supplies all three by hand
  (`setViewTreeLifecycleOwner` / `...ViewModelStoreOwner` / `...SavedStateRegistryOwner`).
- **The hand-rolled era (what this used to cost).** To get a DI'd ViewModel you also built your
  own `ViewModelProvider.Factory` (since `@HiltViewModel` can't reach a Service), and to let an
  *independent* composable own its ViewModel you reached for `rememberViewModelStoreOwner`.
  Workable — but plumbing.
- **The new era — Compose finally first-class anywhere (all shipped in the last few months):**
    - ⭐ **`retain` API** (`androidx.compose.runtime.retain`): `retain { }` + `RetainObserver` give
      you a value that survives recomposition, config changes, and navigation **at the
      Compose-runtime level — not tied to the Activity's ViewModelStore.** Wrap it in a
      `RetainedViewModel` base (its own `viewModelScope`, an `onCleared`) and fetch with
      `rememberRetainedViewModel { }`: a ViewModel that works in a Service, an IME, or an overlay,
      with no owner plumbing.
    - ⭐ **`rememberViewModelStoreOwner`**: a composable owns its own `ViewModelStore` where it's
      remembered — independent composables spin up their own VMs (the thing I most wanted).
    - ⭐ **Navigation 3**: Compose-first nav — the back stack is a plain `mutableStateListOf` *you*
      own, `NavDisplay` renders it, entries are decorated via `NavEntryDecorator`. No Activity
      assumptions, so it drops straight into an IME/Service/overlay. A `RetainDecorator` wires
      retain into Nav 3 — each entry gets its own retained store, cleared on pop → **per-screen
      ViewModel scoping with zero Activity involvement.**
- 🎯 **THE SELLING SLIDE — "what's in it for you?"** (the WIIFM — this is *why* you're showing
  developers any of this; without it the snippets are just trivia). Headline: **you can now
  scope a ViewModel, and resolve its dependencies, right down at the composable layer —
  depending on nothing above it.**
    - 🧩 **The relatable hook — "remember Fragments?"** In the View era, the way to drop a
      *self-contained* piece into a busy screen — with its own lifecycle, its own ViewModel scope,
      its own dependencies — was a **Fragment**. You embedded it right in the layout and it managed
      itself. **Compose took that away:** you can't drop a Fragment into a composable tree, and
      Compose had no native equivalent — every composable just inherited the host's (Activity /
      nav-destination) owners. These APIs hand that capability back, the Compose-native way. *This
      lands with anyone who shipped Android before Compose.*
    - **The everyday case — surgery on a busy screen:** edits to a huge, busy screen are scary. Now
      you drop in a stateful, self-contained composable that instantiates its own dependencies and
      scopes its own ViewModel *right there* — no threading owners/state down from the top, no
      rippling edits up the whole tree. Contained blast radius. (This is the common case — *not*
      just for library authors.)
    - **Also — library / design-system authors:** a component can carry its own state + ViewModel
      and pull its own dependencies, so the consumer just drops it in. No "you must provide a
      `ViewModelStoreOwner`" in your README.
    - 🎤 **The line:** *"Fragments used to be how you dropped a self-contained, self-scoped piece
      into a screen. Compose lost that — and just got it back. Any composable, anywhere in the
      tree, can now own its state, its ViewModel, and its dependencies, and tear them down
      correctly, without asking permission from anything above it."*
- 🎤 **Takeaway:** *"Compose used to be an Activity thing — three invisible owners you only
  noticed when they went missing. Between `retain`, `rememberViewModelStoreOwner`, and Navigation
  3, it isn't anymore. You can build a full Compose app — scoped ViewModels and navigation and
  all — inside a keyboard, a service, or a floating overlay."* (Trifecta:
  `DeckardOverlayService.kt:176-180`; retain + Nav 3 in the earlier keyboard build.)

### 5. Why local + why system-wide — the close (the vision)

> 🎬 **Assets:**
> - 🖼️ `[A-21]` IMAGE the "god-mode" permission set (accessibility + screenshot + overlay) as
    > one deliberately alarming slide.
> - 📊🔥 `[A-22]` **DIAGRAM (HERO)** the privacy boundary: screenshot → on-device LLM (stays on
    > phone) ──✋── only the *chosen text* → Pangram (cloud). A dashed "nothing else crosses" line.
> - 📊 `[A-23]` DIAGRAM Chrome extension (browser box, needs the extension API) vs this app
    > (whole-OS box, needs nobody).
> - 🖼️ `[A-24]` IMAGE (optional) a "the future" vision closer.

The philosophical payoff, and the answer to *"why is nobody else doing this?"* This is the
strongest note to end on — don't bury it in a recap. Two threads converge here: **why local**
(privacy) and **why system-wide** (independence + scale).

- **The setup question:** a cloud vision API (GPT-4V, Gemini) could read a screenshot in one
  call — no hardware floor, no four lines of XML. So why endure all of Pillar A?
- **Because of what this app *is*:** to read any screen it holds the most dangerous permission
  set on the platform — an **accessibility service** (reads every app's content), **screenshot
  capture**, and a **draw-over-everything overlay**. It can see everything you do on your
  phone.
- **Now hand that to a *remote* model:** a thing that screenshots your bank, your messages,
  your email — continuously — and ships it to a server. *No sane user grants that.* God-mode
  permissions + the cloud is radioactive, and that's exactly why nobody ships this.
- **The local LLM is what makes the permissions acceptable:** the screen-reading model runs
  entirely on-device. The screenshots and screen contents **never leave the phone.** What the
  all-seeing eye sees, stays on the device.
- **The one network hop is decoupled and optional:** the Pangram slop check sends only the
  *extracted text the user chose to check*, only when they summon a verdict. The dangerous
  capability (reading) is local; the cloud part (detecting) is small, explicit, opt-in — a
  completely separate decision the user makes.
- 🌐 **And why system-wide? (the other half of the vision):** the point isn't one more app with
  a built-in chatbot — it's *one agent that works across every app.* You can't rely on each app
  to build its own AI, and you shouldn't have to: a per-app silo only gives you the tools each
  vendor chooses to expose. Complete control means one privileged app that can read anything and
  **relies on nobody.** (Callback to Pillar B: "works on apps you've never seen" — *that's* the
  property that makes it system-wide.)
- 🌐 **The Chrome-extension contrast (for the devs):** in the browser, an extension can already
  do exactly this — Pangram even ships one. But it's browser-only, it depends on the extension
  API, and it carries its own privacy concerns. That model doesn't scale past the browser.
  Android deliberately gives you *no* easy cross-app hook — apps are sandboxed — so the
  accessibility + screenshot + overlay stack is the only way to get system-wide reach. That's
  the hard part, and it's also the moat: it works everywhere and needs no one's cooperation.
- 🎤 **Closing line:** *"This app has god-mode permissions, works across every app, and relies
  on nobody — and the only reason that's OK is that the thing wielding them never phones home.
  Privacy isn't a feature here, it's the architecture; system-wide isn't a nice-to-have, it's
  the whole point. On-device models only got good enough to do this in the last year — which is
  why this is the shape of the future, and why almost nobody's built it yet."*
- Code's on GitHub (deckard). Thanks → Q&A.

---

## Speaker cheat sheet (the "save you on stage" facts)

| Beat                  | The line that lands                                                                                                                    | Source                                                            |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| The pivot             | "Detection is one HTTPS call. The interesting problems are the two things the API can't do."                                           | `AiDetectorRepository`, `DetectSlopUseCase`                       |
| A11y betrayal         | "X stuffs the whole tweet into one `contentDescription` and makes you regex it back apart."                                            | `XContentExtractor.kt:90-135`                                     |
| The fix               | "N brittle parsers became one prompt — and it works on apps I've never tested."                                                        | `OcrPrompt.kt:26-43`                                              |
| Comprehension         | "The model isolates the post itself — it's not OCR, it's understanding the screen. That's new."                                        | `OcrPrompt.transcribe()` vs `extractMainContent()`                |
| Verbatim rule         | "The model has to copy, not summarize — or it poisons the slop detector."                                                              | `OcrPrompt.kt:21-25`                                              |
| LLM gotcha            | "The hardest bug in the project was four lines of XML."                                                                                | `AndroidManifest.xml:22-34`, `LlmEngine.kt:87`                    |
| LLM design            | "Warm up off the main thread; return null while loading — never block the summon."                                                     | `LlmEngine.kt:40-48`                                              |
| Hardware floor        | "Same CPU-fallback symptom, two causes: a manifest typo you can fix, and an old phone you can't."                                      | `LlmEngine.kt:115-119` (`backends()`), `:87-113` (`initialize()`) |
| The synthesis         | "I traded app-fragility for device-fragility."                                                                                         | (Pillar A table)                                                  |
| Why local (the close) | "God-mode permissions are only OK because the thing wielding them never phones home. Privacy isn't a feature — it's the architecture." | (Close)                                                           |
| Aside                 | "An Activity is just three registries in a trench coat. So I became one."                                                              | `DeckardOverlayService.kt:176-180`                                |

---

## Delivery notes

- **Problem-first, then hand it to the devs.** The first ~4.5 min (demo + problem + Pangram
  pivot) honor the title and earn the audience's buy-in *before* any code. The explicit pivot
  — "detection is the easy part, here are the real Android problems" — is the hinge of the
  whole talk. Land it clean.
- **Order is problem-driven, not LLM-first.** Pangram needs text → reading the screen
  (Pillar B) → the fix needs an on-device model → how the model runs (Pillar A). The
  cold-open demo already proved the on-device LLM works (airplane mode), so revealing it as
  "the fix" mid-Pillar-B raises curiosity, not doubt — and Pillar A pays it off.
  *(Alternative if it plays better: front-load Pillar A right after the pivot as "first, the
  surprising part — I'm running an LLM on this phone," then do the reading trade-off. Decide
  in rehearsal — see open questions.)*
- **The X regex slide is your single best moment.** It's visceral — the audience *feels* the
  pain of the a11y approach in one glance, making the one-prompt alternative feel like a
  release. Build to peak there (mid-talk), and let Pillar A's "four lines of XML" be the
  second peak.
- **Thread the hardware floor as device-fragility.** It's the counterweight to app-fragility;
  the synthesis table at the end of Pillar A is where the two meet. Don't introduce it as a
  late caveat — frame it as the honest cost of the trade.
- **Keep the Activity stuff an aside.** Clever but not the focus — most people will never
  build UI outside an Activity. Get the laugh, move on. Keep it tight (~1 min) so it doesn't
  deflate the privacy close that follows.
- **End on privacy, not a recap.** The "why local" argument is the philosophical payoff and
  the answer to "why nobody does this." Plant it in one line during the pivot, then let it be
  the last thing the audience hears. It also retroactively justifies all of Pillar A's pain —
  the hardware floor and the four lines of XML were the *price of keeping the reading local*.

---

## Honesty notes (so a repo-reader can't catch you out)

- **Pangram is a cloud API; the on-device LLM is the *reader*, not the judge.** Detection =
  Pangram (cloud). The local LLM's job in the screenshot path is reading / content-isolation.
  Keep that distinction crisp if asked — "on-device" refers to the *screen reading*, not the
  slop verdict (which needs network).
- The live mascot UI (`DeckardComposeView`) currently takes a `StateFlow` in its constructor;
  the `OverlayViewModelFactory` + `ExperimentViewModel` is *proven scaffolding* (the
  experiment VM's `greeting` confirms injection works) but isn't wired into the production
  mascot yet. Frame the aside as "here's the mechanism that makes it possible," not "every
  screen uses it."

---

## Visual assets — shot list

Production checklist for every asset suggested inline above. Drop produced files in
`talk/assets/` (naming: `A-09-x-regex-wall.png`). Types: 🎞️ GIF/video · 🖼️ image · 💻 code
snippet · 📊 diagram/chart. 🔥 = hero (make these great — they carry the talk).

| ID   | Type  | Section     | What to show                                                                | Source / how                                                          | Status |
|------|-------|-------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------------|--------|
| A-01 | 🎞️🔥 | 0 Cold open | Hero demo: swipe verdict → long-press on-device read → report card          | Screen recording                                                      | ☐      |
| A-02 | 📊    | 0 Cold open | Architecture in one glance (a11y → overlay → LLM+Pangram)                   | Draw                                                                  | ☐      |
| A-03 | 🖼️   | 1 Problem   | "Can you tell which is AI?" post grid (quiz)                                | Real screenshots                                                      | ☐      |
| A-04 | 📊    | 1 Pangram   | text ≥50 words → POST /task → poll → verdict                                | Draw                                                                  | ☐      |
| A-05 | 💻    | 1 Pangram   | 50-word gate                                                                | `DetectSlopUseCase`, `WordCount.kt`                                   | ☐      |
| A-06 | 💻    | 2B Act 1    | a11y read                                                                   | `AccessibilityScreenTextReader`, `captureScreenText`                  | ☐      |
| A-07 | 📊    | 2B Act 1    | Stylized a11y node tree                                                     | Draw                                                                  | ☐      |
| A-08 | 🖼️   | 2B Act 2    | LinkedIn dump: body in `contentDescription`, "… more"                       | `deckard_tree.txt`                                                    | ☐      |
| A-09 | 💻🔥  | 2B Act 2    | **The X regex wall**                                                        | `XContentExtractor.kt:90-135`                                         | ☐      |
| A-10 | 🖼️   | 2B Act 2    | Annotated raw X `contentDescription` blob                                   | Real dump + annotate                                                  | ☐      |
| A-11 | 💻🔥  | 2B Act 3    | **One prompt** (side-by-side vs A-09)                                       | `OcrPrompt.kt:26-43`                                                  | ☐      |
| A-12 | 🎞️   | 2B Act 3    | Long-press isolates a post on an unseen app                                 | Screen recording                                                      | ☐      |
| A-13 | 💻🔥  | 3A          | **Four `<uses-native-library>` lines**                                      | `AndroidManifest.xml:22-34`                                           | ☐      |
| A-14 | 💻    | 3A          | Backend fallback GPU→CPU→NPU                                                | `LlmEngine.kt:87-119`                                                 | ☐      |
| A-15 | 💻    | 3A          | `EngineConfig` (vision/audio backend, tokens, cacheDir)                     | `LlmEngine.kt:92-101`                                                 | ☐      |
| A-16 | 📊    | 3A          | GPU vs CPU inference time (~10×) bar chart                                  | Draw / measure                                                        | ☐      |
| A-17 | 🖼️   | 3A          | Hardware floor: flagship ✅ vs old phone ❌                                   | Draw / meme                                                           | ☐      |
| A-18 | 💻    | 4 Compose   | The 3 owners + `attachOwners()` — what Compose needs                        | `DeckardOverlayService.kt:69-74,176-180`                              | ☐      |
| A-19 | 🖼️   | 4 Compose   | "Activity = three registries in a trench coat"                              | Illustrate                                                            | ☐      |
| A-20 | 💻    | 4 Compose   | `rememberViewModelStoreOwner` — composable owns its VM store                | `ComponentViewModelScope.kt`                                          | ☐      |
| A-21 | 🖼️   | 5 Close     | "God-mode" permission set slide                                             | Draw                                                                  | ☐      |
| A-22 | 📊🔥  | 5 Close     | **Privacy boundary** — what crosses to the cloud, what doesn't              | Draw                                                                  | ☐      |
| A-23 | 📊    | 5 Close     | Chrome extension (browser) vs this app (whole OS)                           | Draw                                                                  | ☐      |
| A-24 | 🖼️   | 5 Close     | "The future" vision closer (optional)                                       | Draw                                                                  | ☐      |
| A-25 | 💻    | 4 Compose   | **`retain` API** — `RetainedViewModel` + `rememberRetainedViewModel`        | old build: `retain/RetainedViewModel.kt`, `retain/RetainDecorator.kt` | ☐      |
| A-26 | 💻    | 4 Compose   | **Navigation 3** — `NavDisplay` + `entryProvider` + `RetainDecorator`       | old build: `keyboard/KeyboardNavHost.kt`                              | ☐      |
| A-27 | 📊    | 4 Compose   | Before/after: hand-rolled owners+factory vs retain+Nav 3                    | Draw                                                                  | ☐      |
| A-28 | 💻    | 4 Compose   | `OverlayViewModelFactory` + `@IntoMap` (deep cut)                           | `OverlayViewModelFactory.kt`, `OverlayViewModelModule.kt`             | ☐      |
| A-29 | 🖼️🔥 | 4 Compose   | **Selling slide** — subtree owns its VM+DI ("Fragment-shaped hole, filled") | Draw                                                                  | ☐      |

**The five heroes** (if you only polish a handful): A-01 (demo), A-09 (regex wall), A-11 (one
prompt), A-13 (four XML lines), A-22 (privacy boundary). The talk lands on these.

---

## Topic reservoir — everything on the table

The full catalog of talk-worthy material, so nothing's lost before trimming. Tags:
**⭐ core** (likely keep) · **✅ strong** (good if there's room) · **🔹 deep cut / color**
(first to trim). Pull items up into the running order as the talk's length gets decided.

### A. Framing, product & story

- ⭐ **The problem:** AI slop is flooding feeds; on a phone you have zero authenticity signal —
  you can't tell a person from a paste of ChatGPT.
- ✅ **The name "Deckard"** = Blade Runner's replicant-hunter, who runs the Voight-Kampff test
  to tell human from artificial — a near-perfect metaphor for an AI detector. Strong cold-open
  hook. (Note: the in-app *persona* leans weary-scholar; the apt reference for the *name* is
  Blade Runner, not Diablo's Deckard Cain — don't cross the streams.)
- ✅ **Three ways to invoke:** swipe the edge tab (a11y read), long-press it (screenshot OCR
  read), or **share text from any app** (share sheet → judge directly, no screen read at all).
- 🔹 **Project history:** started as a custom soft-keyboard (IME) with a predictive-suggestion
  engine (dictionary + n-gram learning) and a Room DB; pivoted to the slop detector. Only the
  on-device LLM layer survived, repurposed for OCR. A tidy "follow the one reusable piece"
  story if you want an origin beat.
- 🔹 **Deckard's voice/persona:** weary-scholar deadpan — *"Slop, my son. (91.7%)"*. Color for
  the verdict UI, not load-bearing.

### B. The detector — the "easy part" (Pangram)

- ⭐ **Pangram API:** hand it ≥50 words of text, get back an AI-vs-human verdict. The whole
  "AI" half of the app is someone else's service.
- ✅ **It's async, not instant:** `POST /task`, then poll `GET /task/{id}` until a terminal
  stage. A real-world "the magic API makes you wait and poll" detail.
- ✅ **The 50-word gate** (`MIN_WORDS_TO_DETECT`): below it, return `NotEnoughText` *without*
  calling the API — cheap, and avoids a nonsense verdict on a nav bar's worth of words.
- ✅ **Clean domain boundary:** `AiDetectorRepository` (API→domain), `DetectSlopUseCase`
  (domain→UI), `SlopCheck` = `Judged` / `NotEnoughText` / `Failed`. The overlay never touches
  the repo directly. (Good "seams" slide for an architecture-minded crowd.)
- 🔹 **The verdict UI:** `SlopReportCard` — a faithful replica of Pangram's report (excerpt
  card, a `Canvas` composition gauge, label/confidence row, "View full analysis" / "Copy
  link"), with a `publicDashboardLink` deep link.

### C. Reading the screen — the trade-off (the heart)

- ⭐ **Two strategies behind one `ScreenTextReader` seam:** a11y-tree extraction vs
  screenshot+LLM. Swapping a gesture onto a different reader is one qualifier flip.
- ⭐ **The bridge pattern:** only an `AccessibilityService` can `takeScreenshot()` or read
  `rootInActiveWindow`. So the service registers on-demand handlers into `ScreenshotCapturer` /
  `ScreenTextCapturer` singletons, and the rest of the app calls through those. Read on demand
  (not cached) so the text is fresh at the exact moment of summon.
- ✅ **Framework-free snapshot:** `rootInActiveWindow` → a plain `ScreenNode` (via
  `ScreenNodeSnapshot`) → extractors become *pure functions*, unit-testable against captured
  trees with no Robolectric / mockk.
- ⭐ **Per-app extractor architecture:** one `ScreenContentExtractor` per app;
  `ScreenContentExtractors` picks the first whose `handles(pkg)` is true, else
  `GenericContentExtractor` (viewport-clipped whole-tree / WebView walk — the unknown-app
  fallback). Contributed via Hilt `@IntoSet` — add an app = one class + one binding.
- ✅ **LinkedIn extractor:** Compose feed, mostly bare `android.view.View` with no ids. Match on
  **content, not class**; read the **fuller of `text` vs `contentDescription`** (a collapsed
  post hides the body + "…more" in the desc); select the **centred** post, fall back to largest
  viewport overlap.
- ⭐ **X extractor — the horror exhibit:** the whole tweet is concatenated into one
  `contentDescription`; the body is parsed back out with anchored regexes (`TRAILING_METRICS`,
  `LEADING_BYLINE`, `QUOTE_LEAD`, `QUOTER_COMMENT`); "Promoted." cards skipped; a quote tweet
  judges the **quoter's own comment**; "frozen at good-enough"; known gaps (a "." in a display
  name defeats the byline strip; absolute timestamps survive). **This is the single best
  slide** — show the regex wall.
- 🔹 **Shared tree helpers** (`NodeText.kt`): `viewport`, `collectVisibleText`, `find`/`findAll`,
  `mostVisible` — the toolkit every extractor reuses.
- ✅ **The discovery loop / runbook:** on summon (debug), `dumpTree` writes the active-window +
  every-window tree to a file we `adb pull` — the exact `ScreenNode` the extractor saw.
  `uiautomator dump` as a zero-code cross-check. Lock each app with a `ScreenNode` fixture +
  extractor test. (Great "here's how I actually debugged this" beat.)
- ⭐ **The fix — screenshot + vision model:** long-press → `OcrContentScreenTextReader` →
  screenshot → `LlmEngine.generateWithImage(extractMainContent())` → text → Pangram. A
  screenshot is the visible **viewport only** — exactly what the user sees, no off-screen feed.
- ⭐ **Comprehension, not OCR:** the model understands the layout and isolates the post itself,
  won't fuse unrelated text blocks — a **recent** capability. The code keeps the dumb
  `transcribe()` for contrast against `extractMainContent()`.
- ⭐ **The verbatim rule:** the prompt forbids summarize/paraphrase/translate/"correct" —
  rewriting would bias the detector toward "AI." The LLM is a *reader*, not a summarizer.
- 🔹 **Dormant text-only `ContentExtractor`** (+ `ContentExtractionPrompt`): would do the same
  isolation over a noisy *a11y* capture (model selects which captured lines are content, never
  rewrites). Superseded by doing isolation at the vision step — mention only if asked.

### D. Loading the on-device LLM (the engine)

- ⭐ **LiteRT-LM `Engine` wrapper** (`LlmEngine`). Model = a multimodal Gemma `.litertlm`,
  2.4–3.5 GB.
- ✅ **Getting a model onto the device:** `adb push` into
  `Android/data/<pkg>/files/models/`; `LlmEngine` loads the first `.litertlm` it finds. It
  warms up once per process and caches, so `force-stop` to reload a new model. Local model
  files are gitignored.
- ⭐ **The GPU native-library bug (the single hardest bug):** the GPU delegate (ML Drift) is
  OpenCL; Android 12+ won't `dlopen` the vendor `libOpenCL.so` unless declared. Four
  `<uses-native-library required="false">` lines (`libOpenCL`, `libvndksupport`, `libcdsprpc`,
  `libedgetpu_litert`). Without them → opaque `INTERNAL` error → silent CPU fallback → ~10×
  slower.
- ✅ **Version pin:** `litertlm 0.11.0` — 0.12.0 regressed GPU for these Gemma builds; 0.11.0
  matched AI Edge Gallery, the reference app we diffed against. ("Pin your runtime" lesson.)
- ✅ **`EngineConfig` specifics:** `maxNumTokens` (KV-cache size), `cacheDir = null` (the GPU
  weight cache lands next to the model in external storage), `visionBackend = GPU` +
  `audioBackend = CPU` for the multimodal Gemma, `maxNumImages`.
- ⭐ **Backend fallback GPU → CPU → NPU**; NPU isn't supported by these Gemma builds.
  `initialize()` keeps the first backend that survives init.
- ⭐ **Warm-up design:** runs on `@ApplicationCoroutineScope` (callers can't cancel/restart it);
  `engineOrNull()` is **non-blocking** — returns null while loading / if no model / if every
  backend fails, so the summon degrades gracefully instead of freezing.
- ✅ **Inference API:** `generate()` / `generateWithImage()` one-shot;
  `createConversation().use {}`; all on `withContext(io)`; `runCatching` → null on failure.
- ⭐ **The hardware floor:** even with the manifest perfect, an old phone has no usable GPU
  delegate → lands on CPU → unusably slow. "Works on a recent, powerful phone," not "works on
  Android." Same CPU-fallback symptom, **two causes** — your XML typo (fixable) vs the hardware
  (not).

### E. Where the UI lives — no Activity, + Compose's new first-class APIs

- ✅ **The overlay:** a plain *started* `Service` hosts two `WindowManager` windows — the
  draggable mascot and the always-present left-edge summon tab. No Activity hosting it.
- ✅ **Window params:** `TYPE_APPLICATION_OVERLAY` + `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL
  | FLAG_LAYOUT_NO_LIMITS` + `TRANSLUCENT`, wrap-content → a floating, click-through mascot;
  touches pass through except where the emoji/bubble sit.
- 🔹 **Focusability toggle:** flip `FLAG_NOT_FOCUSABLE` only while the mascot shows, so **back**
  closes the mascot (not the app beneath); `OnBackInvokedCallback` on API 33+ with a
  `dispatchKeyEvent` legacy fallback (they don't double-fire).
- ✅ **The edge tab:** `setSystemGestureExclusionRects` so Android 10+'s back gesture doesn't
  eat the left→right swipe (budget ~200dp/edge → the tab is short). Two gestures coexist — a
  swipe moves past touch slop and cancels the long-press; a hold stays put. Haptic tick on
  long-press.
- ⭐ **Compose with no Activity:** `AbstractComposeView` won't compose until it finds three
  owners off the view tree that an Activity normally supplies — so the Service implements
  `LifecycleOwner` + `ViewModelStoreOwner` + `SavedStateRegistryOwner`, drives its own
  `LifecycleRegistry` to RESUMED, restores saved state, and calls `setViewTreeLifecycleOwner` /
  `...ViewModelStoreOwner` / `...SavedStateRegistryOwner` on each view.
- ✅ **ViewModels with no Activity:** `@HiltViewModel` / `hiltViewModel()` can't work in a
  Service (their bindings live under `ActivityRetainedComponent` → `ViewModelComponent`, which
  only exists off an Activity). The pre-`@HiltViewModel` pattern: `@Binds @IntoMap @ClassKey`
  VMs into a `Map<Class, Provider<ViewModel>>` in the singleton graph; a hand-rolled
  `OverlayViewModelFactory` resolves them; the Service exposes it via
  `HasDefaultViewModelProviderFactory` so a plain `viewModel()` call works.
  `ComponentViewModelScope`
  uses **`rememberViewModelStoreOwner`** so independent composables own their own VM store — *the
  bit you wanted to highlight.*
- ⭐ **The `retain` API — the new, first-class answer (shipped recently):** `retain { }` +
  `RetainObserver` (`androidx.compose.runtime.retain`) keep a value alive across recomposition,
  config change, and navigation **at the Compose-runtime level, decoupled from the Activity's
  ViewModelStore.** Wrap it in a `RetainedViewModel` base (own `viewModelScope` + `onCleared`),
  fetch via `rememberRetainedViewModel { ctx -> … }` → a ViewModel that works in a Service / IME /
  overlay with no owner plumbing. Resolve the dependency from Hilt inside the factory lambda
  (match the entry point's component — app/singleton for service contexts).
- ⭐ **Navigation 3 — Compose-first nav, no Activity baked in:** the back stack is a plain
  `mutableStateListOf` you own; `NavDisplay` + `entryProvider { entry<Route> { … } }` render it;
  entries are decorated via `NavEntryDecorator`. A `RetainDecorator` scopes a `RetainedValuesStore`
  per entry and clears it on pop → per-screen ViewModel scoping with zero Activity involvement.
- ⭐ **Why it matters (the selling point / WIIFM):** these APIs let *any* developer scope a
  ViewModel + its DI at the composable layer, depending on nothing up-chain. It's the
  Compose-native answer to what a **Fragment** used to give you in the View world — a
  self-contained, self-scoped unit you could drop into a busy layout — which Compose had quietly
  lost (you can't put a Fragment in a composable tree). So: do surgery on one busy app section
  without rippling changes up the tree; or ship a library / design-system component that brings
  its own state + VM and just drops in. **Encapsulation Compose didn't have — until now.** (This
  is the "why you should care," not the overlay trick.)
- 📌 **Source for retain + Nav 3:** the earlier keyboard build at
  `C:\Users\jarla\code\old\sloppy-keyboard` (`retain/RetainDecorator.kt`,
  `retain/RetainedViewModel.kt`, `keyboard/KeyboardNavHost.kt`). deckard itself uses the
  hand-rolled trifecta + factory — so the talk can show **before (deckard) → after (retain + Nav
  3)**.
- ✅ **Hilt in services generally:** `@AndroidEntryPoint` works on `Service` and
  `AccessibilityService` with plain field injection (`@Inject lateinit var`). That's the easy
  half — only *ViewModels* need the workaround above.

### F. Privacy + system-wide independence (the thesis)

**Why local (privacy):**

- ⭐ **The dangerous permission set:** accessibility service (reads every app's content),
  screenshot capture, draw-over-everything overlay (`SYSTEM_ALERT_WINDOW`). God-mode.
- ⭐ **Why not a cloud vision model:** handing god-mode + continuous screen access to a remote
  server is radioactive; no sane user grants it. That's *why nobody ships this.*
- ⭐ **The local LLM makes the permissions acceptable:** the reading runs fully on-device; the
  screenshots and screen contents never leave the phone.
- ⭐ **Decoupled egress:** the Pangram check is separate, optional, and user-initiated, and
  sends only the *chosen text* — never the screen.
- ✅ **Why now:** on-device models only just got good enough — the same "why now" as the
  comprehension point. The privacy design wasn't *possible* until recently.

**Why system-wide (independence + scale):**

- ⭐ **System-wide is the whole point:** one agent that reads *any* app or *any* text — not yet
  another app with its own built-in chatbot. The future is a system-wide assistant with access
  to everything, not N siloed per-app AIs.
- ⭐ **Rely on nobody:** you can't depend on every app to build (good) AI, and a per-app silo
  only gives you the tools each vendor chooses to expose. Complete control means one privileged
  app that accesses everything and depends on no third party — no API cooperation required.
- ✅ **Android has no easy cross-app hook (unlike a Chrome extension):** apps are
  sandboxed/isolated, so there's no cross-app API to plug into. The accessibility + screenshot +
  overlay stack is the *only* route to system-wide reach — which is exactly why this is hard,
  and why it's a moat.
- ✅ **The Chrome-extension contrast:** in the browser, an extension can already do this (Pangram
  ships one) — but it's browser-only, dependent on the extension API, with its own privacy
  concerns. It doesn't scale past the browser. This project is the system-wide,
  no-cooperation-required version — and it works even inside Android's sandbox.

### G. Engineering practice (dev-audience nice-to-haves)

- 🔹 **Testing without the framework:** pure `ScreenNode` fixtures via a `node()` helper → fast
  JVM tests, no Robolectric / mockk. Extractors are pure functions by design.
- 🔹 **`CoroutinesTestRule(eager)`:** `UnconfinedTestDispatcher` for hot flows vs
  `StandardTestDispatcher` for asserting debounce timing.
- 🔹 **Logging:** `logDebug { }` over Timber's `DebugTree` (debug only); tag = the calling
  class's simple name; the native runtime logs under `litert` / `litert-lm`.
- 🔹 **Comment style:** KDoc on classes / public functions only, no inline narration.

### H. Demo plan & live-risk

- ⭐ Real device **above the hardware floor**; have a backup recording.
- ✅ Demo beats: scroll a slop feed → voice the doubt → swipe for a verdict → long-press (the
  on-device read) → show the report card.
- 🔹 Show the discovery loop live: summon → `adb pull deckard_tree.txt` → point at the
  `contentDescription` blob that started the X regex saga.

---

## Open questions / TODO for next revision

- [ ] Working title set: "Fighting AI slop with anti-slop". Decide on a subtitle (and whether
  to keep one).
- [ ] **Ordering call:** Pillar B (reading) before Pillar A (LLM), as drafted? Or front-load
  the LLM right after the Pangram pivot? Decide in rehearsal.
- [ ] Confirm the demo device is above the hardware floor (and have a backup recorded demo).
- [ ] **Privacy framing placement:** drafted as a one-line plant in the pivot + full payoff at
  the close. Decide if it deserves more room earlier (e.g. a dedicated "the scary
  permissions" beat when the accessibility service first appears in Pillar B).
- [ ] Expand Pillar B into slide-by-slide content (exact regex snippet, side-by-side layout,
  trade-off table as a build)?
- [ ] Draft the spoken script for the cold-open demo + the Pangram pivot (the opening that
  carries the most weight)?
- [ ] **The trim pass (later):** once the talk length is fixed, walk the Topic reservoir and
  promote ⭐ first, then ✅ as room allows, and cut 🔹 unless one earns its place as color.
  Decide which single "deep cut" (project history? the discovery-loop live demo? the
  testing story?) is worth keeping as flavor.
- [ ] Anything missing from the reservoir? (Sweep the codebase again before the trim pass so
  we're cutting from a complete set, not adding late.)
- [ ] **Produce the assets** (see the "Visual assets — shot list" table). Start with the five
  heroes: A-01 demo, A-09 regex wall, A-11 one prompt, A-13 four XML lines, A-22 privacy
  boundary. Drop files in `talk/assets/`.
