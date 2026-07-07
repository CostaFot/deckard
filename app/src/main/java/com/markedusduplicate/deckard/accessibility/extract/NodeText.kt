package com.markedusduplicate.deckard.accessibility.extract

import com.markedusduplicate.deckard.accessibility.tree.Bounds
import com.markedusduplicate.deckard.accessibility.tree.ScreenNode

/** Cap on extracted characters — Pangram doesn't need more than a few paragraphs to judge. */
const val MAX_SCREEN_CHARS = 2000

/** Cap on collected lines, so a huge tree can't produce an unbounded read. */
private const val MAX_LINES = 200

/**
 * The on-screen viewport: the root window's bounds. Off-screen scrollback (feed items scrolled out
 * of view) has bounds outside this rect even when `isVisibleToUser` wrongly reports them visible.
 */
fun ScreenNode.viewport(): Bounds = bounds

/**
 * Visible `text` under [roots], clipped to [viewport], de-duplicated, in DFS (reading) order, joined
 * by newlines and capped at [MAX_SCREEN_CHARS]. The shared core every extractor builds on.
 */
fun collectVisibleText(roots: List<ScreenNode>, viewport: Bounds): String {
    val lines = LinkedHashSet<String>()
    roots.forEach { collectInto(it, viewport, lines) }
    return lines.joinToString("\n").take(MAX_SCREEN_CHARS)
}

private fun collectInto(node: ScreenNode, viewport: Bounds, out: LinkedHashSet<String>) {
    if (out.size >= MAX_LINES) return
    if (node.isVisibleToUser && !node.isPassword && node.bounds.intersects(viewport)) {
        node.text?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
    }
    node.children.forEach { collectInto(it, viewport, out) }
}

/** The first node (this subtree, pre-order) matching [predicate], or null. */
fun ScreenNode.find(predicate: (ScreenNode) -> Boolean): ScreenNode? {
    if (predicate(this)) return this
    for (child in children) child.find(predicate)?.let { return it }
    return null
}

/** All nodes in this subtree matching [predicate], in pre-order. */
fun ScreenNode.findAll(predicate: (ScreenNode) -> Boolean): List<ScreenNode> {
    val out = ArrayList<ScreenNode>()
    fun walk(node: ScreenNode) {
        if (predicate(node)) out.add(node)
        node.children.forEach(::walk)
    }
    walk(this)
    return out
}

/** All nodes with [className], not descending into a match (so nested duplicates aren't collected). */
fun ScreenNode.findTopmostByClassName(className: String): List<ScreenNode> {
    val out = ArrayList<ScreenNode>()
    fun walk(node: ScreenNode) {
        if (node.className == className) {
            out.add(node)
            return
        }
        node.children.forEach(::walk)
    }
    walk(this)
    return out
}

/** All nodes whose [viewId] ends with `:id/$idSuffix` (version-stable way to match an app's view). */
fun ScreenNode.findByViewIdSuffix(idSuffix: String): List<ScreenNode> {
    val needle = ":id/$idSuffix"
    val out = ArrayList<ScreenNode>()
    fun walk(node: ScreenNode) {
        if (node.viewId?.endsWith(needle) == true) out.add(node)
        node.children.forEach(::walk)
    }
    walk(this)
    return out
}

/** The candidate with the largest overlap with [viewport] — i.e. the one the user is looking at. */
fun mostVisible(candidates: List<ScreenNode>, viewport: Bounds): ScreenNode? =
    candidates
        .maxByOrNull { it.bounds.intersectionArea(viewport) }
        ?.takeIf { it.bounds.intersectionArea(viewport) > 0 }
