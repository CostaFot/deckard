# Fighting AI slop with anti-slop

The internet is drowning in slop.

Every feed, every day: 🚀 emojis, "I got fired on Monday.", five numbered life lessons nobody asked for. Did a person write this, or did they paste it out of ChatGPT? I can't tell anymore.

![A maximally sloppy LinkedIn post](./assets/linkedin_slop_post.png)

*a real specimen, caught in the wild*

So I built a thing that tells me.

I gave a talk about it at GDG's Android Circuit recently — this post is that talk, retrofitted into blog form. Same war stories, fewer memes. (Some memes.)

## TL;DR

![Demo: summoning Deckard over a LinkedIn post and getting the AI-slop verdict](./assets/demo_fast.gif)

Meet **Deckard**. A floating mascot that lives in a system overlay, over every app on the phone. Summon him on whatever you're reading and he tells you whether a human wrote it.

Under the hood:

- an `AccessibilityService` reads the screen — either straight off the accessibility tree, or via a screenshot fed to an **on-device Gemma** model that plucks out the main post
- the text goes to the **Pangram** API, an AI-text detector
- the verdict comes back as a report card

That's the whole product. The rest of this post is what's inside it.

## Housekeeping

Two things I want you to leave with:

1. **Shipping an on-device LLM with LiteRT-LM** — and the handful of gotchas that cost me weeks
2. **New-ish Compose/Lifecycle APIs** (`retain`, `rememberViewModelStoreOwner`) that let a composable own its own state, ViewModel, and dependencies — the drop-in encapsulation we lost when Fragments faded

Code is on GitHub if you want to skip the post entirely. *[TODO: repo link]*

Let's lose a few braincells together, shall we?

## Astute observers might have noticed

Scroll back up to that slop post. There's a little **🤖 AI** badge sitting right next to the author's name.

![The slop post zoomed on the AI badge](./assets/linkedin_slop_post_zoomed_in.png)

I didn't draw that. It was already there, flagging the post as AI-generated, in the wild, on LinkedIn.

Where does it come from?

## AI is very good at detecting other AI

That badge is **Pangram** — an AI-text detector. It ships as a Chrome extension that auto-tags AI-generated posts on a handful of sites, LinkedIn among them. (I'm not affiliated with them in any way. Just a happy user.)

The intuition behind why this works at all: AI writes with a very particular fingerprint. No matter how hard you prompt a model to "write like a human" or "don't sound like AI", it can't escape it — the training bakes it in. A detector trained on that signal picks it up even when a human can't.

And Pangram has an API. This is the *entire* integration:

```bash
# send the text
curl https://text.external-api.pangram.com/task \
  -H "x-api-key: $PANGRAM_API_KEY" \
  -d '{ "text": "🚀 3 days. Zero regrets. …" }'
# → { "task_id": "…" }
```

```bash
# poll for the verdict
curl https://text.external-api.pangram.com/task/$TASK_ID \
  -H "x-api-key: $PANGRAM_API_KEY"

# → { "stage": "STAGE_SUCCESS", "fraction_ai": 1.0 }
```

Detection takes a few seconds, so it's async: one POST with the text, then poll the task id. `fraction_ai: 1.0` is the 100% on the report card. In the app this is a two-method Retrofit interface.

So the "is this AI?" part is a solved problem — for a browser, on a select few websites.

I wanted that same verdict **everywhere**. Every app. System-wide, on a phone. 📱

Deckard's whole job is reading the text off the current screen and shipping *that* to Pangram. Same detector, new eyes.

The eyes are the hard part.

## Attempt #1: just read the screen

Android hands you the whole UI tree — just walk it.

```kotlin
class DeckardAccessibilityService : AccessibilityService() {

    fun readScreen(): String? {
        val root = rootInActiveWindow      // the foreground app's view tree
        return root.collectVisibleText()   // walk the nodes, gather the text
    }
}
```

(Heavily elided, like every snippet in this post.)

`rootInActiveWindow` is the whole magic: the framework hands you the foreground app's view tree — the same one TalkBack uses — and the rest is a tree walk. Every view, its text, its bounds. Structured. Fast. No AI needed.

This is what I built first. And it works… sort of.

## Not so fast

First hurdle: you need an `AccessibilityService`, which most Android devs have never touched. And you need two scary permissions:

- **Accessibility access** — *observe your actions, retrieve window content, perform gestures, take screenshots* ⚠️
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — for the floating mascot

![Android's accessibility consent dialog](./assets/scary_permission.png)

*Android is right to make this dialog terrifying*

Remember how scary this permission set is. It comes back later.

## WTF #1: LinkedIn hands you *more* than you see

![A collapsed LinkedIn post ending in "…more"](./assets/linked_in_more_collapsed.png)

This post looks short — it's truncated at "…more".

But the reader doesn't stop there. It hands back the **entire post**. LinkedIn stores the full text in the node's `contentDescription` (so TalkBack can read it aloud), and the tree walk returns all of it.

What you *see* is not what your code *gets*. The tree is richer than the pixels. Annoying here, but survivable.

Then I opened X.

## WTF #2: Twitter/X

On the timeline, the **entire tweet card is one `contentDescription`**. Name, handle, "Verified", the actual tweet, reply counts, repost counts, likes, views, timestamp — one string:

> ~~bobby @bobby Verified.~~ **Clavicular ran into a frat leader at ASU and got brutally frame mogged by him👀😂** ~~14 replies. 92 reposts. 1,203 likes. 88,417 views. 3h~~

The bold bit is the only bit you actually want. No child node holds just the body, and the only way to get it out is… regex. 🫠

## Let's write some Java 1998

Fine. I'll just handle every app myself.

One interface, one parser per app:

```kotlin
interface ScreenContentExtractor {
    fun handles(packageName: String): Boolean   // "com.linkedin.android"?
    fun extract(root: ScreenNode): String?      // the text worth judging
}
```

…then wire them all up:

```kotlin
class ScreenContentExtractors @Inject constructor(
    private val extractors: Set<ScreenContentExtractor>,
    private val generic: GenericContentExtractor,   // unknown-app fallback
) {
    fun extract(packageName: String, root: ScreenNode): String? =
        (extractors.firstOrNull { it.handles(packageName) } ?: generic).extract(root)
}
```

This looks *great* in a design doc. Clean seam, Hilt multibinding, adding an app is one class and one `@IntoSet`. I was very proud of it.

I was building a beautiful, extensible system… for hand-writing a parser for every app on Earth.

## The reality: regex wars

Here's what one of those parsers actually costs. This is the X extractor:

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

This is real code. It's still in the repo. It's frozen at "good-enough", because every fix reveals a new special case:

- a quote tweet fuses **two posts** into one string, so you have to decide which author you're judging (the quoter's comment — the text after the word "Added")
- promoted cards need skipping, so Deckard refuses to judge ads
- a display name containing a "." defeats the byline strip — one person named "Dr. Smith" breaks the parser

And that was **one** app. Not even done well.

What about Reddit? Medium? Chrome? Every app you've never heard of? There is no way to handle everything for every app.

## A quick aside — the agents are coming

Sitting in that dead end, a thought: what if **agents** become the de facto way of using a device?

ChatGPT, Gemini, Claude computer use, whatever Siri turns into — they all drive apps the same way I was trying to: through the accessibility tree. The same one-blob layout that beat me will beat every agent trying to operate X on the user's behalf.

The apps that expose a clean tree will be the apps agents can actually use. Write your app accessibly and you're exposing an API for whatever assistant your user runs — screen readers included.

Anyway. Back to my problem.

## Maybe an AI model can just… *look* at it

The realisation, told as it happened: these models are getting good. What if I take a **screenshot** and let a model figure out what the relevant text is?

No per-app code. No regexes. The model does the "which text matters" reasoning I was hand-writing per app.

## Step one: get the pixels

```kotlin
// yep. still using the AccessibilityService
takeScreenshot(Display.DEFAULT_DISPLAY, executor, callback)
```

The irony: only an `AccessibilityService` can call `takeScreenshot()`. The "dead end" tech is still the only door to the pixels — it changes jobs, from reading the tree to grabbing the screen.

Then capture, downscale, compress:

```kotlin
val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
val scaled = bitmap.downscale(maxDimension = 1024)
val jpeg   = scaled.compress(JPEG, quality = 85)
```

A vision model's time is expensive — inference scales with input size, and 1024px is plenty for reading text.

## Houston, we have a problem

This thing sees **everything on the screen** — your messages, your banking app, all of it.

Now imagine shipping all of that to a remote LLM.

A cloud vision API would make everything that follows trivial: no hardware constraints, one HTTP call. It's also *exactly* the thing you must not do with god-mode screen access. Remember that terrifying permission dialog? This is why it matters — and why nobody ships apps like this.

So the screen-reading brain has to live **on the phone**. Can you even run an LLM on a phone?

## Bring your own brain

"Doesn't Android just… give you this?" Sort of. Gemini Nano exists, but it's **gated** (limited devices) and **quota'd**. Fine for a feature; useless for an app whose whole job is hammering a vision model. Where's the fun in that?

So, bring your own:

**1.** Grab a model off Hugging Face — a multimodal **Gemma** `.litertlm`, about 3 GB.

**2.** Push it onto the phone:

```bash
adb push gemma-4-E4B-it.litertlm /sdcard/Android/data/<pkg>/files/models/
```

Yes, `adb push` is the caveman distribution channel. 🧌 A real app would download the model at runtime via Play delivery. For a weekend project, the cable wins.

## Using LiteRT-LM

```kotlin
// build.gradle.kts
implementation("com.google.ai.edge.litertlm:litertlm-android:0.11.0")
```

```kotlin
val engine = Engine(
    EngineConfig(
        modelPath     = modelFile.absolutePath,
        backend       = Backend.GPU(),      // GPU is faster!
        visionBackend = Backend.GPU(),
        maxNumTokens  = 1024,
    ),
)
engine.initialize()   // takes a while
```

Two design points worth stealing:

- `initialize()` takes seconds, so warm-up runs on an application-scoped coroutine no caller can cancel
- `engineOrNull()` returns `null` while loading — summon Deckard too early and he just tells you his brain isn't ready, nothing blocks

See those `Backend.GPU()` lines? Let me tell you about the two weeks they cost me.

## First run: everything worked!

…but it was *so slow*. A single summon took the better part of a minute.

That's the trap: nothing crashes. No exception reaches you. Just a slow app and one cryptic line in logcat:

```
E/litert: GPU backend initialization failed: INTERNAL
I/litert: falling back to CPU
```

An opaque `INTERNAL` error, then a **silent** fallback to CPU — roughly 10× slower.

What would you even google for `INTERNAL`?

## Four lines of XML

The fix:

```xml
<!-- AndroidManifest.xml -->
<uses-native-library android:name="libOpenCL.so"        android:required="false" />
<uses-native-library android:name="libvndksupport.so"   android:required="false" />
<uses-native-library android:name="libcdsprpc.so"       android:required="false" />
<uses-native-library android:name="libedgetpu_litert.so" android:required="false" />
```

The GPU delegate runs on OpenCL, and the OpenCL library is a *vendor* lib that lives on the phone — if you're lucky. Since Android 12, an app can't `dlopen` a native library it hasn't declared in its manifest. Undeclared means invisible, invisible means `INTERNAL`, `INTERNAL` means CPU. (`required="false"` keeps the app installable on devices that don't have the lib.)

For the record, I did *not* reason this out from first principles. There was nothing to google. So I diffed my app against Google's own **AI Edge Gallery** sample — the official app runs the same Gemma on GPU, so *what's different?* Two things fell out of that diff: these four manifest lines, and the `0.11.0` version pin (`0.12.0` regressed GPU for these Gemma builds).

The meta-lesson beats the specific fix: when the platform hands you an opaque error, find the first-party sample that works and diff against it. The reference app is the real documentation.

## Old phones will still choke

The honest counterweight: loading a **3 GB model** into memory — and its weights onto the GPU — takes a powerful phone.

Two ways it dies on an old device:

- **Capacity** — the whole model has to be resident in RAM. 3 GB on a 4 GB phone doesn't fit next to Android plus your app.
- **GPU** — a weak or unsupported GPU fails at init (that same `INTERNAL`) and silently lands on CPU, which is unusable.

An on-device LLM is a "recent flagship" feature, not a "works on Android" feature. Some floors software can't lift.

The trade, in one sentence: the accessibility tree was fragile across **apps**; the model is fragile across **devices**. I swapped app-fragility for device-fragility — and for this problem, that's the right trade.

## Asking it something

The whole OCR path is one call — it's multimodal, so you hand it the prompt *and* the screenshot:

```kotlin
val conversation = engine.createConversation()
val reply = conversation.sendMessage(prompt, screenshot)
```

And what is `prompt`? Remember what it used to be:

```kotlin
val TRAILING_METRICS   = Regex("(?:\\s*[\\d,]+\\s+(?:repl(?:y|ies)|reposts?…")
val LEADING_BYLINE     = Regex("^.*?@\\w+\\b(?:\\s+Verif…")
val QUOTE_LEAD         = Regex("^.*?Quoted\\.\\s+[^.\\n]…")
// × every app on Earth
```

Now:

```kotlin
val prompt = """
    This is a screenshot of a social-media feed or article.
    Find the one thing the user is reading and copy its body
    text out, word for word. Leave out names, handles, buttons,
    like counts, ads, and the mascot. Output only that text.
""".trimIndent()
```

Same variable, different world. N brittle parsers became a paragraph of plain English that works on apps I've never seen.

Two nuances hiding in that paragraph:

- **"word for word" is load-bearing.** The model is a *reader*, not a summarizer. If it rewrites the text even slightly, the output picks up an AI fingerprint and biases Pangram toward "AI" — the whole pipeline depends on the model resisting its own urge to be helpful.
- **"and the mascot."** Yes, the prompt has to tell the model to ignore Deckard's own face floating over the post. He must not judge himself.

## Does it actually work?

![Verdict on a human-written post: Human Written, confidence high](./assets/human_written.jpg)
![Verdict on an AI-generated post: AI-written, confidence high](./assets/robot_written.png)

*one human, one robot — both caught*

It does.

## Or… just share it

There's a third summon path that involves no screen reading at all: select text in any app, hit the system **share sheet**, pick "Judge with Deckard".

![Selecting text in a LinkedIn post](./assets/select_text.jpg)
![The share sheet with the Deckard target](./assets/share_improved.jpg)

This one is free — a `ShareTextActivity` plus an intent filter, and Android does the plumbing. Also the demo that can't fail on stage.

## The good, the bad

**The good:**

- The model **finds the post itself** — strips the chrome, the buttons, the noise
- **Verbatim** output — no rewriting, no detector poisoning

**The bad:**

- Not 100% — once in a while it grabs the wrong text
- Not instant — a vision inference per summon

## Everything you just saw is a real app

Time for the second half of the contract. Everything above — the mascot, the bubble, the report card — is:

- pure **Compose** UI
- **ViewModels**
- **Repositories** making API calls
- **DI** (Hilt)

The whole boring stack. And there is **no `Activity` anywhere**.

A complete app architecture, running in a place where none of the usual machinery exists.

How?

## Three registries in a trench coat

An `Activity` is (mostly) three registries in a trench coat:

- `LifecycleOwner`
- `ViewModelStoreOwner`
- `SavedStateRegistryOwner`

Compose needs these three things and nothing else — an `Activity` provides them without you ever noticing. Supply them yourself and Compose runs anywhere: a `Service`, an IME, a window overlay.

In Deckard, the overlay `Service` implements all three owner interfaces itself, drives its own `LifecycleRegistry`, and stamps itself onto each overlay view:

```kotlin
private fun attachOwners(view: View) {
    view.setViewTreeLifecycleOwner(this)
    view.setViewTreeViewModelStoreOwner(this)
    view.setViewTreeSavedStateRegistryOwner(this)
}
```

Compose feels at home.

Now — you probably don't build overlays for a living. Fair. But the next part is about the screen you maintain at work.

## What's in it for me?

![A busy restaurant menu screen](./assets/menu_default_screen.jpg)

*a real production screen I work on. you maintain one like it.*

You know this screen. Every app has one:

- a **God ViewModel** (or three)
- 100 API calls, 50 feature toggles
- 10 people working on it, 3 teams trying to catch the next release

And in Compose it looks like this:

```kotlin
@Composable
fun MenuScreen(
    restaurant: Restaurant,
    offers: List<Offer>,
    basket: Basket,
    deliveryEta: Eta,
    onItemClick: (MenuItem) -> Unit,
    onAddToBasket: (MenuItem) -> Unit,
    onOfferClick: (Offer) -> Unit,
    onBack: () -> Unit,
    // …plus 90 more params
) { /* … */ }
```

Now change one small thing on that screen. You get to:

- thread your state, callbacks, and dependencies through **everything above it**
- break a hundred call sites on the way
- …and a hundred screenshot and UI tests
- (don't get me started on default parameters)

Remember Fragments? Self-contained: own lifecycle, own ViewModel, own DI. You dropped one into a layout and it managed itself. Compose quietly took that away — every composable inherits the host's owners, so everything gets threaded from the top.

The classic workarounds — hang state off the nav graph, scope to the Activity — all mean your composable depends on something far above it. The new APIs kill that dependency.

## API #1: `retain`

First: a composable that **keeps its own state**.

```kotlin
import androidx.compose.runtime.retain.retain

@Composable
fun BeerCounter() {
    // like remember — but also survives configuration changes
    val counter = retain { Counter() }
}
```

`retain` is retention at the **Compose-runtime level**, scoped to the composition itself. Rotate the phone: `remember` → gone, `retain` → still there. No Activity, no owner plumbing, no nav library.

It ships as a separate artifact, `androidx.compose.runtime:runtime-retain` — public API, not experimental.

⚠️ One honest caveat: `retain` does **not** handle process death / saved state. Rotation yes, low-memory kill no.

## Do we even need `ViewModel` anymore?

`retain` comes with a hook called `RetainObserver` — `onRetired()` fires when the retained value is finally let go. Which is… suspiciously close to `onCleared()`. So, naturally:

```kotlin
abstract class RetainedViewModel : RetainObserver {
    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onRetired() {
        viewModelScope.cancel()
        onCleared()
    }
}
```

No `androidx.lifecycle.ViewModel` anywhere. Its own coroutine scope, its own teardown — the whole "ViewModel contract" in five lines.

DI? The factory lambda is yours, so reach straight into the Hilt graph:

```kotlin
// DIY dependency injection — the factory reaches straight into the DI graph
val viewModel = rememberRetainedViewModel { context ->   // = retain { factory(context) }
    EntryPoints.get(context, SampleEntryPoint::class.java).sampleRetainedViewModel()
}
```

No `@HiltViewModel`, no `ViewModelProvider.Factory`.

Should someone actually do this? **Not really.** `ViewModel` works, it's proven, and your team already knows it. But it's a funny little experiment — and building it forces you to understand `retain`, `RetainObserver`, and where state actually lives.

## API #2: `rememberViewModelStoreOwner`

Where `retain` is the lightweight runtime-level answer, this one gives you the *real* androidx `ViewModel` machinery — owned by a composable:

```kotlin
@Composable
fun ComponentViewModelScope(content: @Composable () -> Unit) {
    val storeOwner = rememberViewModelStoreOwner()   // ← this composable OWNS a store
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides storeOwner,
        content = content,
    )
}
```

Inside the subtree, it's plain, boring `viewModel()` — but scoped to *this* subtree:

```kotlin
ComponentViewModelScope {
    val vm: BeerCounterViewModel = viewModel()
}
```

The store owner shadows the host's via `LocalViewModelStoreOwner`, and the ViewModel lives and dies with the subtree — leave the composition, store cleared, `onCleared()` called. Saved state works the same way via `rememberSaveableStateHolder()`.

(This snippet is straight from the Deckard repo — it's how independent composables in the overlay own their ViewModels.)

## Wait a minute…

A component that owns its own state, its own ViewModel, its own dependencies, its own lifecycle…

Yeah. It's a `Fragment`.

Except this time it's pure Compose — no `FragmentManager`, no transactions, no XML. The drop-in self-containment was the one thing worth missing about Fragments, and it's back.

And that busy production screen? Change the small component *in place*. It makes its own ViewModel, injects its own dependencies, retains its own state. Nothing above it needs to know. The overlay was just the stress test.

## Was this even worth it?

Two takeaways.

**Local LLMs are actually quite good now.** I went in expecting a science project and came out with a working screen-reader brain on a phone. Getting one running is not too hard, a couple of GB gets you a *multimodal* model, and they're capable enough. The engineering in this post is the boring, stable part — the brains keep improving underneath it.

**Compose is in a good place.** Scoping a ViewModel to a composable never had a clear-cut answer — everyone hacked around it or let a nav library do it, all chained to an Activity underneath. `retain` and `rememberViewModelStoreOwner` should've shipped with Compose 1.0, but better late than never — and they play very nicely with **Navigation 3**, which is built on exactly these primitives. A floating mascot overlay and your busy production screen get fixed by the same APIs, which is a decent sign they got the abstraction right.

## Anyways

Not affiliated with Pangram, not sponsored by Google — just a man who wanted to know if a LinkedIn post was written by a robot.

Code is on GitHub. *[TODO: repo link]* Go build something — accessibly, please. The agents are coming.

Hope you found this somewhat useful.

[@markasduplicate](https://x.com/markasduplicate)

Later.
