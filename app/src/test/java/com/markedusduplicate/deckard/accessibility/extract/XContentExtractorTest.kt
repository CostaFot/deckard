package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.node
import com.markedusduplicate.deckard.accessibility.tree.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures are hand-built from real `deckard_tree.txt` captures of X / Twitter. The defining trait:
 * the whole tweet lives in the card node's `contentDescription`; the body has no node of its own on
 * the timeline. Bounds use the captured 1280×2800 viewport (centre 640,1400).
 */
class XContentExtractorTest {

    private val extractor = XContentExtractor()

    private val linearLayout = "android.widget.LinearLayout"
    private val viewGroup = "android.view.ViewGroup"

    @Test
    fun `handles the X package only`() {
        assertTrue(extractor.handles("com.twitter.android"))
        assertFalse(extractor.handles("com.linkedin.android"))
    }

    /** A timeline post centred on screen: the body sits inside the card's desc, wrapped in the
     *  byline, a "Replying to …" clause (with zero-width marks around the handle), a "Reposted by …"
     *  attribution, the timestamp and the engagement counts — all of which are chrome to drop. */
    @Test
    fun `returns the centred post body, stripping byline, replying-to, reposted-by and metrics`() {
        val molson = "molson 🧠⚙️ @Molson_Hart Verified.   " +
                "Replying to ﻿‎@AlecTorelli﻿.  " +
                "reminds me this joke:\n\nAmerican is talking to guy from the USSR:\n\n" +
                "\"Man your propaganda is so bad!\"\n\nUSSR guy:\n\n" +
                "\"yes it's terrible, yours is much much better.\"\n\nAmerican:\n\n" +
                "\"What? What do you mean?\".       Reposted by molson 🧠⚙️.      " +
                "2 hours ago.  2 replies.  3 reposts.  34 likes.  2569 verified views. "
        val declaration = "Declaration of Memes @LibertyCappy Verified.    What meat is the best?." +
                "            1 hour ago.  129 replies.  15 reposts.  92 likes.  11138 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = molson, bounds = Bounds(0, 457, 1280, 2064)),
                node(className = linearLayout, contentDescription = declaration, bounds = Bounds(0, 2066, 1280, 2722)),
            ),
        )

        val expected = "reminds me this joke:\n\nAmerican is talking to guy from the USSR:\n\n" +
                "\"Man your propaganda is so bad!\"\n\nUSSR guy:\n\n" +
                "\"yes it's terrible, yours is much much better.\"\n\nAmerican:\n\n" +
                "\"What? What do you mean?\""
        assertEquals(expected, extractor.extract(root))
    }

    /** A longer real post, centred over a smaller neighbour: no "Replying to"/"Reposted by", and the
     *  body keeps its own paragraph breaks while the byline and trailing metrics are stripped. */
    @Test
    fun `keeps multi-paragraph body and prefers the centred post over a neighbour`() {
        val molson = "molson 🧠⚙️ @Molson_Hart Verified.   reminds me this joke." +
                "      2 hours ago.  2 replies.  3 reposts.  34 likes.  2602 verified views. "
        val battleByrd = "฿₳₮₮ⱠɆ ฿ɎⱤĐ @BattleByrd Verified.    " +
                "Been gone for a few days working on something big for the running community.\n\n" +
                "I'm proud after months of R&D testing we have finally completed our product " +
                "Carbodupa. (Patent Pending)\n\nPreorder today. \n\n" +
                "Don't wait cause this will sell out quick!." +
                "            6 hours ago.  232 replies.  184 reposts.  1974 likes.  513594 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = molson, bounds = Bounds(0, 311, 1280, 877)),
                node(className = linearLayout, contentDescription = battleByrd, bounds = Bounds(0, 879, 1280, 2722)),
            ),
        )

        val expected = "Been gone for a few days working on something big for the running community.\n\n" +
                "I'm proud after months of R&D testing we have finally completed our product " +
                "Carbodupa. (Patent Pending)\n\nPreorder today. \n\n" +
                "Don't wait cause this will sell out quick!"
        assertEquals(expected, extractor.extract(root))
    }

    /** A promoted post sits under the screen centre, with a real post above it. Ads carry "Promoted."
     *  in their desc and must be skipped, so the real post wins on viewport overlap. */
    @Test
    fun `skips a promoted post under the centre and falls back to the real post`() {
        val zarathustra = "Zarathustra @zarathustra5150 Verified.    " +
                "A tech billionaire doxxing a literal teenager and trying to ruin his life is " +
                "utterly psychotic behavior.." +
                "            2 hours ago.  179 replies.  654 reposts.  7922 likes.  154721 verified views. "
        val grokAd = "Grok @grok Verified Business xAI.    One subscription for smarter research, " +
                "image generation, and Grok's most advanced AI models. Get 3 months of SuperGrok for " +
                "just \$30.\n\nAvailable for new subscribers only..  Image. Grok - AI Chat & Video  ." +
                "          Promoted.  11 hours ago.  4 replies.  4 reposts.  39 likes.  543212 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = zarathustra, bounds = Bounds(0, 507, 1280, 963)),
                node(className = linearLayout, contentDescription = grokAd, bounds = Bounds(0, 965, 1280, 2722)),
            ),
        )

        val expected = "A tech billionaire doxxing a literal teenager and trying to ruin his life " +
                "is utterly psychotic behavior."
        assertEquals(expected, extractor.extract(root))
    }

    /** The tweet-detail screen: the focused tweet is the centred card (a `ViewGroup`, its desc has no
     *  trailing metrics — just the timestamp). A reply card sits below it and must not be chosen. */
    @Test
    fun `on the detail screen returns the focused tweet, not a reply below it`() {
        val focused = "Zarathustra @zarathustra5150 Verified.    " +
                "A tech billionaire doxxing a literal teenager and trying to ruin his life is " +
                "utterly psychotic behavior..            2 hours ago.  "
        val reply = "Illimitable Man (IM) @SovereignIM Verified.   " +
                "Replying to ﻿‎@zarathustra5150﻿.  " +
                "I am about 95% confident these are the eyes of a genetic psychopath..            " +
                "1 hour ago.  9 replies.  2 reposts.  180 likes.  9912 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = viewGroup, contentDescription = focused, bounds = Bounds(0, 337, 1280, 1440)),
                node(className = linearLayout, contentDescription = reply, bounds = Bounds(0, 1610, 1280, 2544)),
            ),
        )

        val expected = "A tech billionaire doxxing a literal teenager and trying to ruin his life " +
                "is utterly psychotic behavior."
        assertEquals(expected, extractor.extract(root))
    }

    /** An unverified account has no "Verified." marker — its byline is "{name} @handle." — so the
     *  author line must still be stripped off the front of the body. */
    @Test
    fun `strips an unverified byline that has no Verified marker`() {
        val antirez = "antirez @antirez.    " +
                "If you need AI to do a search for you in the real world, ds4-agent is basically SOTA, " +
                "because it can access the web sites without any limitations given that it uses your " +
                "local Chrome browser (no, not in headless mode, that's the trick...), and DeepSeek v4 " +
                "is great at search.." +
                "            3 hours ago.  20 replies.  28 reposts.  651 likes.  41165 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = antirez, bounds = Bounds(0, 1134, 1280, 2589)),
            ),
        )

        val expected = "If you need AI to do a search for you in the real world, ds4-agent is " +
                "basically SOTA, because it can access the web sites without any limitations given that " +
                "it uses your local Chrome browser (no, not in headless mode, that's the trick...), and " +
                "DeepSeek v4 is great at search."
        assertEquals(expected, extractor.extract(root))
    }

    /** A quote tweet packs the quoter's lead-in, the embedded "Quoted." preview, and the quoter's
     *  "Added" comment into one desc. X only previews the quoted post, so we judge the quoter's own
     *  comment — and the centred quote tweet is chosen over the reply below it. */
    @Test
    fun `on a quote tweet returns the quoter's own comment, not the quoted preview`() {
        val quote = "Anthony @anthonybuitran Verified.    " +
                "Quoted. molson 🧠⚙️ @Molson_Hart Verified.  " +
                "Get ready for inflation in physical products.\n\n" +
                "Product prices are up 3.9% in China, the USD is down 5.8%, and container shipping is " +
                "up ~40%.\n\nThis affects all products not just those with a “made in China” tag.\n\n" +
                "Why? Because almost everything uses made in China components. pic.x.com/z46eqE6Z9G.    " +
                "Anthony @anthonybuitran Verified.  Added Been seeing some of my suppliers increase " +
                "price quotes over the past couple of weeks." +
                "          1 hour ago.  2 replies.   6 likes.  1089 verified views. "
        val reply = "molson 🧠⚙️ @Molson_Hart Verified.   Replying to ﻿‎@anthonybuitran﻿.  " +
                "yes, same.            1 hour ago.    1 like.  551 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = quote, bounds = Bounds(0, 636, 1280, 2248)),
                node(className = linearLayout, contentDescription = reply, bounds = Bounds(0, 2248, 1280, 2544)),
            ),
        )

        val expected = "Been seeing some of my suppliers increase price quotes over the past " +
                "couple of weeks"
        assertEquals(expected, extractor.extract(root))
    }

    /** X truncates the quoted preview mid-sentence ("…head-to-head against"), so judging the quoted
     *  post would return a fragment. The quoter's own multi-paragraph comment is returned in full. */
    @Test
    fun `prefers the quoter comment when the quoted preview is truncated`() {
        val quote = "Sonny @sonnylazuardi.    " +
                "Quoted. Jamieson O'Reilly @theonejvo Verified.  " +
                "Did I just unlock claude-fable-5-lite? 😂\n\n" +
                "Since Fable 5 got pulled (US export control order, Anthropic is contesting it), I " +
                "wanted to see how much of its character lives in the system prompt vs. the model " +
                "itself.\n\nI ran the leaked Fable 5 prompt on Opus 4.8 head-to-head against " +
                "pic.x.com/UFRa2AoBMC.    Sonny @sonnylazuardi.  Added just tried fable 5 lite system " +
                "prompt with kimi k2.7 model\n\ni'm using opencode go, the result is not bad." +
                "          9 hours ago.  7 replies.  8 reposts.  287 likes.  58711 verified views. "

        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = quote, bounds = Bounds(0, 508, 1280, 2151)),
            ),
        )

        val expected = "just tried fable 5 lite system prompt with kimi k2.7 model\n\n" +
                "i'm using opencode go, the result is not bad"
        assertEquals(expected, extractor.extract(root))
    }

    @Test
    fun `returns null when no tweet card is on screen`() {
        val root = node(
            bounds = Bounds(0, 0, 1280, 2800),
            children = listOf(
                node(className = linearLayout, contentDescription = "For you", bounds = Bounds(0, 141, 374, 309)),
                node(className = linearLayout, contentDescription = "Home. New items", bounds = Bounds(0, 2722, 256, 2800)),
            ),
        )

        assertNull(extractor.extract(root))
    }
}
