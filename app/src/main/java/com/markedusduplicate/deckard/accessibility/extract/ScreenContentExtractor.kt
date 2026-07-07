package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.tree.ScreenNode

/**
 * Pulls the content worth checking for slop out of one app's accessibility tree. Each app the user
 * reads in (X, LinkedIn, Reddit, …) lays its tree out differently, so each gets its own extractor;
 * [ScreenContentExtractors] dispatches by foreground package name and falls back to
 * [GenericContentExtractor]. Implementations are pure functions over a [ScreenNode] snapshot, so they
 * unit-test against captured trees.
 */
interface ScreenContentExtractor {

    /** Whether this extractor handles the given foreground package (e.g. `com.linkedin.android`). */
    fun handles(packageName: String): Boolean

    /** The text to check, or null when there's nothing readable on screen. */
    fun extract(root: ScreenNode): String?
}
