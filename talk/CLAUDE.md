# talk/ — GDG Android talk about Deckard

Everything for Costa's GDG (Android circuit) conference talk lives in this folder.

**Start every session by reading `PLAN.md`** — it is the living document: current status,
decisions made, and what's next. Update it whenever the state changes (a decision lands, slides
get added, an asset gets produced). Date the entries.

## Ground rules

- **Audience:** Android developers. **Length:** 15–20 min. **Slides:** aim ~15–20, err on
  *more* slides than needed — Costa cuts down himself.
- **Tooling:** [Slidev](https://sli.dev). The deck is `slides.md`; `npm run dev` serves it at
  `http://localhost:3030` with hot reload; `npm run export` for PDF, `npm run build` for a
  static site.
- **Voice/tone:** adapt from `blog_writing_style.md` (Costa's blog style guide). It's written
  for blog posts — for slides that means: self-deprecating but credible, anti-ceremony, dry
  deadpan humour, short punchy fragments, everything technical in backticks, honest "was it
  worth it?" beats. No corporate/marketing tone, no LinkedIn energy, don't pile on
  catchphrases.
- **The v1 skeleton is dead.** `reference/old-talk-skeleton.md` was judged not good enough and
  must not be restored or followed as a structure. It IS a good quarry: war stories, exact
  `file:line` code references, and the "topic reservoir" catalog are accurate.
- **Assets** (screenshots, GIFs, diagrams) go in `assets/`, named `<slide-slug>-<what>.<ext>`.
- Work iteratively with Costa slide by slide; don't generate the whole deck in one shot.

## The project being presented (one paragraph)

Deckard (this repo) is an Android AI-slop detector: a floating 🧙 mascot in a system overlay
over every app. Summoning it reads the current screen — swipe = accessibility-tree read with
per-app extractors, long-press = screenshot → on-device multimodal Gemma (LiteRT-LM) isolates
the main post verbatim, or share text via the share sheet — and sends the text to the Pangram
API for an AI-generated verdict, shown as a report card. Root `CLAUDE.md` has the full
architecture; `PLAN.md` has the raw-material index with the demo gotchas (mocked Pangram flag,
hardware floor, model push).
