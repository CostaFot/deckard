# Skeleton v2 — Fighting AI slop with anti-slop

Working skeleton, built interactively with Costa (broad strokes first, then slides). The
locked title/synopsis and scope live in `PLAN.md`. Deck ground rule: **real code, tiny
snippets** — a few lines each, heavily elided, never a 100-line wall.

> **Asset convention:** demo GIFs/videos are marked `🎬 [PLACEHOLDER: …]` at the exact slide
> point — Costa records and provides them later. Don't block on assets.

## 1. Cold open — the slop, then the fix (Costa's framing, 2026-07-07)

- Start humorous, zero setup: *the internet is full of slop and I really got sick of it.*
  Audience has seen the title; land the joke fast.
- 🎬 `[PLACEHOLDER: hero demo — scrolling LinkedIn, maximum-slop post (🚀 rocket emojis,
  "I'm humbled to announce…") → summon Deckard → verdict: "This is slop, son."]`
  The app proves itself before a single word of engineering.
- **The pivot:** "so… how is this done?"

## 2. The machine at a glance

- One architecture slide: *"let's look at this thing at a glance — then we go into detail."*
  The mascot overlay (the face) → screen reading (the eyes) → the verdict (the brain).
- 📊 `[PLACEHOLDER: one-glance architecture diagram]`
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

- **You need a model.** We use a multimodal Gemma `.litertlm` (~3 GB) — but any model would
  do; the loading story is the same. Getting it on the device (adb push, external files dir,
  load the first `.litertlm` found). Tiny snippet.
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

## 7. Pillar 2 — the Compose/`retain` weave — placement TBD

(Costa: LLM story first, weave Compose/`rememberViewModelStoreOwner`/`retain` in "if
appropriate" — exact entry point still parked.)

## 8. Close — TBD

(Privacy is spent mid-talk (§5). Candidates: the two synopsis takeaways; or the a11y
forward-look (§3b) if it moves here. TBD with Costa.)
