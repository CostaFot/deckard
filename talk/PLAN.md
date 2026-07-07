# PLAN.md — living document for the GDG Deckard talk

The single source of truth for where this talk stands. Read top to bottom before doing
anything; update it (dated) whenever something changes. Rules of engagement are in
`CLAUDE.md` next to this file.

## Goal

A 15–20 min talk for Android developers at GDG (Android circuit) about **Deckard**, the
on-device AI-slop detector in this repo. Roughly 15–20 slides; deliberately overshoot on
slide count first, Costa trims.

## Status log

- **2026-07-07** — Slidev scaffolded in `talk/` (seriph theme, 2 placeholder slides in
  `slides.md`), dev server verified on `:3030`. Folder structure + this living doc created.
  Costa's blog style guide added (`blog_writing_style.md`). v1 skeleton recovered from git
  into `reference/old-talk-skeleton.md` and marked superseded. **No talk content written yet
  — next session starts on the actual deck.**

## Decisions

| Date | Decision |
|---|---|
| 2026-07-07 | New skeleton from scratch — v1 (`reference/old-talk-skeleton.md`) rejected as not good enough. Mine it, don't follow it. |
| 2026-07-07 | Slidev for the deck, everything lives in `talk/`. |
| 2026-07-07 | Audience: Android devs. 15–20 min. ~15–20 slides, err on more. |
| 2026-07-07 | Tone: Costa's blog voice (`blog_writing_style.md`), adapted for spoken/slide format. |

## Open questions (for Costa)

- [ ] Title — v1's working title was *"Fighting AI slop with anti-slop"*; keep, rework, or new?
- [ ] Live demo on device vs recorded GIF fallback (or both)?
- [ ] Which sections of the story get the minutes — the screen-reading war stories, the
      on-device LLM, the Compose-without-an-Activity material, the privacy close?
- [ ] Exact GDG event/date and any CFP abstract deadline?

## Next steps

1. Structure the new skeleton **with Costa, interactively** — pitch a running order, iterate.
2. Turn the agreed structure into section-header slides in `slides.md` (placeholders first).
3. Fill slides one by one; produce assets as each slide needs them (into `assets/`).
4. Rehearsal pass: count minutes against slides, cut.

## Raw-material index (verified against the codebase, 2026-07-07)

The stories and where the evidence lives. Full architecture: root `CLAUDE.md`.

**The product in one line:** floating 🧙 overlay mascot → reads the screen → Pangram API →
"is this AI slop?" report card. Name = Blade Runner's replicant hunter.

**The three summon paths:**
- Swipe edge tab → a11y-tree read: `slop/AccessibilityScreenTextReader` →
  `accessibility/ScreenTextCapturer` → per-app extractors in `accessibility/extract/`.
- Long-press edge tab → screenshot + on-device LLM isolates the main post *verbatim*:
  `slop/OcrScreenTextReader.kt` (`OcrContentScreenTextReader`), prompt in
  `suggestion/llm/OcrPrompt.kt` (`extractMainContent()` — verbatim rule is load-bearing,
  rewriting would bias Pangram toward "AI").
- Share sheet from any app → `ui/activity/ShareTextActivity` → judges text directly, no
  screen read.

**War story 1 — the a11y tree betrayal:** LinkedIn hides full post text in
`contentDescription` ("…more" truncation) — `LinkedInContentExtractor.kt`. X concatenates the
entire tweet (byline, body, metrics, timestamps) into ONE `contentDescription` blob → the
regex wall in `XContentExtractor.kt:94-134` (`TRAILING_METRICS`, `LEADING_BYLINE`,
`QUOTE_LEAD`, `QUOTER_COMMENT`). Frozen at "good-enough" — per-app parsing is a treadmill.

**War story 2 — the four lines of XML:** GPU delegate is OpenCL; Android 12+ blocks `dlopen`
of vendor libs unless declared. `AndroidManifest.xml:23-34` (`<uses-native-library>` ×4).
Without them: opaque `INTERNAL` error, silent CPU fallback, ~10× slower. Hardest bug in the
project. Related: pin `litertlm = 0.11.0` (0.12.0 regressed GPU); backend fallback
GPU→CPU→NPU in `LlmEngine.kt:87-119`; warm-up on app scope, `engineOrNull()` never blocks.

**The trade:** a11y tree is fragile across *apps* (each app's dev), screenshot+LLM is fragile
across *devices* (hardware floor: ~2-year-old phones land on CPU, unusable). "I traded
app-fragility for device-fragility."

**Compose without an Activity:** `DeckardOverlayService` implements
`LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner` itself + view-tree owners
(`DeckardOverlayService.kt:176-180`). ViewModel DI without `@HiltViewModel`:
`OverlayViewModelFactory` + `@IntoMap` bindings. Nav3-in-a-Service proven and documented in
`../notes/nav3-viewmodels-in-a-service-overlay.md` (POC deleted, doc is the reference).
Newer first-class APIs (`retain`, `rememberViewModelStoreOwner`, Nav 3): snippets live in the
old keyboard build at `C:\Users\jarla\code\old\sloppy-keyboard`.

**The privacy thesis:** god-mode permission set (accessibility + screenshot + overlay) is only
acceptable because the screen-reading model never leaves the device; the sole network hop is
the user-chosen text to Pangram. That's also why nobody ships this.

**Demo gotchas (bite you on stage):**
- `AiDetectorRepository.detect()` defaults to `isMocked = true` (canned "AI Generated"
  verdict, saves ~5¢/call). Flip for a real demo + needs `AI_DETECTOR_API_KEY` in BuildConfig.
- Needs a `.litertlm` model (~2.4–3.5 GB) pushed via adb (exact commands in root `CLAUDE.md`),
  and a *recent flagship* phone — the hardware floor is real.
- Pangram is async (POST + poll) — a real verdict takes seconds; rehearse the wait.
- 50-word minimum (`MIN_WORDS_TO_DETECT`) or it answers `NotEnoughText`.

**Deeper catalogs:** the "topic reservoir" + 29-item asset shot list in
`reference/old-talk-skeleton.md` (structure superseded, material accurate).
