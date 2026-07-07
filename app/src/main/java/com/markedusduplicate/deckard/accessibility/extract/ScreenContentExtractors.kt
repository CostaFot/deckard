package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.tree.ScreenNode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches screen-text extraction to the right [ScreenContentExtractor] for the foreground app,
 * falling back to [GenericContentExtractor] when no per-app extractor claims the package. The
 * per-app extractors are contributed via Hilt `@IntoSet` (see
 * [com.markedusduplicate.deckard.di.ScreenTextExtractorsModule]), so adding an app is one binding.
 */
@Singleton
class ScreenContentExtractors @Inject constructor(
    private val extractors: Set<@JvmSuppressWildcards ScreenContentExtractor>,
    private val generic: GenericContentExtractor,
) {
    fun extract(packageName: String, root: ScreenNode): String? {
        val extractor = extractors.firstOrNull { it.handles(packageName) } ?: generic
        return extractor.extract(root)
    }
}
