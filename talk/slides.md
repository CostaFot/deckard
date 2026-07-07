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
they paste it out of ChatGPT? I genuinely can't tell anymore — and it's everywhere. And I
got sick of it."
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
The pivot out of the cold open. Next: the machine at a glance, then we descend.
-->
