
/
Blog drafts
Blog drafts







Recents
Android ViewModel basics for beginners
7 days ago
Instructions
When I ask for a blog post, write it in the voice from the attached style guide.

Memory
Only you
Purpose & context Costa runs a technical blog with an established voice and format system. The blog includes a recurring series called "Android Shorts 🩳" aimed at concise, focused posts (around two minutes). Costa has a defined persona: casual, self-deprecating tone, a beer counter as a recurring code example motif, and a characteristic sign-off pattern. A skill file at /mnt/skills/user/costa-blog-voice/SKILL.md captures this voice for Claude to reference before writing. Current state Costa recently ran a test of a blog content workflow, asking Claude to produce a short ViewModel post to evaluate fit before committing to a fuller process. The trial output covered ViewModel basics (configuration change survival, StateFlow + viewModel() pattern, and a process death caveat) and was saved to /mnt/user-data/outputs/android-shorts-viewmodel-basics.md. No corrections were requested, suggesting the output was satisfactory. On the horizon Potential next steps flagged during the test run include: expanding a ViewModel post to a full deep-dive format, tuning catchphrase density, or adding a meme at a suggested natural placement. These represent open options rather than committed plans. Key learnings & principles The "Android Shorts 🩳" format intentionally drops certain sections (e.g., Housekeeping, TL;DR) to preserve brevity — format selection should be deliberate based on target length. Costa's blog voice is formalized in a skill file and should always be consulted before writing. The beer counter motif is a consistent, recognizable element across code examples and should be preserved as a signature pattern. Tools & resources Blog voice skill file: /mnt/skills/user/costa-blog-voice/SKILL.md Output directory: /mnt/user-data/outputs/

Last updated 7 days ago

Context
1% of project capacity used

costa-fotiadis-writing-style.md
109 lines

md


Scheduled
Set up recurring tasks for this project.

costa-fotiadis-writing-style.md


# Costa Fotiadis — Blog Writing Style Guide (for Claude)

**How to use this file:** Drop it into a Claude Project as project knowledge. When asking for a new post, just give the topic (plus any code, repo link, or rough notes) and say "write it in my blog voice." This guide is the blueprint; pair it with a custom Style built from a couple of real posts for the voice layer.

This guide was derived from analyzing the blog posts on https://www.costafotiadis.com — both the original style notes and a close read of the actual published posts.
 
---

## Who is writing
A senior Android engineer at Just Eat Takeaway (JET) who blogs about Kotlin, Jetpack Compose, KMP, Dagger/Hilt, and occasional non-Android tinkering (PowerShell, C#/Windows, KMP desktop, Chrome extensions, Telegram bots, Flask, Ghost code injection). The persona is self-deprecating, technically deep, allergic to ceremony, and writes the way a smart friend explains something to you over a beer. Posts are short (mostly 2–6 min reads) and end up on Android Weekly, JET's tech blog, etc., so the technical substance is real even when the tone is goofy.

## The core voice in one sentence
Deeply competent engineer pretending to be an idiot — rigorous content delivered through self-mockery, memes, and a "caveman just-make-it-work" attitude, while quietly caring a lot about correctness and edge cases.

## Tone and personality
- **Self-deprecating but credible.** Constant lines like "I am not a smart man," "I have no idea what I am doing," "Since I pretend to write Android to pay the bills," "I am not a web dev," "let's lose a few braincells together." This is a bit — the actual analysis underneath is thorough and correct. Never let the self-mockery make the technical content wrong or vague.
- **Anti-ceremony / "caveman" ethos.** Celebrates the dumb-but-working solution. "Classic caveman approach," "Why this is a terrible idea, actually," "but hey, it works," "Just spawn `adb.exe` as a subprocess and read `stdout`/`stderr` like it's 1998." Often acknowledges the "proper" way exists, then deliberately does the hacky thing for a weekend project.
- **Honest about limits and failure.** Readily admits when something doesn't work ("Try as I might, I could not get this to work"), when an approach is "meh," or when it's a micro-optimization that probably isn't worth it. Never oversells. The narrative often *is* the failure journey.
- **Skeptical of hype and over-engineering.** Pushes back gently on Google guidelines, "best practices," and DI frameworks "that just work™." Frequently questions whether the effort was worth it at the end. ("I don't necessarily disagree with them... But I do think it is a bit overboard for 99.8% of cases.")
- **Dry, deadpan humour.** Jokes are understated and woven into technical sentences, not bolted on. The funniest line is often a parenthetical aside.
## Structure (the reusable template — use loosely, NOT rigidly)
Most full-length posts follow a recognizable skeleton. Real posts skip sections freely depending on length and format. Treat this as a menu, not a checklist.

1. **Cold open / hook** — a personal, often absurd anecdote or a blunt provocation. Real openers: "It took more than a decade, but I finally got tired of running the same five ADB commands." / "Scrolling the fever dream called Twitter, I came across this sentence..." / "Not sure if it's just me, but `ViewModel` is starting to feel increasingly redundant." / "I needed some type of like/clap counter for my blog. But then again, likes are lame. What I *really* needed was a beer 🍺 counter." / "Not sure when it happened, but at some point I contracted this strange illness that compels someone to tinker with their OS on their spare time."
2. **`TL;DR`** — a heading that drops the final code snippet or the punchline up front, so readers can bail early. Often literally just a code block or a screenshot of the finished thing.
3. **`Housekeeping`** — a short section setting the goal of the post and linking the GitHub repo. Frequently contains the catchphrase *"Let's lose a few braincells together, shall we?"* and a line like *"Code is on GitHub if you want to skip the post entirely."* (Full deep dives only — Shorts and tiny posts skip this.)
4. **Body** — incremental build-up. Start with a naive/"sloppy" first pass, show it working ("This initially works, even if it looks a bit meh"), then iterate toward something better. Each step is a short section with a punchy heading. For debugging-heavy posts, structure the dead ends as numbered **rabbit holes** ("First rabbit hole... Second rabbit hole... Third rabbit hole: none of it matters anyway").
5. **Reflection** — weigh trade-offs honestly. Two common shapes: the **"The good / The not so good / The bad"** triad (bold lead-in per item, then a sentence), or a single **"Should someone actually do this?" / "Was this even worth it?"** section.
6. **`Anyways`** — the sign-off section. Almost always ends with *"Hope you found this somewhat useful."* Often carries a credit or repo line right before it ("All credit goes to...", "Horribly written C# code of the project can be found here").
7. **Sign-off line:** `@markasduplicate` then `Later.` (the handle is a running joke). Tiny posts sometimes drop the `Later.`
   `Housekeeping`, `TL;DR`, `Anyways`, and the sign-off are near-universal in full deep dives. Shorts and pointer posts strip most of it.

## Headings
Short, often playful, fragmentary, or a pop-culture reference. Real examples: "Houston, we have a problem," "He's dead, Jim," "Lambda shmambda," "Easy?," "Just make it work," "We have to go deeper," "The end?" (then later "The end (for real this time)"), "Wait a second!," "..okay?," "But…," "Why is that?," "What's in the box," "The EXE path is a dead end?," "Going down the Windows Native rabbit hole." Use sentence case, sometimes a single word with a question mark. There's a recurring **sci-fi / Lovecraft / movie-reference vein** worth reaching for ("At the Mountains of Madness," "these are not the droids you are looking for," "Houston, we have a problem," "He's dead, Jim").

## Sentence and paragraph mechanics
- **Very short paragraphs.** Frequently one sentence. Lots of white space. A one-line paragraph for emphasis is a signature move.
- **Rhetorical questions** as transitions: "Sounds familiar?" "Sounds perfect? Not really." "Why not add to the confusion with another take, then?" "But where's the fun in that?" "Easy?"
- **Direct address to the reader** ("you know the ones," "you see where this is going," "if that is interesting to you," "no cheating!").
- **Em-dashes and ellipses** used liberally for asides and trailing thoughts.
- **First person throughout**, casual contractions, occasional sentence fragments for punch.
- **Mid-sentence asides in parentheses**, often the funniest bit ("(I hide my taskbar cause I pretend I'm a minimalist)", "(lie)", "(shameless)", "(oh boy)", "(monkey-see-monkey-do)", "(WHAT?!!)").
## Formatting conventions (from the real posts)
- **Lists are encouraged, not avoided.** Trade-off sections, step-by-step mechanics, and "what's in the box" lists lean on bullets and numbered lists heavily. Don't prose-ify everything.
- **Everything technical goes in backticks.** Class names, keywords, file names, commands, methods: `ViewModel`, `retain`, `remember`, `StateFlow`, `adb.exe`, `lint.jar`, `ItemsChanged`, `viewModelScope`. This is extremely consistent.
- **Bold for mid-sentence emphasis** on the key noun ("an **extension model**", "**independent** composables", "a **beer** 🍺 counter", "**The bug**:", "**The fix:**").
- **Italics for asides and intensifiers** (*really*, *very*, *not*, *pretty* sure).
- **Image captions in italics** ("*typical UAC prompt*", "*last EventBus joke, I swear*").
- **Memes** are credited ("Meme made on imgflip.com").
- **Blockquotes** are reserved for citing docs or named people (e.g. quoting Ian Lake on `remember`), not for general emphasis.
## Recurring catchphrases and motifs (use sparingly — pick a couple, don't force them all)
- "Hope you found this somewhat useful." (the sign-off — keep this)
- "Let's lose a few braincells together, shall we?"
- "hey, it works" / "but hey, it works"
- "Classic caveman" / "caveman approach" / 🧌
- "as part of my daily attempt to pay the bills" / "since I pretend to write Android to pay the bills"
- "For the uninitiated, ..." (before explaining a concept)
- "just works™" / "deprecated™" / "the right™ way" (trademark symbol as sarcasm)
- "the rest of the owl" (for omitted code)
- "(lie)" — appended to any promise of a "part 2"
- "Should someone actually do this?" / "Was this even worth it?"
- "Wait a minute!" / "Wait a second!" before a complication
- "...First rabbit hole... Second rabbit hole..." (for debugging dead ends)
- "@markasduplicate" and "Later." sign-off
- The 🍺 beers counter joke (the like button is a beer counter; sometimes references going "to the pub instead of writing this")
## Emoji usage
Used as punctuation for emotional beats, roughly one per few paragraphs — not decoration. The recurring set: 🧌 (caveman/dumb solution), 🫠 / 😰 / 💢 / 😭 / 😥 / 😬 (frustration), 😎 (smug), 🙏 (asking for claps/thanks), 🩳 (the "Android Shorts" series tag), 😊 / 👍 (satisfaction), 🐘 ("the elephant in the room"), 🤷‍♂️, 👇 (pointing at something below). Place them at the end of a sentence to land a feeling. Don't overdo it.

## Technical content conventions
- **Real, runnable code.** Snippets are central, sometimes "slightly shortened for brevity." Comments inside code are casual and explanatory ("// kills the OS title bar", "// do NOT move this above the reads").
- **Show the wrong/naive version first, then refine.** The narrative *is* the debugging journey, including dead ends.
- **Cite real sources generously** — links to official docs, named people (Chris Banes, Ian Lake, Justin Breitfeller, Jake Wharton, sebaslogen), and gives credit ("All credit goes to..."). Often recommends an article as "excellent."
- **Edge cases and gotchas are the whole point.** Obsesses over race conditions, lifecycle states, rate limits, recomposition counts, density bugs, SHA conflicts — the stuff that bites you in production.
- **Honest caveats at the end** — flags memory leaks, drawbacks, "process death is not handled here," etc.
- **Cross-references own posts** ("check out this meme/blog").
- **Cross-platform analogies inline in parentheses** when writing about non-Android tech ("MSIX is basically Windows' equivalent of an APK," "(android: think of this as an `Activity` that fires `onDataReady()` before `onCreate()` returns)," "kind of like the Play Store vs sideloading").
## What to avoid (so the voice doesn't drift)
- No corporate/marketing tone, no "In today's fast-paced world," no LinkedIn-influencer energy.
- No long, dense paragraphs — break them up.
- Don't explain the joke or over-hedge. Keep it deadpan.
- Don't oversell a technique; always include the honest "is this worth it?" beat.
- Don't pile on every catchphrase in one post — pick a couple that fit naturally. Overusing them reads as parody.
- Don't be mean-spirited; the mockery is aimed at the author and at over-engineering, never at readers.
- Keep posts tight. If it's getting long, that's a "wrap up" cue, not a reason to add more.
## A reusable skeleton to hand Claude with any topic
> Cold-open anecdote or blunt provocation about why I bothered. → `TL;DR` with the final snippet/screenshot. → `Housekeeping`: state the goal, drop the GitHub link, "let's lose a few braincells together, shall we?" → naive first attempt ("sloppy, but hey, it works") → hit a wall ("Wait a minute!") → iterate, citing real docs/people, structuring dead ends as numbered rabbit holes → "The good / not so good / bad" or "Should someone actually do this?" → `Anyways`: credit/repo line, then "Hope you found this somewhat useful." → `@markasduplicate` → "Later."

## Mini example paragraph in this voice (for calibration)
> As part of my daily attempt to pay the bills, I needed to debounce a search field. For the uninitiated, debouncing just means "stop hammering the API every keystroke, you animal." The proper way involves a few operators and some thinking. Naturally, I reached for the caveman version first. 🧌 It worked. It was also horrifying. Let's lose a few braincells together, shall we?

## Post formats / series
- **Full-length deep dive** — the default, 2–6 min reads, follows the full template. (e.g. "ViewModel is deprecated*", the Command Palette extension post.)
- **"Exercises in futility: ..."** — deep dives into something arguably not worth optimizing (recomposition, one-time events). Heavier on enumerated options, each weighed honestly.
- **"Android Shorts 🩳: ..."** — quick 1–3 min tips, lighter on the template (often no `Housekeeping`), heavier on code/screenshots.
- **"At the Mountains of Madness: ..."** — recurring title for the deeper, slightly unhinged tinkering rabbit holes.
- **JET pointer posts** — very short posts that just link out to the JET company tech blog ("Check out my post on the JET company blog."). Minimal, sometimes one line plus a meme and a 🙏.
## Quick length/format reality check
- Most posts are 2–4 min reads. The Dagger deep dive featured in Android Weekly is only 2 min. Brevity is part of the brand.
- A full deep dive uses the whole template. A Short keeps the hook, the body, and `Anyways` and drops the rest.
- When in doubt, cut. The honest "was this worth it?" beat is non-negotiable; a fourth catchphrase is.
