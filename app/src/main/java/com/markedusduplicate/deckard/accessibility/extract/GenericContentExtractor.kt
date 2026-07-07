package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.tree.ScreenNode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback extractor for any app without a dedicated one. A viewport-clipped walk of the whole tree
 * (or, when present, the `WebView` subtrees only, so a browser's chrome drops out). Good enough to
 * not come up empty on an unknown app; per-app extractors exist to do better than this.
 */
@Singleton
class GenericContentExtractor @Inject constructor() : ScreenContentExtractor {

    override fun handles(packageName: String): Boolean = true

    override fun extract(root: ScreenNode): String? {
        val webViews = root.findTopmostByClassName(WEB_VIEW_CLASS)
        val roots = webViews.ifEmpty { listOf(root) }
        return collectVisibleText(roots, root.viewport()).ifEmpty { null }
    }

    private companion object {
        const val WEB_VIEW_CLASS = "android.webkit.WebView"
    }
}
