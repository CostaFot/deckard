package com.markedusduplicate.deckard.accessibility.tree

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.markedusduplicate.deckard.accessibility.tree.ScreenNodeSnapshot.MAX_DEPTH
import com.markedusduplicate.deckard.accessibility.tree.ScreenNodeSnapshot.MAX_NODES

/**
 * Builds an immutable [ScreenNode] snapshot from a live `AccessibilityNodeInfo` tree. Done once, on
 * the accessibility service, so the rest of the screen-reading pipeline works on plain Kotlin. The
 * walk is capped ([MAX_NODES] / [MAX_DEPTH]) so a pathological tree can't blow up the read.
 */
object ScreenNodeSnapshot {

    private const val MAX_NODES = 4000
    private const val MAX_DEPTH = 60

    fun from(root: AccessibilityNodeInfo?): ScreenNode? {
        root ?: return null
        return build(root, depth = 0, remaining = intArrayOf(MAX_NODES))
    }

    private fun build(node: AccessibilityNodeInfo, depth: Int, remaining: IntArray): ScreenNode? {
        if (remaining[0] <= 0 || depth > MAX_DEPTH) return null
        remaining[0]--

        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val children = ArrayList<ScreenNode>(node.childCount)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            build(child, depth + 1, remaining)?.let(children::add)
        }

        return ScreenNode(
            className = node.className?.toString(),
            viewId = node.viewIdResourceName,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isVisibleToUser = node.isVisibleToUser,
            isPassword = node.isPassword,
            children = children,
        )
    }
}
