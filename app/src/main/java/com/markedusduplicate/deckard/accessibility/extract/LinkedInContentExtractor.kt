package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.extract.LinkedInContentExtractor.Companion.FEED_CONTAINER_ID
import com.markedusduplicate.deckard.accessibility.tree.Bounds
import com.markedusduplicate.deckard.accessibility.tree.ScreenNode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extractor for the LinkedIn Android app — returns the **most-visible post**, the one the user is
 * looking at.
 *
 * The feed is Jetpack Compose ([FEED_CONTAINER_ID] = `sdui:lazyColumn`), mostly bare
 * `android.view.View`s with no ids. A post's whole body lands in one large node — sometimes a
 * `TextView`, sometimes a clickable `Button` — so we match on **content, not class**: a visible node
 * that carries text. When a post is collapsed the visible `text` ends in "… more" but the full post
 * is kept in `contentDescription`, so we read the fuller of the two. Chrome (search bar, bottom nav)
 * sits *outside* the feed container and the author/headline nodes are smaller, so the body wins.
 *
 * Selection follows what the user centers: prefer the content node under the screen centre, falling
 * back to the largest by viewport overlap (so a big neighbouring post can't steal a centred one).
 *
 * Known gaps to iterate on: author/timestamp aren't included; post-detail and comment screens are
 * untested (the `?: root` fallback should still find the body there).
 */
@Singleton
class LinkedInContentExtractor @Inject constructor() : ScreenContentExtractor {

    override fun handles(packageName: String): Boolean = packageName == LINKEDIN_PACKAGE

    override fun extract(root: ScreenNode): String? {
        val viewport = root.viewport()
        val feed = root.find { it.viewId == FEED_CONTAINER_ID } ?: root
        val candidates = feed.findAll { it.isReadableContent(viewport) }

        val post = candidates
            .filter { it.bounds.contains(viewport.centerX, viewport.centerY) }
            .maxByOrNull { it.bounds.intersectionArea(viewport) }
            ?: candidates.maxByOrNull { it.bounds.intersectionArea(viewport) }

        return post?.bestText()?.ifEmpty { null }
            ?: collectVisibleText(listOf(feed), viewport).ifEmpty { null }
    }

    private fun ScreenNode.isReadableContent(viewport: Bounds): Boolean =
        isVisibleToUser && !isPassword && bounds.intersects(viewport) && bestText().isNotEmpty()

    /**
     * The fuller of [ScreenNode.text] and [ScreenNode.contentDescription]: LinkedIn truncates the
     * visible post text with "… more" but keeps the whole post in the content description.
     */
    private fun ScreenNode.bestText(): String {
        val text = text?.trim().orEmpty()
        val description = contentDescription?.trim().orEmpty()
        return if (description.length > text.length) description else text
    }

    private companion object {
        const val LINKEDIN_PACKAGE = "com.linkedin.android"
        const val FEED_CONTAINER_ID = "sdui:lazyColumn"
    }
}
