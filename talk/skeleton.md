# Skeleton v2 — Fighting AI slop with anti-slop

Working skeleton, built interactively with Costa (broad strokes first, then slides). The
locked title/synopsis and scope live in `PLAN.md`. Deck ground rule: **real code, tiny
snippets** — a few lines each, heavily elided, never a 100-line wall.

> **Asset convention:** demo GIFs/videos are marked `🎬 [PLACEHOLDER: …]` at the exact slide
> point — Costa records and provides them later. Don't block on assets.

## 1. Cold open — the slop, then the fix (Costa's framing, 2026-07-07)

- Start humorous, zero setup: *the internet is full of slop and I really got sick of it.*
  Audience has seen the title; land the joke fast.
- 🖼️ `[PLACEHOLDER ×2: real screenshots of extremely sloppy posts — two quick flash
  slides, passed through fast for the laugh; second one lands "and I got sick of it."]`
- 🎬 `[PLACEHOLDER: hero demo — scrolling LinkedIn, maximum-slop post (🚀 rocket emojis,
  "I'm humbled to announce…") → summon Deckard → verdict: "This is slop, son."]`
  The app proves itself before a single word of engineering.
- **The name beat (~10 sec, millennial double reference — both intended):** Deckard is
  Blade Runner's replicant hunter, the guy whose job is testing what's human vs artificial
  — AND Deckard Cain from Diablo, the wise old man who guides the hero and **identifies
  your unidentified items**. That's literally the UX: bring him your loot (a post), he
  tells you what it's worth. It's also why the verdict talks like that ("This is slop,
  son."). (Overrides v1's "don't cross the streams" note — crossing them IS the joke.)
- **The pivot:** "so… how is this done?"

## 2. The machine at a glance

- One architecture slide: *"let's look at this thing at a glance — then we go into detail."*
  The mascot overlay (the face) → screen reading (the eyes) → the verdict (the brain).
- 📊 `[PLACEHOLDER: one-glance architecture diagram]`
- One-liner on the verdict box: the detection itself is an API call (Pangram) — and it
  won't even bother under ~50 words; Deckard refuses to judge fewer than 50 words (short
  text can't be classified reliably).
- Keep it to one beat; it's a map for the descent, not a lecture.

## 3. The naive first attempt — just read the screen, how hard can it be?

- *"I started naively: just read the `contentDescription` off the accessibility tree."*
  Storytelling beat — the honest first approach, told in build order.
- **It's harder than it seems.** Things the audience likely hasn't touched:
  - You need an `AccessibilityService` — most devs have never written one.
  - You need **dangerous permissions** (accessibility access + draw-over-apps +
    screenshots) and the user has to be walked into accepting them.
  - And then the tree itself fights you: the war stories.
    🖼️ `[PLACEHOLDER: war-story visuals — e.g. LinkedIn hiding the post in
    contentDescription / X fusing the whole tweet into one blob]`
- **The dead end — shown in code.** I really tried: interfaces and implementations for every
  app, dispatching on the foreground app's package. Two-snippet build:
  1. **The innocent setup** (looks clean, feels scalable): the `ScreenContentExtractor`
     interface — `handles(packageName)` + `extract(root)` — and the dispatcher picking the
     first extractor that claims the package
     (`ScreenContentExtractor.kt:12-19`, `ScreenContentExtractors.kt:18-21`).
     *"One interface per app, what could go wrong?"*
  2. **The reality** (pure hell): a taste of what one implementation costs — X fuses the
     whole tweet into one `contentDescription`, so the "extractor" is a wall of anchored
     regexes (`XContentExtractor.kt:99-134` — `TRAILING_METRICS`, `LEADING_BYLINE`,
     `QUOTE_LEAD`…). Show enough of the regex wall to hurt; elide the rest.
     - Garnish gags for this slide (a line each): quote tweets pack **two posts into one
       string** (we judge the quoter's comment — the text after the word "Added");
       promoted cards are skipped, so Deckard refuses to judge ads; and a display name
       containing a "." defeats the byline parser — one guy named "Dr. Smith" breaks the
       whole thing.
- Punchline: every app is a new bespoke parser, plus the browser, plus every app you've
  never seen. There is **no way to handle everything for every app.** A treadmill, not a
  roadmap.

### 3b. Aside — accessibility is about to matter *more* (placement tentative)

- The twist that falls out of the dead end: the tree failed *me*, but in an agent future the
  a11y tree is how **agents will drive your app**. Write your app accessibly and you're not
  just serving screen readers — you're exposing an API for whatever assistant your user runs.
- Costa's call pending: brief aside **here** (recommended — the irony is freshest right at
  the dead end, and it keeps the close free) vs saved for the close.

## 4. The pivot — "wait, maybe the model can just… look at it"

- The realisation, told as it happened: *these models are getting good — maybe it's good
  enough to take a screenshot and figure out what the relevant text is by itself.*
- One idea replaces the whole extractor treadmill: screenshot → model → the post.
- **The irony beat (keep):** even on the screenshot path, only an `AccessibilityService`
  can call `takeScreenshot()`. You never escape the scary service — the "dead end" tech is
  still the only door to the pixels.
- **Screenshot pipeline snippet (keep):** hardware buffer → bitmap → downscale to 1024px →
  JPEG 85. You feed the model a shrunken JPEG, not the raw screen — latency budget.
  (`DeckardAccessibilityService.kt:146-164`)

## 5. The privacy bridge — why that model must be local

- Follow the thought: an LLM that sees **everything on your screen**. Now imagine shipping
  that — every screen you look at — to a remote LLM owned by somebody else. That's a massive
  privacy violation; no user would (or should) accept it.
- So the screen-reading brain has to live **on the phone**. Which raises the question:
  *can you even run an LLM on a phone?*
- This is a **bridge**, not the close — one or two slides, then straight into the model.

## 6. Pillar 1 — the on-device model (LiteRT-LM)

How I actually went about it, in build order:

- **Opening beat — "doesn't Android just give you this?"** Sort of: Google is pushing
  Android toward an intelligent system and exposes Gemini Nano / on-device models to apps
  for free — but gated: limited devices, and **quotas** on who calls them and how much
  (battery constraints). Fine for a feature; not for an app whose whole job is hammering a
  vision model. So: bring your own model. *(Verify the current AICore/Gemini Nano
  availability + quota story shortly before the talk — this area moves fast.)*
- **You need a model.** We use a multimodal Gemma `.litertlm` (~3 GB) — but any model would
  do; the loading story is the same. Getting it on the device: here, `adb push` into the
  external files dir, load the first `.litertlm` found. Tiny snippet.
  - **The honesty aside (keep):** adb push is the caveman hack for this project 🧌 — in the
    real world you'd have the user download it at runtime via Play delivery (Play Asset
    Delivery / Play's on-device AI delivery). One line, gets the "you're not serious" laugh
    out of the way.
  - One-liner color: `LlmEngine` still lives in a package called `suggestion/llm/` — this
    app used to be a *keyboard*; the LLM is the sole survivor of the pivot.
- **You need the library — LiteRT-LM.** `Engine` + `EngineConfig`, warm-up off the main
  thread, `engineOrNull()` never blocks. Tiny snippets.
- **The intricacies of loading it — and getting it on the GPU.** The war story, spend real
  time here: *first load landed on the CPU and it was basically unusable — shit.* You NEED
  the GPU. The `<uses-native-library>` manifest lines (the hardest bug), the silent CPU
  fallback with the opaque `INTERNAL` error, backend fallback order, `visionBackend = GPU`.
- **War story — the version pin.** `litertlm 0.12.0` regressed GPU for these Gemma builds;
  pin `0.11.0`. And the debugging method: **diff your app against Google's AI Edge Gallery
  sample** until the configs match. Pin your runtime; the reference app is the real
  documentation.
- **The honest cost — the hardware floor ("same symptom, two causes").** CPU fallback
  happens when *you* screw up (missing manifest lines — fixable) and when the phone is just
  old (not fixable). An on-device LLM is a "works on a recent flagship" feature, not a
  "works on Android" feature.
  - The synthesis line (lands here): *"the a11y tree was fragile across **apps**; the model
    is fragile across **devices**. I traded app-fragility for device-fragility — and for
    this problem, that's the right trade."*
- **The payoff — it *understands* the screen, it doesn't just OCR it.**
  - **Hero slide:** the code that replaced the regex wall isn't Kotlin — it's a paragraph of
    English. Show `OcrPrompt.extractMainContent()` (`OcrPrompt.kt:26-43`) side-by-side with
    the X regexes from §3. N brittle parsers → 1 prompt, works on apps never seen.
  - The model does the "which text matters" reasoning that per-app extractors couldn't.
    Verbatim rule as the sharp nuance: a model that rewrites poisons the slop detector.
  - Cheap laughs (a bullet each): the prompt must tell the model to ignore *the floating
    mascot* — Deckard has to be instructed not to judge his own face; and
    `OcrPrompt.clean()` exists because the model insists on wrapping answers in quotation
    marks — peak LLM-era engineering.
  🎬 `[PLACEHOLDER: long-press demo — model isolates the post on an app with no extractor]`

## 7. Callback — "remember what we were building?"

- 🎬 `[PLACEHOLDER: the demo again, zoomed in — the mascot + verdict card. "Hey look — this
  works!"]` The audience has been in the engine room for ten minutes; resurface before the
  second movement.
- **The third door (one line):** you can also just **share** text from any app straight to
  Deckard — share sheet → mascot pops up and judges it, no screen read involved. (Also the
  stage demo that can't fail, if a live demo is ever on the table.)

## 8. Pillar 2 — composables that slot in anywhere

**Framing (Costa, 2026-07-07):** the "no Activity" situation gets *mentioned*, not dwelt on
— most people write Compose against an Activity and that's fine. The draw is the **new
AndroidX APIs** that make composables genuinely self-contained: *"I knew about these APIs
and they made my job so much easier."*

- **The bridge (one beat only):** "everything you just saw is a **real app** — pure
  Compose UI, ViewModels, repositories, API calls, DI, the whole boring stack — and there
  is **no Activity anywhere**. Normally we have the *luxury* of an Activity quietly handing
  us all that (turns out **an Activity is just three registries in a trench coat**, so I
  became one 🥸). But that's MY weird problem. Here's the part that's YOURS…"
- **The problem statement (its own slide, before any API):** a very busy screen.
  🖼️ `[PLACEHOLDER: a genuinely busy production screen — Costa will use a Just Eat
  Takeaway screen, he works on that app]`
  The story everyone in the room has lived: you're asked to change one **small component**
  inside that screen. In Compose that means the hundred-parameter composable — to add one
  thing you thread parameters/callbacks through everything above it and break a hundred
  other call sites. A composable is very hard to make *independent* on a busy screen.
- **Sharpen it into the actual question:** how do I get a composable that makes its own
  dependencies and handles its own ViewModel — everything it needs, *inside* the
  composable? And how do I scope that ViewModel to the composition? Scoping is classically
  the navigation library's job (nav-entry scope) — which is where **Navigation 3** slots
  into the picture too.
- **API slide 1 — `retain`** (`androidx.compose.runtime.retain`, a full slide):
  - What it is: retention scoped to **the composition itself** — survives recomposition
    and config changes, no Activity/owner plumbing.
  - The sky's-the-limit bit: roll your own DI with it — grab the application component,
    make your own factory, and inject straight into a composable via `retain`. A very
    economical API. Snippet. (Source: old keyboard build, `retain/`.)
- **API slide 2 — `rememberViewModelStoreOwner`** (a full slide):
  - A composable owns its own `ViewModelStore` — the ViewModel lives and dies with the
    composition, no nav library required. Snippet (in-repo: `ComponentViewModelScope.kt`).
- **API slide 3 — Navigation 3** (a full slide, LAST in the pillar — ⚠️ cuttable, Costa
  decides in the trim pass whether it stays):
  - A **pure Compose** navigation library: not tied to an Activity, not tied to anything —
    fits any app, and it's great for **multiplatform** too. Back stack is plain state you
    own; per-entry ViewModel scoping built in (`rememberViewModelStoreNavEntryDecorator`).
  - The wow beat: *"I ran Navigation 3 — its own back stack, per-screen ViewModels —
    completely outside an Activity."* (Proven on device; write-up in
    `../notes/nav3-viewmodels-in-a-service-overlay.md`.)
  - 💻 `[PLACEHOLDER: Nav3 snippet — NavDisplay + entryProvider in the overlay]`
  - 🎬 `[PLACEHOLDER: GIF — Nav3 panel navigating inside the overlay, no Activity]`
- **The WIIFM, landed:** drop-in encapsulation — a subtree that owns its ViewModel *and*
  its dependencies, depending on nothing above it. The thing Fragments used to give you and
  Compose quietly lost. That busy JET screen: now you change the small component *in place*.
- Tiny snippets, per the deck rule; the overlay is the proof it works *anywhere*, the busy
  screen is why they care.

## 9. Close — two takeaways, honestly told (Costa, 2026-07-07)

**Takeaway 1 — local LLMs surprised me.**
- It's *quite easy* to get a local LLM running — they're not that big (~2 GB), not that
  hard to use, and they're **capable enough**. "I was surprised by how powerful they are."
- The forward lean: they'll only get better — imagine what we can get out of them then.

**Takeaway 2 — Compose finally feels mature (the gossipy-but-fair beat).**
- The claim: *this year* Compose is really ready to replace Views and Fragments.
- The honest history (deadpan, not mean): scoping a ViewModel to a composable — and
  surviving rotation — never had a clear-cut answer. People rolled custom hacks or let a
  navigation library do it, and it was all Activity-tied underneath. Everyone quietly
  asked "how do I get my old Fragment back, the nice way, in pure Compose?" and there
  wasn't one.
- The editorial: these were **core** APIs, and they arguably should have shipped years
  ago — without them Compose felt like an incomplete framework. But: `retain` +
  `rememberViewModelStoreOwner` are the abstract, pure-Compose answer. It's here now.
- (This lands as the payoff of pillar 2 — the busy-screen problem from §8 is exactly the
  wound these APIs close.)

**Out:** repo link, thanks, Q&A.

(Note: the a11y-forward-look stays at §3b — the close is full. Privacy was spent at §5.)
