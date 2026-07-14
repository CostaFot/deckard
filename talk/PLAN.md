# PLAN.md — living document for the GDG Deckard talk

The single source of truth for where this talk stands. Read top to bottom before doing
anything; update it (dated) whenever something changes. Rules of engagement are in
`CLAUDE.md` next to this file.

## Goal

A 15–20 min talk for Android developers at GDG (Android circuit) about **Deckard**, the
on-device AI-slop detector in this repo. Roughly 15–20 slides; deliberately overshoot on
slide count first, Costa trims.

## Title & synopsis (locked, 2026-07-07)

**Title:** *Fighting AI slop with anti-slop*

> The internet is drowning in AI slop. So I built a detector that reads whatever's on the
> screen and ships it off to an API for slop-validation. Turns out a lot has changed on Android
> these past few months — enough to make the whole thing far more straightforward than I'd
> imagined.
>
> First, we'll load a Gemma model with **LiteRT-LM** and watch it *understand* a screenshot
> rather than just OCR it. Along the way, we'll see why exposing the right **accessibility**
> information might be about to matter even more than it already does.
>
> Then, with the newer **Compose** and **Lifecycle** APIs (`retain`,
> `rememberViewModelStoreOwner`), we'll work out how a composable can own its ViewModel and its
> dependencies — scoped exactly to the composition — bringing back the drop-in encapsulation we
> lost when Fragments faded.
>
> You'll leave able to:
>
> - Ship an on-device LLM with LiteRT-LM, and knock out a few of the common gotchas
> - Leverage the new Compose and Lifecycle APIs to make contained changes to very busy screens

**What this synopsis commits us to (scope, vs the dead v1):**
- Two co-equal pillars: **(1)** on-device Gemma via LiteRT-LM that *understands* screenshots
  (+ the accessibility angle, framed forward-looking: a11y info may matter *more* in an
  LLM-agent world, not just "the tree betrayed me"), **(2)** the new Compose/Lifecycle APIs
  (`retain`, `rememberViewModelStoreOwner`) restoring Fragment-style drop-in encapsulation —
  **promoted from v1's short aside to a headliner**.
- Framing: "a lot changed on Android recently — this got *easier* than expected" (optimistic),
  not v1's "everything betrayed me" (grievance).
- The two audience takeaways are the contract: ship an on-device LLM + gotchas; contained
  changes to busy screens. Every slide should serve one of them.
- The privacy close and the Pangram/detection plumbing are not in the synopsis — they can
  appear as connective tissue/color but don't headline.

## Status log

- **2026-07-15 (polish pass, slide-by-slide with Costa)** — **s1**: title forced one-line
  (`!text-4xl` + nowrap), subtitle and `@markasduplicate` handle removed, pic+name+role
  reveal together on one click. **s2**: left column text cut, image centered
  (`layout: center`); the "can't tell anymore" beat moved to the speaker note. **s11**:
  `{all|4}` highlight step focusing `rootInActiveWindow`. **s35**: snippet trimmed 16 → 8
  params (`text-xs` → `text-sm`), comment now "…plus 90 more params"; click order = code
  whole (1) → focus the comment line `{all|11}{at:2}` (2) → bullets (3–7). **s39**: the
  redundant closing line cut (snippet comments carry it); new final click "Saved state
  too? `rememberSaveableStateHolder()` — same trick, for `rememberSaveable`." **s40**:
  collapsed to a one-beat punchline (the recap bullets repeated the previous slides):
  title **"Wait a minute…"** alone, click → **"Yeah. We just rewrote Fragments in
  Compose. 🧩"**; the sincere half + busy-screen payoff live in the speaker note. **Comment colour**: `setup/shiki.ts` comments `#7A7E85` grey → `#FFC66D` yellow
  (comments are on-slide annotations; pops on dark, no token-colour collision).
- **2026-07-14 (title slide)** — Slide 1 gained the speaker intro: `assets/profile_pic.jpg`
  as a round avatar next to "Costa Fotiadis · `@markasduplicate` / Senior Android dev ·
  Just Eat Takeaway.com". Deck map updated (s1 no longer asset-free).
- **2026-07-14 (trim & polish pass, with Costa slide-by-slide)** — **49 → 43 slides.** Six
  merges, zero beats lost: **16+17** (interface + dispatcher, two clicks on "Java 1998");
  **28+29** ("everything worked! …but slow" + the `INTERNAL` logcat, one slide, two clicks);
  **38+39** (WIIFM: JET screenshots swap out for the god-VM bullets + 100-param snippet via
  `v-click.hide` overlay); **41→42** (the "what we actually want" checklist became the
  `retain` slide's lead-in — **rescoped by Costa to state-only** ("keeps its own state,
  surviving rotation"), since `retain` doesn't answer the VM/deps want; those land on the
  next slides); **43+44** ("Should someone actually do this? Not really 🧪" is now the final
  click on the DIY-ViewModel slide); **25+26** (Gemini-Nano slide folded into the model
  slide, retitled **"Modelling" → "Bring your own brain"**, the question as an on-slide
  lead-in so Costa can't forget the beat; VERIFY-note kept, on-slide wording kept
  stale-proof). **Kept, per Costa:** the 3-frame replay (s5, note now flags SKIPPABLE ON
  STAGE) and the share-sheet slide (s31). **Code line-highlight steps added** (Costa likes
  the pattern): dispatcher `{2|5-6|all}{at:3}` (the `Set` first — Costa's call — then the
  dispatch line), `RetainedViewModel` `{1|2|4-7|all}`, engine block `{all|4-5}{at:2}` (the
  two `Backend.GPU()` lines light up last, as the cliffhanger into the war story).
  **Hard-won Slidev fact:** in `{...}{at:N}`, `at` is the click of the first *change* —
  ranges[0] is active from the start — so a block inside a `<v-click>` wrapper needs
  `at = wrapper click + 1` or the first highlight plays while hidden. **Hardware-floor
  slide reworked:** the two bare boxes and the title peepoFine gone; `fail_simp.gif` (Bart
  collapsing — new asset) is the punchline; comma fix in the 3 GB line. **Wording:** roadmap
  bullet fixed ("New-ish useful Compose APIs — and how to apply them"), all ellipses
  normalized to `…` (6 spots), "code cut" → "release". **Declined by Costa:** the
  boxes' explainer one-liners (gif instead), the Houston-slide Pangram-cloud Q&A note.
  **All `<!-- Slide N -->` markers renumbered 1–43 and verified against `@slidev/parser`**
  (every marker matches its live number). Deck map below regenerated from the live deck.
- **2026-07-14 (defect-fix pass)** — Review of the full deck found 7 defects; 6 fixed, 1
  deferred. Fixed: **s45** `ComponentViewModelScope` snippet had a dangling `}` +
  bad indentation (now compiles); **s20** "accesibility" typo; **s33/s35 notes** — the
  second-movement transition ("everything you just saw is Compose") moved from s33's note
  to s35's, since s34–35 still follow s33 (slide order kept); **s8/s9 notes** — s8's ghost
  reference to the cut at-a-glance slide removed, and s9's note gained an explicit
  say-out-loud line that Deckard ships the screen text to Pangram (the deck stated it
  nowhere after that slide was cut); **s37** "three services in a trench coat" →
  "three **registries**" (a talk starring a literal `Service` shouldn't overload the word);
  **s42** verified against androidx sources (runtime-retain 1.11.0 in the Gradle cache):
  `RetainedValuesStore` + `RetainObserver` are the real names, `retain` is public/stable in
  `androidx.compose.runtime.retain` — but it's a **separate artifact**
  (`androidx.compose.runtime:runtime-retain`), so the snippet gained an import line with an
  artifact-coordinate comment + the note carries the full coordinate. **Deferred: the s48
  close still name-drops Navigation 3, which no slide covers** — Costa will look at it.
  Review's remaining big item: the trim (49 → time-fits-20-min via merges: 16+17, 28+29,
  38+39, 41→42, 43+44, and one of s4/s5; candidates to cut: s25, s34).
- **2026-07-14 (later)** — **Editing pass over the deck: 3 slides cut, 1 added, reaction
  images wired, deck now 49 slides (was 51).** The trim pass has *started*. Cut: **"At a
  glance"** (the face/eyes/brain cards) and **"Another easy weekend project"** (Interstellar
  meme) from the a11y run-up, and **"Bonus: Navigation 3"** (the ⚠️CUTTABLE slide — gone, so the
  Nav3-in-overlay GIF placeholder is retired with it). Added one: **"Should someone actually do
  this?"** (new s44, right after the retain-`ViewModel` slide) — the honest beat: *no, don't
  reinvent `ViewModel`, but it's a fun experiment that shows what the runtime is doing*.
  Retitled **old "Throwback 📼" → "Does it actually work?"** (s33). Slide 40 ("Change one
  thing") lost its trailing summary line + the extra click. Slide 6's first roadmap bullet (the
  Irish-step-dancing joke) now **strikes through on click 2** (CSS `line-through` bound to
  `$clicks`, not `v-mark` — rough-notation was flaky inline). Slide 23 (privacy) de-clicked to
  reveal at once. Slide 42 (`retain`) gained a red caveat callout: **doesn't survive process
  death**. Wired a batch of **Pepe/meme reaction images inline** (all in `assets/`): `pangram_logo`
  (s8, top-right), `peeponote` (s11), `madge` (s13 & s28, replacing angry emoji), `peepoFine`
  (s18 & s31 titles), `peepoHappy` (s44). All slide `<!-- Slide N -->` comments renumbered after
  each cut/add. Deck map below refreshed. **Still ~49 → ~20 to go on the trim.**
- **2026-07-14** — **Deck map refreshed against the live deck; slides numbered in-source.**
  The map above was stale (dated 07-08, 45 slides) while commits ran through 07-13 ("slide 43");
  the live deck is now **51 slides**, verified with `@slidev/parser`. Every slide got a
  `<!-- Slide N -->` comment right under its heading/first element — non-rendering, greppable, so
  Costa can reference a slide by number. (Gotcha found & avoided: a comment placed at the very
  *top* of a slide, right after the `---`, collides with Slidev's note detection on image+note
  slides and silently *merges* them — 51→37; placing it after the first content line is safe,
  verified across all 51.) Placeholder count is down from 6 → **2** (Nav3 GIF s47, repo QR s51);
  the hero-demo, CPU-run, demo-zoom, long-press and busy-JET boxes are all filled and wired. The
  **trim pass (51 → ~20) has not started** — the deck grew, it hasn't been cut yet; still the
  main outstanding task. Titles/section order drifted from the 07-08 map: the callback/pillar-2
  region is now s35–48 (was s32–45).
- **2026-07-08 (evening)** — **Slide-by-slide review advanced through the first act +
  pillar 1; paused at slide 32.** The review has walked the deck from the top; slides
  1–31 are reviewed, and it now resumes at **slide 32 "Remember what we were building?"**
  (the demo-zoom callback) and runs **32→45 = the Compose/Lifecycle pillar 2 + the close**.
  During review the deck grew to **45 slides** and titles drifted substantially from the
  old map, so the **deck map below is fully refreshed against the live `slides.md`** (H1s
  and asset status verified line-by-line). Most assets are now real and wired; **6
  dashed-box `PLACEHOLDER`s remain** to fill (all Costa's to record/provide): hero demo GIF
  (s4), slow-CPU-run recording (s27), demo-zoom GIF (s32), long-press demo GIF (s33), busy
  JET screen 🖼️ (s36), Nav3-in-overlay GIF (s41). Also: `assets/quote_tweet.png` sits in
  the folder **unwired** — a candidate for the X war-story slide (s14), which currently
  uses the typographic one-blob rendition. Note: the a11y-forward-look aside is now **slide
  19 "What if.."** (agents will drive apps via the tree), sitting right after the
  regex/treadmill dead-end.
- **2026-07-08** — **Slide 5 reworked into the "AI-flag gotcha" + new Pangram slide.**
  Costa retitled slide 5 "Astute observers might have noticed" — it now zooms the slop post
  on its 🤖 AI badge (🖼️ placeholder, Costa crops from `linkedin_slop_post.png`), landing
  "that flag *is* the whole app — where does it come from?" New **slide 6 "So — what's
  Pangram?"** answers: a specialist AI-text-detection API (submit → poll → verdict), rendered
  as a report card; on-tone with the cold open (no code, a submit→Pangram☁️→verdict flow
  diagram). Speaker note plants the privacy distinction (just the text, never the screen)
  without spoiling. The two-pillar roadmap idea is **dropped**. Deck: **43 slides**.
- **2026-07-08** — **Slide 5 "Why Deckard?" replaced** with a two-promise roadmap ("That's
  the product. Here's what's inside it.") — two v-click cards: (1) teaching a phone to read a
  screen (on-device Gemma/LiteRT-LM, nothing leaves the device), (2) Compose without an
  Activity (`retain` / `rememberViewModelStoreOwner`, the Fragment drop-in back). Framed as
  two promises, not an agenda. **The Blade Runner + Diablo-Cain name beat is dropped** (Costa's
  call) — reverses the 2026-07-07 "millennial double reference" decision; one-liner survives in
  the speaker note. Softened the title-slide note that said "no agenda slide" (roadmap now
  lands after the hero demo). Deck still 42 slides.
- **2026-07-07 (evening)** — Slide-by-slide refinement started. **Slide 2** ("Drowning in
  slop") reworked to a `two-cols-header` **side-by-side** layout — heading on top, the
  "can't tell anymore" beat on the left, the real-slop screenshot on the right (real asset
  now wired: `assets/linkedin_slop_post.png` — "I got fired on Monday", 🚀 spam, flagged AI);
  dropped the four overlapping 🚀 decorations that cluttered the box.
  **Old slide 3** (second slop screenshot + "And I got sick of it.") **dropped entirely**
  per Costa — the Victoria meme carries the sentiment. **Deck: 42 slides.** Asset list loses
  the second real-slop screenshot (now one).
- **2026-07-07 (evening)** — First real asset wired in: `assets/victoria_meme.jpg` (Victoria
  Beckham "be honest" meme, punchline "OK I slopped it on Claude") replaces the slide-4
  placeholder.
- **2026-07-07 (evening)** — **Costa's 16 slide-note TODOs all addressed.** Added 4 meme
  placeholder slides (Victoria Beckham after slop #2, "glad you asked" replacing the plain
  pivot divider, Interstellar before Attempt #1, "challenge accepted" before the
  architecture slide) + sweating-guy meme on the treadmill slide + 🚀 rockets interspersed
  on slop slide. Removed: the loot/"slop, son" line from Why-Deckard, the "brain is easy /
  eyes are the story" explanation from the glance slide (moved to speaker notes), and the
  ENTIRE "More fine print" slide (version pin / AI Edge Gallery / fallback ladder — Costa:
  not useful). Reworked: LinkedIn war story to side-by-side image+snippet; X war story to a
  placeholder image (annotated normal/quote/reply captures) instead of the typographic
  blob; four-XML-lines explanation de-jargoned (no OpenCL/dlopen on the slide); first-run
  slide to log ∥ 🎬 recording side-by-side; adb snippet now on a v-click; "real app" stack
  as animated bullets; trench-coat slide shows the 3 registry chips + "hand Compose those
  three and it runs anywhere"; retain-VM slide retitled "You don't even need a ViewModel
  anymore" with 3 snippets on separate clicks. **Deck: 43 slides**, map updated below.
  New assets added to the list: Victoria Beckham meme, glad-you-asked meme, Interstellar
  meme, challenge-accepted meme, sweating-guy meme, slow-CPU-run recording 🎬, X
  normal/quote/reply annotated captures, LinkedIn "…more" screenshot.
- **2026-07-07** — **Close built — FULL DECK COMPLETE, 41 slides** (skeleton §9): takeaway
  #1 "local LLMs are ready. I was surprised too." (easy/small/capable → "the worst they'll
  ever be 🔮"), takeaway #2 "Compose finally feels… complete" (the gossip: VM-scoping never
  had an answer, core APIs years late 🙊 → "this is the year Compose is really ready"),
  sign-off slide ("That's the talk" + agents-are-coming echo + QR placeholder + repo +
  @markasduplicate + "Hope you found this somewhat useful."). Deck now needs: Costa's
  assets for all placeholders, then the trim pass (40 → ~20).
- **2026-07-07** — Two Costa-requested splits: bridge slide → "real app / no Activity 😳"
  reveal + "How did I even go about this?" (luxury line, trench coat); retain slide →
  plain-API slide (`retain { }` vs `remember`, benefits, `RetainObserver` teaser) + "…so I
  built a ViewModel out of it 🧪" (RetainedViewModel + DIY DI). Deck at 37 slides.
- **2026-07-07** — Callback + pillar 2 built (slides 29–35, skeleton §7–§8): demo-zoom
  callback (🎬 placeholder + share-sheet third-door line) → "everything you just saw is
  Compose / no Activity / trench coat 🥸 / here's the part that's yours" → busy-JET-screen
  problem slide (🖼️ placeholder) → "what we actually want" checklist → `retain` slide
  (REAL snippets from the old keyboard build: `RetainedViewModel`/`rememberRetainedViewModel`
  + EntryPoint DIY-DI) → `rememberViewModelStoreOwner` slide (repo's
  `ComponentViewModelScope`) → Nav 3 bonus slide (real snippet from notes doc, ⚠️ CUTTABLE
  marked in speaker note, 🎬 GIF placeholder) → "Fragment-shaped hole: filled 🧩" WIIFM
  lander. Deck at 35 slides. Remaining: §9 close (2–3 slides).
- **2026-07-07** — LiteRT-LM act built (slides 20–28, skeleton §6): "doesn't Android just
  give you this?" (Gemini Nano gated — ⚠️ speaker note: verify current state before the
  talk) → "bring your own brain" (adb 🧌 vs Play delivery + keyboard-era color) → engine
  snippet (`EngineConfig`, warm-up, `engineOrNull()`) → "first run" CPU-fallback war story
  (fake-logcat INTERNAL error) → four-XML-lines hero slide ("Four lines. Two weeks. 🫠")
  → fine-print slide (0.11.0 pin, AI Edge Gallery diffing, fallback ladder) → hardware
  floor two-causes cards + fragility-trade synthesis line → prompt-vs-regex-wall hero
  (greyed regexes | the English paragraph) → "understands, not OCRs" (verbatim rule,
  ignore-the-mascot + clean() gags, 🎬 long-press demo placeholder). Deck at 28 slides.
  Remaining: §7 callback, §8 pillar 2, §9 close.
- **2026-07-07** — Pivot + privacy built (slides 16–19, skeleton §4–§5): "maybe the model
  can just look at it" (screenshot → model → the post), "step one: get the pixels" (irony
  beat — takeScreenshot is a11y-service-only — + downscale/JPEG pipeline snippet), "One
  problem." privacy slide (sees everything → remote LLM → "No. 🙅", callback to the scary
  dialog), "the brain has to live on the phone → can you even run an LLM on a phone?"
  divider into the LiteRT-LM act. Next: skeleton §6.
- **2026-07-07** — A11y act built (slides 8–15, skeleton §3+§3b): naive-read snippet →
  scary-permissions slide (🖼️ placeholder: the "full control of your device" dialog) →
  LinkedIn war story (`bestText()` snippet + 🖼️ tree-dump placeholder) → X blob slide
  (typographic colored-highlight rendition of the one-string card — can swap for a real
  dump screenshot) → innocent interface/dispatcher snippets → regex-wall slide + the three
  gags on v-clicks → "one app / treadmill" dead-end slide → a11y-forward-look aside
  ("accessibility is becoming your app's API"). Speaker notes carry the privacy-bridge
  plant ("remember how scary these permissions are").
- **2026-07-07** — Architecture slide built (slide 7, skeleton §2): face→eyes→brain cards
  on v-clicks (built as styled divs, swappable for a drawn diagram later), Pangram +
  50-word one-liner, lands on "the eyes are where the story is" as the descent hook. Next:
  §3 naive a11y attempt.
- **2026-07-07** — **Slides started.** Cold open built in `slides.md` (skeleton §1 → 6
  slides): title, "internet is drowning in slop" + real-slop screenshot placeholder #1
  (flash slide), real-slop placeholder #2 → "I got sick of it" (Costa provides two real
  sloppy-post screenshots), hero-demo slide (dashed-box 🎬 placeholder), "Why Deckard?"
  two-panel name beat (image placeholders), "So… how is this done?" pivot divider. Speaker
  notes on every slide. Renders clean on the dev server. Next slides: §2 architecture
  glance.
- **2026-07-07** — Review picks placed (Costa chose 1, 2, 6, 7, 10): trench-coat line →
  §8 bridge; app-vs-device-fragility synthesis → §6 hardware floor; share-sheet third door
  → §7 callback; X garnish gags (quote tweets, Promoted skip, fatal ".") → §3 regex-wall
  slide; 50-word gate → §2 architecture glance. Declined: airplane-mode flourish,
  exclusion-rects story, error-copy-on-slides, 5¢/isMocked confession.
- **2026-07-07** — Name beat added to cold open: Deckard = Blade Runner (tests
  human-vs-artificial) + Deckard Cain from Diablo (wise elder, identifies your items) —
  deliberate millennial double reference, overriding v1's don't-cross-the-streams note.
  Full review vs v1 done; strong adds 1–5 and garnish 6–10 offered, Costa picking.
- **2026-07-07** — LiteRT-LM section gains two real-world beats (skeleton §6): opener
  "doesn't Android just give you this?" (Gemini Nano/on-device models exist but are
  device-gated + quota'd — so BYO model; verify current state before the talk) and the
  adb-push honesty aside (real apps use Play delivery for runtime model download).
- **2026-07-07** — **Close locked; skeleton v2 complete end to end.** Two takeaways:
  (1) local LLMs surprised me — ~2 GB, easy, capable enough, and they'll only get better;
  (2) Compose finally feels mature — the gossipy-but-fair editorial that VM-scoping in a
  composable never had a clear-cut answer (hacks, nav libraries, Activity-tied) and these
  core APIs arguably shipped years late, but `retain` + `rememberViewModelStoreOwner` are
  the real answer now. a11y forward-look stays at §3b. Next: turn skeleton into Slidev
  slides.
- **2026-07-07** — Nav 3 gets its own slide, last in pillar 2, ⚠️ explicitly cuttable
  (Costa decides in trim): pure-Compose nav, Activity-free, multiplatform-friendly,
  per-entry VM scoping; wow beat = ran it completely outside an Activity. Snippet + GIF
  placeholders. NOTE for asset production: the Nav3 overlay POC was deleted — rebuild from
  `notes/nav3-viewmodels-in-a-service-overlay.md` (or `git show 933f0a`-era `poc/` files)
  before recording the GIF.
- **2026-07-07** — Pillar 2 content detailed: problem-statement slide first (busy JET
  screen 🖼️ placeholder — hundred-parameter composables, one small change breaks
  everything), sharpened into "how does a composable own its deps + VM, scoped to the
  composition?" (nav-library scoping → where Nav3 slots in), then one full slide each for
  `retain` (composition-scoped, DIY DI via app component + own factory) and
  `rememberViewModelStoreOwner` (composable owns its VM store). WIIFM: change the small
  component in place.
- **2026-07-07** — Pillar 2 framed and drafted (skeleton §7–8): re-show the demo zoomed
  ("this works!") as the callback, then the Compose pillar with the no-Activity situation
  *mentioned only* — the draw is the new AndroidX APIs (`rememberViewModelStoreOwner`,
  `retain`) making composables self-contained/slot-in-anywhere; WIIFM = contained changes
  to busy screens (the lost Fragment encapsulation). Only the close remains TBD.
- **2026-07-07** — Screenshot/LLM section completed after a material audit: added the
  irony beat (a11y service is still the only screenshot door), screenshot pipeline snippet
  (downscale→JPEG), prompt-as-code hero slide (`extractMainContent()` vs the regex wall),
  version-pin war story (0.12.0 regression, diffed AI Edge Gallery), hardware floor ("same
  symptom, two causes"), and cheap laughs (ignore-the-mascot, `clean()` strips quotes,
  keyboard-era `suggestion/llm/` package). All in `skeleton.md` §4–6, cuttable in trim pass.
- **2026-07-07** — Dead-end beat gets a two-snippet code build: the innocent
  `ScreenContentExtractor` interface + package dispatcher, then the X regex wall as the
  "pure hell" reveal (exact `file:line` refs in `skeleton.md` §3).
- **2026-07-07** — Middle fully shaped: a11y attempt ends in an explicit **dead end** (per-app
  extractors/browser handling can't scale) → a11y forward-look aside (agents will drive apps
  via the tree — placement tentative, recommended at the dead end) → the pivot idea ("maybe
  the model can just look at a screenshot") → privacy bridge → LiteRT-LM pillar, ending on
  the "understands, not OCRs" payoff + verbatim rule. Still open: Compose/retain weave point,
  close.
- **2026-07-07** — Body arc agreed: naive a11y attempt first ("just read the
  contentDescription — harder than it seems": AccessibilityService, dangerous permissions,
  tree war stories) → **privacy as the bridge** (can't ship screen access to a remote LLM →
  must be local) → LiteRT-LM pillar. Privacy is spent mid-talk, NOT the close. Compose/retain
  weaves in later, exact point parked. Close still TBD.
- **2026-07-07** — Skeleton v2 sections 2–3 agreed: architecture-at-a-glance beat after the
  cold open, then Pillar 1 = LiteRT-LM (model → library → GPU war story: "loaded on CPU,
  basically unusable"). Demo assets = `🎬 [PLACEHOLDER]` markers, Costa records later.
- **2026-07-07** — Skeleton v2 started in `skeleton.md` (new file — the working structure
  doc; PLAN.md stays the state/decisions doc). Cold open agreed: humorous "internet is full
  of slop" → hero GIF of Deckard judging a real LinkedIn slop post ("This is slop, son.") →
  pivot to "how is this done". Deck-wide rule: real code but tiny elided snippets, never
  walls. Body/close still TBD — being worked out in live Q&A with Costa.
- **2026-07-07** — Title + synopsis locked (above). Scope reframed vs v1: Compose/Lifecycle
  APIs promoted to co-headliner; accessibility angle turned forward-looking; optimistic
  framing. Title slide updated in `slides.md`.
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
| 2026-07-07 | Title: *Fighting AI slop with anti-slop*. Synopsis locked verbatim (see top of this file). |
| 2026-07-07 | Two co-equal pillars: LiteRT-LM/Gemma screenshot understanding (+ a11y forward-look), and `retain`/`rememberViewModelStoreOwner` encapsulation. Privacy/Pangram demoted to connective tissue. |

## Open questions (for Costa)

- [x] Title — locked: *"Fighting AI slop with anti-slop"* (2026-07-07).
- [x] Structure — skeleton v2 complete end to end (2026-07-07, see `skeleton.md`).
- [ ] Demos: placeholders agreed; all assets recorded/provided by Costa later. Before the
      hero GIF: flip `isMocked` + real API key. Before the Nav3 GIF: resurrect the POC.
- [x] Nav 3 slide (§8, last of pillar 2): **cut** in the 07-14 trim pass.
- [ ] a11y forward-look placement: currently §3b at the dead end (recommended); revisit in
      rehearsal.
- [ ] Exact GDG event/date and any CFP abstract deadline?

## Next steps

1. ~~Structure the new skeleton with Costa~~ — done 2026-07-07, complete in `skeleton.md`.
2. ~~Turn the skeleton into slides~~ — done 2026-07-07: full 40-slide deck in `slides.md`,
   all sections, real snippets, speaker notes throughout.
3. **Trim pass: effectively done — 43 slides (was 49; peaked at 51).** All six planned
   merges landed (see the 07-14 trim-pass status entry); the remaining cut candidates were
   reviewed with Costa and **kept deliberately**: s5 (3-frame replay — note says SKIPPABLE
   ON STAGE, decide live) and s31 (share sheet — the can't-fail demo path). ~10 of the 43
   are 5-second beats (memes/dividers), so the deck should now rehearse at roughly 18–20
   min. **Next gate is a timed rehearsal**, not more cutting.
4. **Loose ends, in order:** (a) the deferred defect — **s42 Takeaway #2 still name-drops
   Navigation 3**, which no slide covers (Costa said he'll look at it); (b) the repo **QR
   code placeholder, s43**; (c) decide whether `quote_tweet.png` replaces the typographic
   X blob on **s14**. **Prerequisites for a live demo:** flip `isMocked` + real
   `AI_DETECTOR_API_KEY`; optionally the persona line on the report card for "This is slop,
   son" on screen.
5. Rehearsal pass: count minutes against the trimmed deck; export
   (`npm run export`) as the backup copy.

## Deck map (slides.md as of 2026-07-14 trim pass, 43 slides)

The slides are numbered **in the source**: each carries a `<!-- Slide N -->` comment right
under its heading/first element (non-rendering; greppable), **renumbered 1–43 and verified
against `@slidev/parser`** — source markers now match live numbers exactly. Asset column
lists what's wired; — = text/emoji only.

| # | Title / content | Assets |
|---|---|---|
| 1 | Title — *Fighting AI slop with anti-slop* + avatar/name/role | `profile_pic.jpg` |
| 2 | The internet is drowning in slop 🚀 | `linkedin_slop_post.png` |
| 3 | Victoria Beckham meme | `victoria_meme.jpg` |
| 4 | So I built a thing that tells me — hero demo | `demo_fast.gif` |
| 5 | Once more, slowly — 3 frozen frames (note: SKIPPABLE ON STAGE) | `start_post` · `summon_in_progress` · `verdict` |
| 6 | What this talk is about — roadmap (joke bullet strikes through) | `start_post.png` |
| 7 | Astute observers might have noticed — zoom on the 🤖 AI flag | `linkedin_slop_post_zoomed_in.png` |
| 8 | AI is very good at detecting other AI — Pangram + curl snippets | `pangram_logo.png` 🐸 |
| 9 | What do we want? — that extension, system-wide on a phone | — |
| 10 | "glad you asked" meme — the pivot | `glad_you_asked_meme.png` |
| 11 | Attempt #1: just read the screen! | `peeponote.png` 🐸 |
| 12 | Not so fast — scary permissions | `scary_permission.png` · `draw_over_other_apps.png` |
| 13 | WTF #1: LinkedIn trips up the reader | `linked_in_more_collapsed.png` · `madge.png` 🐸 |
| 14 | WTF #2: Twitter/X — one-blob desc (typographic) | (⚠️ `quote_tweet.png` in `assets/`, unwired) |
| 15 | "challenge accepted" meme | `challenge_accepted.png` |
| 16 | Let's write some Java 1998 — interface + dispatcher (merged; Set-first highlight steps) | — |
| 17 | The reality: Regex wars | `peepoFine.gif` 🐸 (title) |
| 18 | That was *one* app, not even done well — treadmill | `sweating.gif` |
| 19 | What if… — a11y-as-API for agents (forward-look aside) | agent logos ×4 |
| 20 | Maybe an AI model can just… *look* at it | — |
| 21 | Step one: get the pixels — irony + pipeline | — |
| 22 | Houston, we have a problem. — privacy | `side_eye_meme.png` |
| 23 | Running an LLM locally — divider | — |
| 24 | Bring your own brain — Gemini-Nano lead-in + HF + adb (merged) | `download_gemma_4_huggingface.png` |
| 25 | Using LiteRT-LM — the engine (GPU lines highlight last → war-story cliffhanger) | — |
| 26 | First run: everything worked! — …slow + INTERNAL logcat (merged) | `madge.png` 🐸 |
| 27 | Four lines of XML | — |
| 28 | Old phones will still choke — hardware floor | `fail_simp.gif` |
| 29 | Asking it something — prompt vs regex wall | — |
| 30 | Does it actually work? — demo replay + human-vs-robot | `demo_fast.gif` · `human_written` · `robot_written` |
| 31 | Or… just share it — share-sheet path (kept per Costa) | `select_text.jpg` · `share_improved.jpg` |
| 32 | Not just OCR — the good/the bad | — |
| 33 | Everything you just saw is a real app — no Activity | — |
| 34 | How? — 3 registries in a trench coat | — |
| 35 | What's in it for me? — JET screens ⇄ god-VM reality (merged, overlay swap) | `serp_default_screen.jpg` · `menu_default_screen.jpg` |
| 36 | Change one thing | `menu_default_screen.jpg` |
| 37 | API #1: `retain` — state-only lead-in + snippet + ⚠️ no process death (merged) | — |
| 38 | Do we even need `ViewModel` anymore? — line-by-line + "should you? not really 🧪" (merged) | `peepoHappy.png` 🐸 |
| 39 | API #2: `rememberViewModelStoreOwner` | — |
| 40 | Wait a minute… → "Yeah. We just rewrote Fragments in Compose. 🧩" (one-beat punchline) | — |
| 41 | Closing #1 — local LLMs are actually quite good now | — |
| 42 | Closing #2 — Compose finally feels… complete (⚠️ still name-drops Nav3, deferred) | — |
| 43 | That's the talk — sign-off | 🖼️ repo QR code placeholder |

**Only 1 placeholder left** (Costa's): the repo QR code (**s43**). **Still unwired:**
`quote_tweet.png` (candidate for s14). Every other referenced image/GIF exists and is wired
(verified on disk, including the new `fail_simp.gif`).

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
