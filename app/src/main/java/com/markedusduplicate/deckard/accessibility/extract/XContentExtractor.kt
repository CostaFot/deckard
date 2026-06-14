package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.tree.Bounds
import com.markedusduplicate.deckard.accessibility.tree.ScreenNode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extractor for the X / Twitter Android app (`com.twitter.android`) — returns the **most-centred
 * post**, the tweet the user is looking at.
 *
 * X is unusual: on the timeline a tweet exposes no per-element text nodes — the entire card (author,
 * `@handle`, "Verified", an optional "Replying to …", the body, an optional "Reposted by …", the
 * timestamp and the engagement counts) is concatenated into the card node's single
 * [ScreenNode.contentDescription] so a screen reader reads it as one unit. There is no child
 * `TextView` to read the body from, so we select the card the user has centred and parse the body out
 * of its description, stripping the byline and the trailing timestamp/metrics chrome. The detail
 * screen's focused tweet is one such card too, so the same path covers it.
 *
 * A tweet card is matched on **content, not class** (it's a `LinearLayout` on the timeline, a
 * `ViewGroup` on detail): any visible node whose description carries an `@handle` and an
 * engagement/time marker. Promoted cards ("Promoted.") are skipped. Selection follows what the user
 * centres: the card under the screen centre, falling back to the largest by viewport overlap.
 *
 * A **quote tweet** packs two posts into one description — the quoter's lead-in, then "Quoted."
 * followed by the embedded original, then the quoter's "Added {comment}". X embeds only a *truncated
 * preview* of the quoted original (it can cut off mid-sentence), so we judge the quoter's **own
 * comment** — the full text the viewed account wrote — falling back to the quoted preview only when
 * there is no added comment.
 *
 * Known gaps to iterate on: absolute timestamps on older tweets ("Jun 14") aren't stripped; a
 * display name containing a "." can defeat the byline strip.
 */
@Singleton
class XContentExtractor @Inject constructor() : ScreenContentExtractor {

    override fun handles(packageName: String): Boolean = packageName == X_PACKAGE

    override fun extract(root: ScreenNode): String? {
        val viewport = root.viewport()
        val cards = root.findAll { it.isTweetCard(viewport) }

        val card = cards
            .filter { it.bounds.contains(viewport.centerX, viewport.centerY) }
            .maxByOrNull { it.bounds.intersectionArea(viewport) }
            ?: cards.maxByOrNull { it.bounds.intersectionArea(viewport) }

        return card?.contentDescription
            ?.let(::bodyOf)
            ?.take(MAX_SCREEN_CHARS)
            ?.ifEmpty { null }
    }

    private fun ScreenNode.isTweetCard(viewport: Bounds): Boolean {
        val desc = contentDescription ?: return false
        return isVisibleToUser &&
                !isPassword &&
                bounds.intersects(viewport) &&
                !desc.contains(PROMOTED_MARKER) &&
                HANDLE.containsMatchIn(desc) &&
                ENGAGEMENT_OR_TIME.containsMatchIn(desc)
    }

    /**
     * Pull the body out of a card's content description. Drop zero-width marks and the trailing
     * engagement counts / timestamp / "Reposted by …" chrome, then either take the quoter's own
     * comment (quote tweet) or strip the leading "{name} @handle Verified." byline and an optional
     * "Replying to …". Finally drop any "pic.x.com/…" image link and the single "." X appends.
     */
    private fun bodyOf(description: String): String {
        val text = description
            .replace(ZERO_WIDTH, "")
            .replace(TRAILING_METRICS, "")
            .replace(TRAILING_TIMESTAMP, "")
            .replace(TRAILING_REPOST, "")

        val stripped = if (QUOTE_LEAD.containsMatchIn(text)) {
            QUOTER_COMMENT.find(text)?.groupValues?.get(1) ?: text.replace(QUOTE_LEAD, "")
        } else {
            text.replace(LEADING_BYLINE, "").replace(LEADING_REPLYING_TO, "")
        }

        return stripped
            .replace(PIC_LINK, "")
            .trim()
            .removeSuffix(".")
            .trim()
    }

    private companion object {
        const val X_PACKAGE = "com.twitter.android"
        const val PROMOTED_MARKER = "Promoted."

        val ZERO_WIDTH = Regex("[\\u200B-\\u200F\\uFEFF]")
        val HANDLE = Regex("@\\w+")
        val ENGAGEMENT_OR_TIME = Regex("\\b(?:ago|views?|likes?|repl(?:y|ies)|reposts?)\\b")

        /** The trailing run of "… 2 replies.  3 reposts.  34 likes.  2569 verified views." counts. */
        val TRAILING_METRICS = Regex(
            "(?:\\s*[\\d,]+\\s+(?:repl(?:y|ies)|reposts?|quotes?|likes?|bookmarks?|(?:verified\\s+)?views?)\\.)+\\s*$",
            RegexOption.IGNORE_CASE,
        )

        /** "… 2 hours ago." / "… 5 minutes ago." at the end (relative timestamps only). */
        val TRAILING_TIMESTAMP = Regex("\\s*\\d+\\s+\\w+\\s+ago\\.\\s*$", RegexOption.IGNORE_CASE)

        /** "… Reposted by molson 🧠⚙️." at the end. */
        val TRAILING_REPOST = Regex("\\s*Reposted by .*$")

        /** The byline at the start: "molson 🧠⚙️ @Molson_Hart Verified. " or, for an unverified
         *  account, "antirez @antirez. " — i.e. "{name} @handle" then "Verified." or just ".". */
        val LEADING_BYLINE = Regex("^.*?@\\w+\\b(?:\\s+Verified)?\\.\\s*")

        /** "Replying to @AlecTorelli. " right after the byline. */
        val LEADING_REPLYING_TO = Regex("^Replying to .*?\\.\\s*")

        /**
         * Marks a quote tweet: the lead-in through the quoted author's byline, "… Quoted. {quoted
         * byline}.  ". Used to detect a quote and, lacking an added comment, to strip the lead-in and
         * fall back to the quoted preview. Dot-matches-all so it spans the body's newlines.
         */
        val QUOTE_LEAD = Regex(
            "^.*?Quoted\\.\\s+[^.\\n]*?@\\w+\\b(?:\\s+Verified)?\\.\\s+",
            RegexOption.DOT_MATCHES_ALL,
        )

        /** Captures the quoter's own comment after the quoted post: "{quoter byline}.  Added {1}". */
        val QUOTER_COMMENT = Regex(
            "@\\w+\\b(?:\\s+Verified)?\\.\\s+Added\\s+(.+)$",
            RegexOption.DOT_MATCHES_ALL,
        )

        /** "pic.x.com/…" image-link artifact embedded in a quoted post. */
        val PIC_LINK = Regex("\\s*pic\\.(?:x|twitter)\\.com/\\S*")
    }
}
