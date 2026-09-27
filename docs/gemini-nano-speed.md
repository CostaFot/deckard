# How fast can Gemini Nano read a screen?

Notes from COS-263, 15 September 2026. The question was whether the ten seconds a summon takes on
the Magic V5 could be configured away. The answer is no, and the reason is more interesting than
the number.

## The setup

Deckard reads the screen by handing Gemini Nano (nano-v3, through the ML Kit Prompt API,
`com.google.mlkit:genai-prompt:1.0.0-beta4`, served by AICore) a screenshot and a prompt that says:
find the one post the user is reading and copy its body out word for word. The verbatim rule is
load-bearing, because the text goes to an AI detector next and a paraphrase would bias the verdict.

To measure the levers one at a time without a summon, a verdict or a paid Pangram call, the debug
build got a broadcast receiver (`NanoBench`) that takes the accessibility service's own screenshot,
re-scales it, runs one read under a chosen configuration inside the same foreground stage the real
read uses, and logs every step's time plus the text it got back. `scripts/deckard nano-bench`
drives it from the desk:

```
scripts/deckard nano-bench dim=512 runs=2
scripts/deckard nano-bench prompt=system
scripts/deckard nano-bench stream=true
```

The page under test was a post on costafotiadis.com scrolled so that 74 words of body text were
visible, with an illustration above and a promo image below. Every configuration was run at least
twice; the numbers below are the warm ones unless they say otherwise.

## What the levers did

| Configuration | Read | Input tokens | Words back |
|---|---|---|---|
| As shipped: 1024 px image, rules in the user turn | 5.0 to 5.5 s | 486 | 74 |
| 512 px image | 5.3 to 5.5 s | 486 | 74 |
| Rules as a system instruction, short user ask | 5.3 to 5.5 s | 508 | 74 |
| Output cap of 400 tokens | 5.3 to 5.5 s | 486 | 74 |
| Warm-up call immediately before the read | 5.3 to 5.5 s | 486 | 74 |
| Fast model preference (`ModelPreference.FAST`) | refused | | |
| Cached prompt prefix | refused | | |

Nothing moved. Two levers were not even available:

- The fast model preference fails inside 10 ms with `FEATURE_NOT_FOUND: Feature 645 is not
  available`. The preference is not a mode of the model the phone has; it is a different model,
  and the Magic V5 does not serve it. What the picker can and cannot name is its own section,
  *Which model this is*, below. `scripts/deckard nano-bench pref=fast` after a system update is
  the check for whether that has changed.
- The cached context API exists (`isCachingFeatureAvailable` is true and creating one takes
  268 ms) but the request builder throws `cacheContextName is not supported for image input`. A
  cached prefix is for text-only chat.

The image token count is the tell: 486 tokens at 1024 px and 486 at 512 px. The image is a fixed
cost however big it is, roughly 258 tokens, and the rules are the other 228. Shrinking the picture
saves nothing and can only lose letters.

## Which model this is, and which it could be

The names confuse because two families run side by side, one open and one on the phone:

- **Gemini Nano** is what AICore serves. It is Google's, closed, and arrives with system updates;
  the app does not choose a version. On the Magic V5 `getBaseModelName()` says `nano-v3`, even
  though Google's device list still files the phone under nano-v2.
- **Gemma** is the open-weight family built on the same architecture. nano-v3 is built on
  [Gemma 3n](https://blog.google/products/gemini/gemini-nano-pixel-10-updates/), in Google's own
  words at the Pixel 10 launch. Gemma 4, released into AICore as a developer preview on
  [2 April 2026](https://developer.android.com/blog/posts/announcing-gemma-4-in-the-ai-core-developer-preview)
  in E2B and E4B sizes, is "the foundation for the next generation of Gemini Nano": Gemini Nano 4,
  due on devices later in 2026. So Gemma 3n pairs with nano-v3 and Gemma 4 with Nano 4. nano-v3 is
  not Gemma 4.

`ModelConfig` is the only picker the API has, and it has two axes, not one:

| | `ModelPreference.FULL` | `ModelPreference.FAST` |
|---|---|---|
| `ModelReleaseStage.STABLE` (the default) | the model the phone ships, nano-v3 here | feature 645, empty on the Magic V5 |
| `ModelReleaseStage.PREVIEW` | Gemma 4 E4B, developer preview | Gemma 4 E2B, developer preview |

The preview row is inferred: the announcement's snippet is `PREVIEW` plus `FULL`, and it says E4B
is for reasoning and E2B for speed ("3x faster than the E4B"), but never says which configuration
names which model. `getBaseModelName()` is the check. AICore tracks each cell it can serve as a
numbered feature, and which cells a device gets is decided per device by Google and the OEM;
Honor's build fills the stable full cell only. `Generation.getClient()` with no config returns that
cell, which is why there is no version picker beyond this table.

The preview row is gated by [enrolment](https://developers.google.com/ml-kit/genai/aicore-dev-preview),
not code: the testing account joins the aicore-experimental Google group, opts in as an AICore
tester on the Play Store, and takes an AICore beta update. Models come down over Wi-Fi only, the
first inference can take about a minute, and the quota bypass is Pixel-only. Whether Honor's AICore
takes the beta channel at all is unknown. `genai-prompt` 1.0.0-beta4, the version pinned here and
the newest on Google's Maven, already ships `ModelReleaseStage`, so nothing needs bumping. The
bench takes `pref=`; a `stage=` key is the one-line addition that would run all four cells.

What Gemma 4 claims: up to 4x the speed of the previous generation, 60% less battery, and better
OCR and handwriting. Against 22 tokens a second it is the one lever left. The verbatim rule still
decides: a smaller model is a worse copier, so E2B's extra 3x is worth having only if the
transcription survives, which is what the bench's logged text is there to catch.

## Where the time goes

Streaming answered it. With `stream=true` the first piece of text arrived **556 ms** after the call,
and the same 74 words took the same 5.2 s in total, in 44 pieces. A second probe said the same
thing from the other side: the full screenshot with a prompt asking for the single word "yes" came
back, as "yes", in **555 ms**. So:

- **Prefill is half a second.** Image and prompt together. This is the part every lever above
  targets, and there is nothing there to win.
- **Everything else is decoding.** 448 characters in about 4.7 s is roughly 90 characters a
  second, or 22 tokens a second. A request that returned 1001 characters took 11.5 s, which is the
  same rate. Time is a straight line through the length of the post, and the post cannot be made
  shorter without reading less of it.

That is the whole story of the ten seconds: a screenful of a longer post is 150 to 200 words, and
at 22 tokens a second that is 8 to 11 s.

Two side findings from the probes:

- A request with **no image** still produced 106 words of a plausible post in 7.2 s. Asked to copy
  out what is on a screen it was never shown, the model wrote one. The verbatim rule holds only
  when there is something to be verbatim about.
- The **foreground stage** (the see-through Activity AICore needs in front) costs 20 to 110 ms
  from launch to `onResume`. The concern that the Activity was part of the wait was wrong.

## Before any of it: the download nobody announces

None of this runs until the phone has the model, and getting it is its own small story (COS-259).
The first `checkStatus()` on the Magic V5 said `DOWNLOADABLE`, so the app asked for it with
`download()` and collected the flow the API returns for progress. It emitted nothing. Not a byte
count, not a completion, nothing at all, for as long as anyone watched it. Meanwhile Private
Compute Services was fetching the model in the background with no notification, no settings entry
that changes, no visible signal of any kind. About four minutes later a fresh `checkStatus()` said
`AVAILABLE`.

So readiness in the app is polled, not listened for: a status check when the model is first
constructed (which is "Start Deckard"), one more on every `isReady`, and a `DOWNLOADABLE` answer is
turned into a download request whose flow is collected only for the log. Status checks are allowed
from the background, so the polling costs nothing and needs no Activity. A phone that has never had
the model spends its first few minutes with Deckard saying he has no eyes yet, and then, without
anything having happened on screen, he does.

## The first read is different

A freshly started process pays extra, and the first summon is the one that sets the impression:

| | Fresh process | Warm |
|---|---|---|
| First AICore call (`countTokens` or `warmup`) | 2.3 to 3.6 s | 30 to 90 ms |
| First `generateContent` | 6.3 to 7.0 s | 5.0 to 5.5 s |

So the first read of a process is about 3 s of session setup plus 1.4 s more on its first generate,
and a warm-up call absorbs the 3 s but not the 1.4 s. In the real path there is no `countTokens`
before the read, so the first summon pays all of it at once: the 9.9 to 12.2 s COS-260 measured.

The session stays warm. Six minutes of nothing, then the next read was 5.05 s with a 90 ms
`countTokens` in front of it. A warm-up from the setup screen when Deckard is started (an Activity
in front, where AICore allows it) would pay the 3 s before the first summon instead of during it.
That is a follow-up, not done here.

## The summon, end to end

With timing lines on the real path (`foreground: in front after`, `nano: read … in`) one summon
over the same 74-word post reads as:

| Step | Time |
|---|---|
| The long-press itself | 1.2 s |
| Screenshot and stepping in front | 0.15 s |
| Nano read | 5.45 s |
| Pangram: `POST /task` | 0.18 s |
| Pangram: polling until `STAGE_SUCCESS` | 3.5 s |

Pangram's own inference took about 3.3 s (inference, then post-processing, then success), and the
app was asking every 1.5 s, so the verdict landed up to a second and a half after it was ready. The
poll is now every 500 ms with the same one-minute timeout. Roughly a second back, for free, from
the part of the wait that is not Nano at all.

## What changed in the repo

- `NanoBench` and `scripts/deckard nano-bench`: the bench stays, registered only in debug builds
  next to the shutter preview receiver.
- Timing lines on the real path, so a slow summon can be split into stage, model and oracle from
  `scripts/deckard log`.
- Pangram polled every 500 ms instead of 1500.

## What is left

- Warm up AICore from the setup screen when Deckard starts, for the first summon's 3 s (COS-264).
- Streaming would not make the read shorter, but the words start arriving at half a second. A
  bubble that fills as he reads is a different product from a card that lands after ten seconds
  of shutter effect. Not started.
- The rate itself belongs to AICore and the phone. A different device, or a different model, is
  the only thing that changes 22 tokens a second, and the Gemma 4 preview is the first candidate:
  enrol the Magic V5, add a `stage=` key to the bench, and run the four cells of the table above
  with the text compared word for word. If the phone never serves the preview, the same code is
  the Nano 4 path when that ships.
