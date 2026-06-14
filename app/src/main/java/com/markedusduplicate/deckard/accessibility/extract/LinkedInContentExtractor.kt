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
 * The feed is Jetpack Compose ([FEED_CONTAINER_ID] = `sdui:lazyColumn`), so the tree is mostly bare
 * `android.view.View`s with no ids. The whole post body lands in a single full-width
 * `android.widget.TextView`; the author/headline are small indented TextViews and all app chrome
 * (search bar, bottom nav) sits *outside* the feed container. So within the feed we pick the visible
 * `TextView` with the largest viewport overlap — the body of the most-visible post — which drops
 * chrome and author noise without needing per-post container ids (which Compose doesn't expose).
 *
 * Known gaps to iterate on: author/timestamp aren't included; a collapsed "…more" post yields only
 * the shown text; post-detail screens are untested (the `?: root` fallback should still find the
 * body there).
 */
@Singleton
class LinkedInContentExtractor @Inject constructor() : ScreenContentExtractor {

    override fun handles(packageName: String): Boolean = packageName == LINKEDIN_PACKAGE

    override fun extract(root: ScreenNode): String? {
        val viewport = root.viewport()
        val feed = root.find { it.viewId == FEED_CONTAINER_ID } ?: root

        val postBody = feed
            .findAll { it.className == TEXT_VIEW && it.isReadable(viewport) }
            .maxByOrNull { it.bounds.intersectionArea(viewport) }

        return postBody?.text?.trim()?.ifEmpty { null }
            ?: collectVisibleText(listOf(feed), viewport).ifEmpty { null }
    }

    private fun ScreenNode.isReadable(viewport: Bounds) =
        isVisibleToUser && !isPassword && !text.isNullOrBlank() && bounds.intersects(viewport)

    private companion object {
        const val LINKEDIN_PACKAGE = "com.linkedin.android"
        const val FEED_CONTAINER_ID = "sdui:lazyColumn"
        const val TEXT_VIEW = "android.widget.TextView"
    }
}
