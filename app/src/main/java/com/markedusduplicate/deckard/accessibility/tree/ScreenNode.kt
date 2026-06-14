package com.markedusduplicate.deckard.accessibility.tree

/**
 * An immutable, framework-free snapshot of an `AccessibilityNodeInfo`. Per-app content extractors
 * ([com.markedusduplicate.deckard.accessibility.extract.ScreenContentExtractor]) operate on this
 * instead of the live tree so they stay pure Kotlin and unit-testable (the project has no
 * Robolectric/mockk). Built from the live tree by
 * [com.markedusduplicate.deckard.accessibility.tree.ScreenNodeSnapshot].
 */
data class ScreenNode(
    val className: String?,
    /** `viewIdResourceName`, e.g. `com.linkedin.android:id/feed_...`. */
    val viewId: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: Bounds,
    val isVisibleToUser: Boolean,
    val isPassword: Boolean,
    val children: List<ScreenNode>,
)

/**
 * On-screen rectangle (from `getBoundsInScreen`), in pixels. A plain data class rather than
 * `android.graphics.Rect` so extractor logic runs on the JVM in unit tests.
 */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    val isEmpty: Boolean get() = left >= right || top >= bottom

    fun intersects(other: Bounds): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    /** Area (px²) of the overlap with [other]; 0 when they don't overlap. Used to pick the
     *  most-visible node within the viewport. */
    fun intersectionArea(other: Bounds): Long {
        val width = (minOf(right, other.right) - maxOf(left, other.left)).coerceAtLeast(0)
        val height = (minOf(bottom, other.bottom) - maxOf(top, other.top)).coerceAtLeast(0)
        return width.toLong() * height.toLong()
    }
}
