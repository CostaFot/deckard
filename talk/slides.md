---
theme: seriph
background: https://cover.sli.dev
title: Fighting AI slop with anti-slop
info: |
  Fighting AI slop with anti-slop — GDG (Android circuit) talk about Deckard,
  the on-device AI-slop detector. On-device LLMs with LiteRT-LM, accessibility,
  and the new Compose/Lifecycle encapsulation APIs.
class: text-center
drawings:
  persist: false
transition: slide-left
mdc: true
---

# Fighting AI slop with anti-slop

The on-device Android engineering behind a slop detector

<div class="pt-16 text-sm opacity-70">
Costa Fotiadis · <code>@markasduplicate</code>
</div>

<!--
Cold open, before advancing: no agenda slide, no "about me" slide. Straight into the bit.
-->

---
layout: center
---

# The internet is drowning in slop

<div class="mt-6 mx-auto flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="width: 420px; height: 320px">
  <div class="text-center px-6">
    🖼️ <b>PLACEHOLDER — real slop post #1</b><br>
    <span class="text-sm">actual screenshot of a maximally sloppy LinkedIn post —
    🚀 emojis, "I'm humbled to announce…", the works</span>
  </div>
</div>

<!--
TODO need rocketemojis interspersed

Flash slide — a couple of seconds, let the room read it, move on. Real content gets the
laugh that a parody can't.
-->

---
layout: center
---

<div class="mx-auto flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="width: 420px; height: 320px">
  <div class="text-center px-6">
    🖼️ <b>PLACEHOLDER — real slop post #2</b><br>
    <span class="text-sm">second real specimen — different flavour (X thread bait /
    "Let that sink in" / AI-image engagement farm)</span>
  </div>
</div>

<v-click>

<div class="pt-8 text-2xl text-center">
And I got <b>sick</b> of it.
</div>

</v-click>

<!--
Second flash — pass through quickly, then land the line: "did a person write this, or did
they paste it out of ChatGPT? I genuinely can't tell anymore — and it's everywhere. And I got sick of it."

TODO add another slide next with placeholder for my Victoria Beckham meme
-->

---
layout: center
---

# So I built a thing that tells me

<div class="mt-6 mx-auto flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="width: 640px; height: 300px">
  <div class="text-center px-8">
    🎬 <b>PLACEHOLDER — hero demo GIF</b><br>
    <span class="text-sm">scrolling LinkedIn → maximum-slop post (🚀 "I'm humbled to announce…")
    → summon Deckard → verdict: <i>"This is slop, son."</i></span>
  </div>
</div>

<!--
THE most important asset in the talk. Let the GIF play, say nothing for a few seconds.

The app proves itself before a single word of engineering. Then: "that's the whole
product. The rest of this talk is what's inside it."
-->

---

# Why "Deckard"?

<div class="grid grid-cols-2 gap-8 pt-4">

<div>

<div class="flex items-center justify-center border-2 border-dashed rounded-xl opacity-70 mb-4" style="height: 180px">
  🖼️ PLACEHOLDER — Blade Runner Deckard
</div>

**Blade Runner** — the guy whose job is testing what's *human* vs *artificial*.

</div>

<div>

<div class="flex items-center justify-center border-2 border-dashed rounded-xl opacity-70 mb-4" style="height: 180px">
  🖼️ PLACEHOLDER — Deckard Cain (Diablo)
</div>

**Deckard Cain** — the wise old man who *identifies your unidentified items*.

</div>

</div>

<v-click>

<div class="pt-6 text-center text-xl">
You bring him your loot. He tells you what it's worth. — <i>"This is slop, son."</i>
</div>

</v-click>

<!--
TODO remove "you bring him your loot.. son" thing

~10 seconds, millennial double reference — both halves are intended.

Blade Runner: detects what's human. Diablo: you bring Cain an unidentified item, he tells
you what it is. That's literally the UX — and it's why the verdict talks like that.
-->

---
layout: center
class: text-center
---

# So… how is this done?

<!--
TODO replace with "wow Costa you are so cool how did you do this" and "glad you asked" meme

The pivot out of the cold open. Next: the machine at a glance, then we descend.
-->

---

# The machine, at a glance

<div class="grid grid-cols-5 items-center gap-2 pt-10">

<v-click>
<div class="col-span-1 border-2 rounded-xl p-4 text-center">
  <div class="text-4xl">🧙</div>
  <div class="font-bold pt-2">The face</div>
  <div class="text-sm opacity-70 pt-1">an overlay <code>Service</code> — floats over <b>every</b> app</div>
</div>
</v-click>

<div class="text-center text-3xl opacity-50">→</div>

<v-click>
<div class="col-span-1 border-2 rounded-xl p-4 text-center">
  <div class="text-4xl">👀</div>
  <div class="font-bold pt-2">The eyes</div>
  <div class="text-sm opacity-70 pt-1">reads the screen — a11y tree <i>or</i> screenshot + model</div>
</div>
</v-click>

<div class="text-center text-3xl opacity-50">→</div>

<v-click>
<div class="col-span-1 border-2 rounded-xl p-4 text-center">
  <div class="text-4xl">🧠</div>
  <div class="font-bold pt-2">The brain</div>
  <div class="text-sm opacity-70 pt-1">the verdict — an API call (Pangram)</div>
</div>
</v-click>

</div>

<v-click>

<div class="pt-10 text-center opacity-80">
The brain is the easy part — it's an HTTP call.<br>
<span class="text-sm opacity-70">(and it refuses to judge fewer than ~50 words — short text can't be classified reliably)</span>
</div>

</v-click>

<v-click>

<div class="pt-4 text-center text-xl">
The <b>eyes</b> are where the story is. 👀
</div>

</v-click>

<!--
TODO I do not like the exaplanation "the brain is the easy part... the story is" etc let's skip all that

"Let's look at this thing at a glance — then we go into detail."

One beat only — this is the map for the descent, not a lecture. Three pieces:
- the face: the floating mascot, a system overlay, no Activity anywhere (that lands later)
- the eyes: reading the screen — the whole middle of the talk
- the brain: the slop verdict is literally an API call. Won't even bother under ~50 words.

Land the last line and descend: "so let's talk about the eyes. How hard can reading a
screen be?"
-->

---

# Attempt #1: just read the screen

<div class="pt-2 opacity-80">How hard can it be? Android hands you the whole UI tree — you just walk it.</div>

```kotlin
// only an AccessibilityService can see other apps' UI
class DeckardAccessibilityService : AccessibilityService() {

    fun readScreen(): String? {
        val root = rootInActiveWindow      // the foreground app's view tree
        return root.collectVisibleText()   // walk the nodes, gather the text
    }
}
```

<v-click>

<div class="pt-4 text-center text-xl">
Structured. Free. Returns in <b>milliseconds</b>. No model needed. 😎
</div>

</v-click>

<!--
TODO: add slide before this with "interstellar" meme this is going to take us 10 years making a joke about a weekend project typically turning itself into a 1 month long project

(Heavily elided, like every snippet in this deck.)

The naive plan: I started by just reading the accessibility tree — the thing screen
readers use. Every view, its text, its bounds. It's RIGHT THERE.

"This is what I built first. And it works… sort of."
-->

---

# Small print: it's harder than it seems

<div class="grid grid-cols-2 gap-8 pt-4">

<div>

<v-clicks>

- You need an **`AccessibilityService`** — be honest, have *you* ever written one?
- You need the **scariest permissions on the platform**:
  - accessibility — <i>"full control of your device"</i> ⚠️
  - `SYSTEM_ALERT_WINDOW` — draw over every app
  - screenshots — also via the accessibility service
- And the **user** has to be walked through granting all of it

</v-clicks>

</div>

<div class="flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="height: 280px">
  <div class="text-center px-6">
    🖼️ <b>PLACEHOLDER</b><br>
    <span class="text-sm">screenshot of Android's scary
    "Allow Deckard full control of your device?" accessibility dialog</span>
  </div>
</div>

</div>

<!--
Most devs have never touched an AccessibilityService — it's niche, and the permission
dialog Android shows is genuinely terrifying (rightly so).

Plant quietly: "remember how scary this permission set is. It comes back later." (That's
the privacy bridge setup.)
-->

---

# War story #1: LinkedIn lies to you

<div class="pt-2 opacity-80">A collapsed post's <i>visible</i> text ends in "…more". Where's the rest?</div>

```kotlin
// the FULL post hides in contentDescription — the visible text is truncated
fun ScreenNode.bestText(): String {
    val text = text?.trim().orEmpty()
    val description = contentDescription?.trim().orEmpty()
    return if (description.length > text.length) description else text
}
```

<v-click>

<div class="pt-3 opacity-80">
…and the feed is Compose: anonymous <code>android.view.View</code>s, no IDs, and the post body
node is sometimes a <code>TextView</code>, sometimes a <code>Button</code>. You match on <b>content, not class</b>.
</div>

</v-click>

<div class="mt-4 flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="height: 130px">
  <div class="text-center px-6">
    🖼️ <b>PLACEHOLDER</b> — annotated tree-dump excerpt: <code>text="…more"</code> vs
    the full post sitting in <code>contentDescription</code>
  </div>
</div>

<!--
TODO make this simpler. People just need to see a small snippet and an image with a post with "..more". Can we do side by side? 

First contact with reality: the tree is only as good as the app developer made it — and
you don't control LinkedIn.

So you write LinkedIn-specific logic just to FIND the post body. Note the foreshadowing:
this is already one bespoke parser.
-->

---

# War story #2: X declares war

<div class="pt-2 opacity-80">On the timeline, a tweet exposes <b>no per-element text at all</b>. The entire card is ONE string:</div>

<div class="mt-4 p-4 border rounded-xl text-sm leading-relaxed font-mono">
<span class="bg-blue-500 bg-opacity-30 rounded px-1">molson 🧠⚙️ @Molson_Hart Verified.</span>
<span class="bg-green-500 bg-opacity-30 rounded px-1">The tariffs are going to hit small businesses first, and here's the part nobody is talking about…</span>
<span class="bg-red-500 bg-opacity-30 rounded px-1">2 hours ago.</span>
<span class="bg-red-500 bg-opacity-30 rounded px-1">2 replies.</span>
<span class="bg-red-500 bg-opacity-30 rounded px-1">3 reposts.</span>
<span class="bg-red-500 bg-opacity-30 rounded px-1">34 likes.</span>
<span class="bg-red-500 bg-opacity-30 rounded px-1">2569 verified views.</span>
</div>

<div class="pt-2 text-sm opacity-70">
<span class="bg-blue-500 bg-opacity-30 rounded px-1">byline</span>
<span class="bg-green-500 bg-opacity-30 rounded px-1">the actual tweet</span>
<span class="bg-red-500 bg-opacity-30 rounded px-1">chrome you don't want</span>
— name, handle, body, timestamp and engagement counts, all fused into one
<code>contentDescription</code>.
</div>

<v-click>

<div class="pt-6 text-center text-xl">
To get the tweet out… you parse it back apart. With regexes. 🫠
</div>

</v-click>

<!--
TODO in this case I think it's better to get a placeholder image with a few examples or quote tweets, reply tweets etc. reading the accesibility with those is a mess

The hostile case. X concatenates the whole card into a single contentDescription so a
screen reader reads it as one unit. There is no child TextView holding just the body.

The only way to get the tweet body is to strip the byline off the front and the
metrics/timestamp off the back of one giant string.
-->

---

# But first, the "scalable architecture" 😎

<div class="pt-2 opacity-80">One interface per app. Dispatch on the foreground package. What could go wrong?</div>

```kotlin
interface ScreenContentExtractor {
    fun handles(packageName: String): Boolean   // "com.linkedin.android"?
    fun extract(root: ScreenNode): String?      // the text worth judging
}
```

```kotlin
class ScreenContentExtractors @Inject constructor(
    private val extractors: Set<ScreenContentExtractor>,
    private val generic: GenericContentExtractor,   // unknown-app fallback
) {
    fun extract(packageName: String, root: ScreenNode): String? =
        (extractors.firstOrNull { it.handles(packageName) } ?: generic).extract(root)
}
```

<!--
TODO probably need a slide previously with a "challenge accepted" meme. put placeholder

This looks GREAT in a design doc. Clean seam, Hilt multibinding, add an app = one class +
one binding. I was very proud of it.

Deadpan: "I was building a beautiful, extensible system… for hand-writing a parser for
every app on Earth."
-->

---

# The reality: one extractor's worth of "handling X"

```kotlin
/** "… 2 replies.  3 reposts.  34 likes.  2569 verified views." */
val TRAILING_METRICS = Regex(
    "(?:\\s*[\\d,]+\\s+(?:repl(?:y|ies)|reposts?|quotes?|likes?|bookmarks?|(?:verified\\s+)?views?)\\.)+\\s*$")

val TRAILING_TIMESTAMP = Regex("\\s*\\d+\\s+\\w+\\s+ago\\.\\s*$")
val TRAILING_REPOST    = Regex("\\s*Reposted by .*$")

/** "molson 🧠⚙️ @Molson_Hart Verified. " — or unverified: "antirez @antirez. " */
val LEADING_BYLINE = Regex("^.*?@\\w+\\b(?:\\s+Verified)?\\.\\s*")

/** a quote tweet packs TWO posts into one description… */
val QUOTE_LEAD     = Regex("^.*?Quoted\\.\\s+[^.\\n]*?@\\w+\\b(?:\\s+Verified)?\\.\\s+")
val QUOTER_COMMENT = Regex("@\\w+\\b(?:\\s+Verified)?\\.\\s+Added\\s+(.+)$")
```

<v-clicks>

- Quote tweets pack **two posts into one string** — we judge the text after the word *"Added"*
- `"Promoted."` cards are skipped — Deckard refuses to judge ads
- A display name containing a `"."` defeats the byline parser — **one guy named "Dr. Smith" breaks everything**

</v-clicks>

<!--
This slide is allowed to hurt — that's the point. Let it sit for a moment before the gags.

"This is real code. It's still in the repo. It is frozen at 'good-enough' because every
fix reveals a new special case."
-->

---
layout: center
---

# That was <span v-mark.red="1">one</span> app

<div class="pt-6 text-xl text-center leading-relaxed">

<v-clicks at="2">

- LinkedIn ✅ <span class="opacity-60">(mostly)</span>
- X ✅ <span class="opacity-60">(frozen at "good-enough")</span>
- Reddit? The browser? <b>Every app you've never seen?</b>

</v-clicks>

</div>

<v-click>

<div class="pt-8 text-center text-2xl">
A per-app parser isn't a roadmap. It's a <b>treadmill</b>. 🏃‍♂️
</div>

</v-click>

<!--
TODO placeholder meme "sweating guy"

The dead end, said plainly: I really tried. Interfaces and implementations per app,
special-casing the browser, content-vs-class matching, centre-of-screen heuristics.

There is no way to handle everything for every app. Full stop.
-->

---

# Aside: that tree is about to matter *more*

<div class="pt-4 text-xl leading-relaxed mx-auto" style="max-width: 36rem">

The accessibility tree failed <i>me</i>…

<v-click>

…but in an **agent future**, that tree is how assistants will **drive your app**.

</v-click>

<v-click>

Write your app accessibly and you're not just serving screen readers —
you're exposing an **API for whatever agent your user runs**.

</v-click>

</div>

<v-click>

<div class="pt-8 text-center text-xl opacity-90">
Accessibility is becoming your app's API. Maybe treat it like one. 🤷‍♂️
</div>

</v-click>

<!--
30-second aside, right while the wound is fresh — the irony is the point.

X's hostile single-blob tree isn't just bad for me, it's bad for every future agent
trying to use X on the user's behalf. The apps that expose a clean tree will be the apps
agents can actually operate.

Then pivot: "anyway. Back to my problem. The tree was a dead end, so…"
-->

---
layout: center
class: text-center
---

# Wait. Maybe the model can just… <i>look</i> at it? 🤔

<v-click>

<div class="pt-6 text-xl opacity-90">
These vision models are getting good.<br>
Take a <b>screenshot</b> — let the model figure out what the relevant text is. <i>Itself.</i>
</div>

</v-click>

<v-click>

<div class="pt-8 text-2xl">
screenshot → model → the post
</div>

<div class="pt-4 opacity-70">
One idea. Replaces the entire treadmill.
</div>

</v-click>

<!--
The realisation, told as it happened. No per-app code. No regexes. The model does the
"which text matters" reasoning I was hand-writing per app.

Don't oversell yet — the payoff proof comes after we get the model running.
-->

---

# Step one: get the pixels

<div class="pt-2 opacity-80">Small irony: guess which class is the <b>only</b> one allowed to take a screenshot…</div>

```kotlin
// yep. still the AccessibilityService. you never escape it. 🙃
takeScreenshot(Display.DEFAULT_DISPLAY, executor, callback)
```

<v-click>

```kotlin
// hardware buffer → bitmap → shrink → JPEG (the model doesn't need your 4K screen)
val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
val scaled = bitmap.downscale(maxDimension = 1024)
val jpeg   = scaled.compress(JPEG, quality = 85)
```

</v-click>

<v-click>

<div class="pt-4 opacity-80 text-center">
Feed the model a shrunken JPEG, not a raw screen — a vision model's time is <b>expensive</b>.
</div>

</v-click>

<!--
The irony beat: the "dead end" tech is still the only door to the pixels. The
AccessibilityService stays; it just changes jobs — from reading the tree to grabbing the
screen.

Downscaling matters: inference time scales with input size, and 1024px is plenty for
reading text.
-->

---
layout: center
---

# One problem.

<div class="pt-6 text-xl text-center leading-relaxed">

<v-click>

This thing sees <b>everything on your screen</b>.<br>
<span class="opacity-80">Your bank. Your chats. Your email. Your questionable 2am searches.</span>

</v-click>

<v-click>

<div class="pt-6">
Now imagine shipping all of that, screen by screen,<br>
to a remote LLM <b>owned by somebody else</b>. ☁️
</div>

</v-click>

<v-click>

<div class="pt-8 text-3xl">
No. 🙅
</div>

<div class="pt-3 opacity-70">
No user would accept that. No user <i>should</i> accept that.
</div>

</v-click>

</div>

<!--
Callback to the scary-permissions slide — "remember that terrifying dialog? This is why
it matters."

A cloud vision API would make everything that follows trivial: no hardware constraints,
one HTTP call. And it's exactly the thing you must not do with god-mode screen access.
This is also why nobody ships apps like this.
-->

---
layout: center
class: text-center
---

# So the brain has to live <b>on the phone</b>

<v-click>

<div class="pt-8 text-2xl opacity-90">
Which raises a question…<br>
<span class="text-3xl"><b>can you even run an LLM on a phone?</b></span>
</div>

</v-click>

<!--
The bridge lands. The screen never leaves the device — that's the deal that makes the
permissions acceptable.

And now the talk owes the audience an answer: yes — and here's how. Into LiteRT-LM.
-->

---

# "Doesn't Android just… give you this?"

<div class="pt-4 text-lg leading-relaxed">

<v-clicks>

- Sort of! Google is turning Android into an intelligent system — **Gemini Nano** and
  on-device models, exposed to any app, **for free**
- The catch: **gated**. Limited devices, and **quotas** on who calls it and how much
  <span class="opacity-70">(your battery says thanks)</span>
- Fine for *a feature*. Not for an app whose whole job is **hammering a vision model**

</v-clicks>

</div>

<v-click>

<div class="pt-8 text-center text-2xl">
So: bring your own model. 💪
</div>

</v-click>

<!--
VERIFY BEFORE THE TALK: current AICore / Gemini Nano availability, device list, quota
specifics, API names — this area moves fast. Don't quote stale details on stage.

The point survives any update: the platform path is rationed; BYO gives you full control.
-->

---

# Bring your own brain 🧠

<div class="pt-2 text-lg leading-relaxed">

- A multimodal **Gemma**, one `.litertlm` file, **~3 GB**
  <span class="opacity-70">— but any model works; the loading story is the same</span>

</div>

```bash
# the caveman delivery pipeline 🧌
adb push gemma-3n.litertlm /sdcard/Android/data/<pkg>/files/models/
```

<v-click>

<div class="pt-4 opacity-80">
Real apps do this properly: the user downloads the model at runtime via <b>Play delivery</b>.
We're hacking around with adb, and I'm not sorry.
</div>

</v-click>

<v-click>

<div class="pt-4 text-sm opacity-60">
(The engine class still lives in a package called <code>suggestion/llm/</code> — this app used to be a
<i>keyboard</i>. The LLM is the sole survivor of the pivot.)
</div>

</v-click>

<!--
TODO thhe ADB snippet should show up as transition when going forward 

Get the "you're not seriously shipping over adb" question out of the way before anyone
asks it — Play Asset Delivery / Play's on-device AI delivery is the production path.

The keyboard line is a throwaway — one beat, move on.
-->

---

# The engine: LiteRT-LM

```kotlin
val engine = Engine(
    EngineConfig(
        modelPath     = modelFile.absolutePath,
        backend       = Backend.GPU(),      // ← the whole war is over this line
        visionBackend = Backend.GPU(),      // multimodal: images go to the GPU too
        maxNumTokens  = 1024,
    ),
)
engine.initialize()   // takes SECONDS — warm up early, off the main thread
```

<v-click>

```kotlin
fun engineOrNull(): Engine?   // non-blocking: null while loading, never freezes the UI
```

</v-click>

<v-click>

<div class="pt-4 opacity-80 text-center">
Warm up once per process on an app-scoped coroutine; callers degrade gracefully until the
brain is online.
</div>

</v-click>

<!--
Two design points worth saying out loud:
- init takes seconds, so it runs on an application-scoped coroutine no caller can cancel
- engineOrNull() returns null while loading — summon Deckard too early and he just tells
  you his brain isn't ready, nothing blocks

"And see that Backend.GPU() line? Let me tell you about the two weeks that line cost me."
-->

---
layout: center
---

# First run: it worked! 🎉

<v-click>

<div class="pt-4 text-2xl text-center">
…at roughly <b>one token per geological era</b>. 🐌
</div>

</v-click>

<v-click>

<div class="mt-8 p-4 border rounded-xl font-mono text-sm text-left mx-auto" style="max-width: 32rem">
E/litert: GPU backend initialization failed: INTERNAL<br>
I/litert: falling back to CPU
</div>

<div class="pt-4 text-center opacity-80">
The GPU init fails with an opaque <code>INTERNAL</code> error…<br>
then it <b>silently</b> falls back to CPU — <b>~10× slower</b>. Basically unusable.
</div>

</v-click>

<!--
TODO probably need to show it here with a gif/video. side by side with  the text.

The war story, spend time here. The model loaded, generated text, everything "worked" —
except a summon took the better part of a minute. It was shit.

Nothing crashes. No exception reaches you. You just get a slow app and one cryptic line
in logcat. What would YOU google for "INTERNAL"?
-->

---

# The hardest bug in the project: four lines of XML

```xml
<!-- AndroidManifest.xml -->
<uses-native-library android:name="libOpenCL.so"        android:required="false" />
<uses-native-library android:name="libvndksupport.so"   android:required="false" />
<uses-native-library android:name="libcdsprpc.so"       android:required="false" />
<uses-native-library android:name="libedgetpu_litert.so" android:required="false" />
```

<v-click>

<div class="pt-4 text-lg leading-relaxed">

**Why:** the GPU delegate is OpenCL. On Android 12+, your app **cannot `dlopen` a vendor
library it hasn't declared.** No declaration → no OpenCL → that `INTERNAL` error → CPU.

</div>

</v-click>

<v-click>

<div class="pt-4 text-center text-xl">
Four lines. Two weeks. 🫠
</div>

</v-click>

<!--
TODO explanation not good here. we need simple stuff. noone is familiar with what OpenCL is.

This is the slide the LiteRT half of the talk exists for. The single most useful thing an
audience member ships next week.

Android 12 locked down native library loading; anything not on the app's declared list is
invisible at runtime. The GPU delegate needs the VENDOR's OpenCL — so you must declare
it, required="false" so you still install on devices without it.
-->

---

# More fine print from the trenches

<div class="pt-2 text-lg leading-relaxed">

<v-clicks>

- **Pin your runtime.** `litertlm = "0.11.0"` — 0.12.0 regressed GPU for these Gemma builds
- **How do you even debug that?** Diff your app against **Google's AI Edge Gallery** sample
  until the configs match. <i>The reference app is the real documentation</i> 🤷‍♂️
- **Have a fallback ladder** — keep the first backend that survives:

</v-clicks>

</div>

<v-click>

```kotlin
for (backend in listOf(Backend.GPU(), Backend.CPU(numOfThreads = 4), Backend.NPU(…))) {
    runCatching { return initialized(backend) }   // GPU → CPU → NPU, first one wins
}
```

</v-click>

<!--
TODO remove this slide. not useful

The 0.12.0 regression cost days: same code, new version, GPU gone. Downgrade → works.
There was no changelog entry that would tell you.

AI Edge Gallery is Google's open-source showcase app for on-device models — when the docs
run out, you read its source and diff against your own config. That's how the manifest
fix was found too.
-->

---

# The honest cost: there's a hardware floor

<div class="pt-2 opacity-80">Same symptom — everything lands on the CPU. <b>Two</b> very different causes:</div>

<div class="grid grid-cols-2 gap-6 pt-6">

<v-click>
<div class="border-2 rounded-xl p-5">
  <div class="text-xl font-bold">Your fault 🔧</div>
  <div class="pt-2 opacity-80">Missing <code>&lt;uses-native-library&gt;</code> lines.</div>
  <div class="pt-2 text-green-500 font-bold">Fixable — four lines of XML.</div>
</div>
</v-click>

<v-click>
<div class="border-2 rounded-xl p-5">
  <div class="text-xl font-bold">The phone's fault 📱</div>
  <div class="pt-2 opacity-80">A ~2-year-old device: no usable GPU delegate. Falls to CPU with a <i>perfect</i> manifest.</div>
  <div class="pt-2 text-red-500 font-bold">Not fixable. A 3 GB model on CPU is a paperweight.</div>
</div>
</v-click>

</div>

<v-click>

<div class="pt-8 text-center text-xl leading-relaxed">
The a11y tree was fragile across <b>apps</b>. The model is fragile across <b>devices</b>.<br>
<span class="text-2xl">I traded app-fragility for device-fragility — and for this problem, that's the right trade.</span>
</div>

</v-click>

<!--
Be honest here — it's the counterweight to all the enthusiasm: an on-device LLM is a
"works on a recent flagship" feature, not a "works on Android" feature.

The synthesis line is the intellectual summary of the entire middle of the talk. Land it
slowly.
-->

---

# The code that replaced the regex wall

<div class="grid grid-cols-2 gap-4 pt-2">

<div class="opacity-40 text-xs">

```kotlin
val TRAILING_METRICS = Regex(
  "(?:\\s*[\\d,]+\\s+(?:repl(?:y|ies)|reposts?|…")
val TRAILING_TIMESTAMP = Regex("\\s*\\d+\\s+\\w+\\s+ago…")
val TRAILING_REPOST    = Regex("\\s*Reposted by .*$")
val LEADING_BYLINE     = Regex("^.*?@\\w+\\b(?:\\s+Verif…")
val QUOTE_LEAD         = Regex("^.*?Quoted\\.\\s+[^.\\n]…")
val QUOTER_COMMENT     = Regex("@\\w+\\b(?:\\s+Verified)…")
// × every app on Earth
```

</div>

<v-click>

<div class="border-2 rounded-xl p-4 text-sm leading-relaxed">

*"Exactly **one** main piece of writing is what the user is reading — find it and copy out
its body text, **word for word**.*

*Do not summarize, paraphrase, shorten, correct, or translate.*

*Leave everything else out: names, @handles, timestamps, buttons, like counts, ads,
'Suggested for you', the keyboard…"*

</div>

</v-click>

</div>

<v-click>

<div class="pt-6 text-center text-xl">
It's not Kotlin. It's <b>a paragraph of English</b>. And it works on apps I've never seen. 🤯
</div>

</v-click>

<!--
The hero slide of the whole LLM act. Left: the treadmill (greyed out, already suffered
through). Right: the entire replacement.

One prompt. Zero per-app code. The model does the "which text matters" reasoning — it
UNDERSTANDS the screen layout; it doesn't just transcribe pixels.
-->

---

# It *understands* the screen — it doesn't just OCR it

<div class="pt-2 text-lg leading-relaxed">

<v-clicks>

- The model **picks the post out of the noise itself** — layout comprehension, not
  transcription. <span class="opacity-70">This is *recent* — you couldn't trust a local model with this a year ago.</span>
- **The verbatim rule is load-bearing:** if the model rewrites the text even slightly, it
  hands the detector *AI-written* text — and poisons the verdict ☠️
- The prompt literally tells it to **ignore the floating mascot** — Deckard must be
  instructed not to judge his own face 🧙
- And there's a `clean()` function because the model *insists* on wrapping answers in
  quotation marks. Peak LLM-era engineering.

</v-clicks>

</div>

<div class="mt-4 flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="height: 120px">
  <div class="text-center px-6">
    🎬 <b>PLACEHOLDER</b> — long-press demo: the model isolates the post on an app
    with <b>no extractor written for it</b>
  </div>
</div>

<!--
The verbatim nuance is the sharpest technical point in this act: the LLM is a READER, not
a summarizer. Any rewriting biases Pangram toward "AI" — the whole pipeline depends on the
model resisting its own urge to be helpful.

The demo GIF should be an app never handled in the a11y era — that's the proof.
-->

---
layout: center
---

# Remember what we were building?

<div class="mt-6 mx-auto flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="width: 560px; height: 260px">
  <div class="text-center px-8">
    🎬 <b>PLACEHOLDER — the demo again, zoomed in</b><br>
    <span class="text-sm">mascot + report card, close up. "Hey look — this works!"</span>
  </div>
</div>

<v-click>

<div class="pt-6 text-center opacity-80">
(There's even a third door: <b>share</b> any text from any app straight to Deckard —<br>
share sheet → verdict. No screen reading involved at all.)
</div>

</v-click>

<!--
The audience has been in the engine room for ten minutes — resurface. This is the same
hero demo, zoomed on the verdict.

Then the turn into the second movement: "I've talked services, models, API calls. But
everything you just SAW — the mascot, the bubble, the report card — is Compose. And
here's the thing…"
-->

---
layout: center
---

# Everything you just saw is a real app

<div class="pt-6 text-xl text-center opacity-90">
Pure Compose UI · ViewModels · repositories · API calls · DI<br>
<span class="opacity-70">— the whole boring stack.</span>
</div>

<v-click>

<div class="pt-10 text-3xl text-center">
And there is <b>no Activity anywhere</b>. 😳
</div>

</v-click>

<!--
TODO probably put these as bullet points animating in on every transition

Hammer the point: this isn't "some Compose in an overlay" — it's a complete app
architecture (ViewModels talking to repositories doing API calls, all DI'd) running in a
place where NONE of the usual machinery exists.
-->

---
layout: center
---

# How did I even go about this?

<v-click>

<div class="pt-4 text-xl text-center opacity-90 leading-relaxed">
Normally we have the <i>luxury</i> of an Activity quietly handing us everything —<br>
lifecycle, ViewModel store, saved state. Here: nothing.
</div>

</v-click>

<v-click>

<div class="pt-8 text-xl text-center leading-relaxed">
Turns out an Activity is just <b>three registries in a trench coat</b>…<br>
so I became one. 🥸
</div>

</v-click>

<v-click>

<div class="pt-8 text-xl text-center">
But that's <i>my</i> weird problem. Here's the part that's <b>yours</b>…
</div>

</v-click>

<!--
TODO we should show these 3 registries and basically hammer home that as long as compose is handed those, everything works  fine. Keep short and simple

One beat only — do NOT descend into overlay plumbing. The Service implements the three
owner interfaces and sets them as view-tree owners; that's the whole story, and it's in
the repo for anyone curious.

The room doesn't build overlays. The next slide is about the screen they maintain at
work.
-->

---

# Your problem: the very busy screen

<div class="grid grid-cols-2 gap-8 pt-4">

<div class="flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="height: 300px">
  <div class="text-center px-6">
    🖼️ <b>PLACEHOLDER</b><br>
    <span class="text-sm">a genuinely busy production screen —
    Just Eat Takeaway home screen</span>
  </div>
</div>

<div class="text-lg leading-relaxed">

<v-clicks>

- You're asked to change **one small component** in there
- In Compose that means: the **hundred-parameter composable** 😰
- Thread your state, callbacks, and dependencies through **everything above it**…
- …and break a hundred call sites on the way

</v-clicks>

</div>

</div>

<v-click>

<div class="pt-6 text-center text-xl">
A composable is <b>really hard to make independent</b> on a busy screen.
</div>

</v-click>

<!--
This is the WIIFM hook — I work on the JET app, this screen is real, and everyone in the
room maintains one like it.

Remember Fragments? Self-contained: own lifecycle, own ViewModel, own DI. You dropped one
into a layout and it managed itself. Compose took that away — every composable inherits
the host's owners, so everything gets threaded from the top.
-->

---

# What we actually want

<div class="pt-4 text-xl leading-relaxed mx-auto" style="max-width: 36rem">

A composable that:

<v-clicks>

- **makes its own dependencies** 🏗️
- **owns its own ViewModel** — scoped *exactly to the composition*
- survives **recomposition and rotation**
- and **cleans up after itself** when it leaves 🧹

</v-clicks>

</div>

<v-click>

<div class="pt-8 text-center opacity-80 text-lg">
Scoping a ViewModel used to be the <i>navigation library's</i> job — or an Activity's.<br>
<b>Not anymore.</b> Two (new-ish) APIs:
</div>

</v-click>

<!--
Name the problem precisely before showing APIs: scoping + DI + cleanup, all local to the
composable.

The classic answers were "put it on the nav graph" or "scope it to the Activity" — both
mean the composable depends on something far above it. The new APIs kill that dependency.
-->

---

# API #1: `retain` — like `remember`, but tougher

```kotlin
@Composable
fun BeerCounter() {
    // like remember { } — but it ALSO survives configuration changes 💪
    val counter = retain { Counter() }
}
```

<v-clicks>

- Retention at the **Compose-runtime level** — scoped to the composition, not to an
  Activity's `ViewModelStore`
- Rotate the phone: `remember` → gone, **`retain` → still there**
- Zero plumbing: no owners, no factories, no nav graph — works **anywhere a composition
  exists**
- Wants cleanup? Implement `RetainObserver` — you get `onRetained` / `onRetired` callbacks

</v-clicks>

<!--
The plain API first — androidx.compose.runtime.retain. One line to adopt: swap
remember{} for retain{} where the value should outlive a config change.

RetainObserver is the hook for anything that needs a lifecycle: onRetired is your
"onCleared" moment. Which is exactly the ingredient for the next slide…
-->

---

# …so I built a ViewModel out of it 🧪

```kotlin
abstract class RetainedViewModel : RetainObserver {
    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onRetired() { onCleared(); viewModelScope.cancel() }   // 🧹
}

@Composable
inline fun <reified T : RetainedViewModel> rememberRetainedViewModel(
    noinline factory: (Context) -> T,
): T {
    val context = LocalContext.current
    return retain { factory(context) }
}
```

<v-click>

```kotlin
// DIY dependency injection — the factory reaches straight into the DI graph:
val viewModel = rememberRetainedViewModel { context ->
    EntryPoints.get(context, SampleEntryPoint::class.java).sampleRetainedViewModel()
}
```

</v-click>

<v-click>

<div class="pt-3 text-center opacity-90">
Scope ✅ cleanup ✅ DI ✅ — a ViewModel that works in an Activity, a Service, an IME, an
overlay. <b>The sky's the limit.</b> 💸
</div>

</v-click>

<!--
TODO "you dont even need a ViewModel anymore" and make it 3 code snippets. animate every transition in so I can do through them without overloading the audience

The complex solution, built on the plain API: ~15 lines and you have the full ViewModel
experience with none of the owner machinery.

- RetainedViewModel: a coroutine scope + onCleared, driven by RetainObserver
- rememberRetainedViewModel: fetch-or-create via retain{}
- DI: the factory lambda is YOURS — grab a Hilt entry point / your application component
  and inject whatever you want. No @HiltViewModel, no ViewModelProvider.Factory.

This is real code from the keyboard era of this very app.
-->

---

# API #2: `rememberViewModelStoreOwner` — real ViewModels, locally scoped

```kotlin
@Composable
fun ComponentViewModelScope(key: Any, content: @Composable () -> Unit) {
    saveableStateHolder.SaveableStateProvider(key) {
        val storeOwner = rememberViewModelStoreOwner()   // ← this composable OWNS a store
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides storeOwner,
            content = content,
        )
    }
}
```

<v-click>

```kotlin
// inside: plain, boring viewModel() — but scoped to THIS subtree, not the Activity
ComponentViewModelScope(key = "beer-counter") {
    val vm: BeerCounterViewModel = viewModel(factory = factory)
}
```

</v-click>

<v-click>

<div class="pt-3 text-center opacity-90">
Everything below the provider sees <i>this</i> store — the VM lives and dies with the
subtree. <b>No navigation library required.</b>
</div>

</v-click>

<!--
Where retain is the lightweight runtime-level answer, rememberViewModelStoreOwner gives
you the REAL androidx ViewModel machinery — a store this composable owns, shadowing the
host's via LocalViewModelStoreOwner.

This snippet is from THIS repo (ComponentViewModelScope.kt) — it's how independent
composables in the overlay own their ViewModels.
-->

---

# Bonus: Navigation 3 — nav that isn't chained to an Activity

```kotlin
val backStack = rememberNavBackStack(ScreenA)     // a plain list. YOU own it.
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),  // ← per-screen VMs, cleared on pop
    ),
    entryProvider = entryProvider {
        entry<ScreenA> { ScreenA(viewModel(factory = factory)) }
        entry<ScreenB> { /* … */ }
    },
)
```

<v-clicks>

- **Pure Compose** — not tied to an Activity, not tied to *anything*. Great for **KMP** too
- I ran this — back stack, per-screen ViewModels, the lot — **inside the overlay. No
  Activity.** And it just worked 🤯

</v-clicks>

<div class="mt-3 flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="height: 90px">
  <div class="text-center text-sm px-6">
    🎬 <b>PLACEHOLDER</b> — GIF: Nav3 panel navigating inside the overlay, no Activity anywhere
  </div>
</div>

<!--
⚠️ CUTTABLE SLIDE — Costa decides in the trim pass.

Navigation 3: the back stack is literally a mutableStateList you own; NavDisplay renders
it; decorators bolt on saveable state + per-entry ViewModel scoping (cleared on pop —
ViewModel.onCleared fires when the screen is popped).

Proving ground: I ran the full thing inside the WindowManager overlay — screens, back
navigation, per-screen DI'd ViewModels, retained across recomposition, cleared on pop.
Write-up lives in the repo (notes/nav3-viewmodels-in-a-service-overlay.md).
-->

---
layout: center
---

# The Fragment-shaped hole: filled 🧩

<div class="pt-4 text-xl text-center leading-relaxed">

<v-clicks>

Any composable, anywhere in the tree, can now<br>
**own its state, its ViewModel, and its dependencies** —<br>
and tear them down correctly —<br>
**without asking permission from anything above it.**

</v-clicks>

</div>

<v-click>

<div class="pt-8 text-center text-lg opacity-80">
That busy screen? Change the small component <b>in place</b>. Nothing above it needs to know. 😌
</div>

</v-click>

<!--
Land the WIIFM: this is what Fragments used to give you — the self-contained drop-in —
and what Compose quietly lost.

Callback to the JET screen: surgery on a busy screen with a contained blast radius. This
is the everyday value; the overlay was just the stress test.
-->

---

# Takeaway #1 — local LLMs are ready. I was surprised too.

<div class="pt-4 text-xl leading-relaxed">

<v-clicks>

- Getting one running is **quite easy** — a file, a library, four lines of XML
  <span class="opacity-70">(you now know which four)</span>
- They're **not that big** — a couple of GB gets you a *multimodal* model
- And they're **capable enough** — capable enough to *understand a screen*, not just read it

</v-clicks>

</div>

<v-click>

<div class="pt-8 text-center text-xl leading-relaxed">
And this is the <b>worst</b> they'll ever be.<br>
<span class="opacity-80">Imagine what we'll get out of them next year. 🔮</span>
</div>

</v-click>

<!--
Honest enthusiasm: I went in expecting a science project and came out with a working
screen-reader brain on a phone.

The forward lean: models this size get better every quarter. The engineering you saw
today is the boring, stable part — the brains keep improving underneath it.
-->

---

# Takeaway #2 — Compose finally feels… complete

<div class="pt-4 text-lg leading-relaxed">

<v-clicks>

- Let's gossip: **scoping a ViewModel to a composable** — and surviving rotation — never
  had a clear-cut answer. Everyone hacked around it or let a nav library do it. All of it
  **chained to an Activity** underneath 🙊
- Everyone quietly asked: *"how do I get my old Fragment back — the nice way, in pure
  Compose?"* There wasn't one.
- These were **core APIs**. Honestly? They should have shipped years ago.

</v-clicks>

</div>

<v-click>

<div class="pt-6 text-center text-xl leading-relaxed">
But they're here: <code>retain</code> · <code>rememberViewModelStoreOwner</code> · Navigation 3.<br>
<b>This is the year Compose is really ready to replace Views and Fragments.</b>
</div>

</v-click>

<!--
Deadpan, not mean — the frustration is real but the ending is genuinely positive.

Compose has been out for years and was missing load-bearing pieces the whole time; we
all just accepted the workarounds as normal. The fact that a floating wizard overlay and
your busy production screen are fixed by the SAME two APIs is the proof they got the
abstraction right.
-->

---
layout: center
class: text-center
---

# That's the talk

<div class="pt-4 text-xl opacity-90">
Go build something. Accessibly, please — the agents are coming. 🤖
</div>

<div class="pt-10 flex items-center justify-center gap-8">

<div class="flex items-center justify-center border-2 border-dashed rounded-xl opacity-70" style="width: 140px; height: 140px">
  <span class="text-sm px-2">🖼️ QR →<br>repo link</span>
</div>

<div class="text-left text-lg">
  <div>🧙 <b>Deckard</b> — code on GitHub</div>
  <div class="pt-2 opacity-80"><code>@markasduplicate</code></div>
  <div class="pt-2 opacity-60 text-sm">Hope you found this somewhat useful.</div>
</div>

</div>

<div class="pt-10 opacity-70">
Questions? <span class="opacity-60">(Deckard will judge them for slop.)</span>
</div>

<!--
Short and out. The a11y line is the one-sentence echo of the §3b aside — last thing they
hear that isn't a joke.

Sign-off is the blog sign-off. "Later."
-->
