package com.markedusduplicate.deckard.accessibility.tree

/**
 * Renders a [ScreenNode] tree as an indented, human-readable string for the debug logcat dump (see
 * `DeckardAccessibilityService`). The output is what we eyeball to design per-app extractors and what
 * we paste into unit-test fixtures, so it surfaces the fields extraction keys on: class, view id,
 * text, content description, bounds, and visibility.
 */
fun ScreenNode.toDebugString(): String = StringBuilder().also { appendNode(it, this, 0) }.toString()

private fun appendNode(out: StringBuilder, node: ScreenNode, depth: Int) {
    repeat(depth) { out.append("  ") }
    out.append(node.className?.substringAfterLast('.') ?: "?")
    node.viewId?.let { out.append(" #").append(it.substringAfterLast('/')) }
    node.text?.takeIf { it.isNotBlank() }?.let { out.append(" \"").append(it).append('"') }
    node.contentDescription?.takeIf { it.isNotBlank() }
        ?.let { out.append(" desc=\"").append(it).append('"') }
    val b = node.bounds
    out.append(" [").append(b.left).append(',').append(b.top)
        .append('-').append(b.right).append(',').append(b.bottom).append(']')
    if (!node.isVisibleToUser) out.append(" [hidden]")
    out.append('\n')
    node.children.forEach { appendNode(out, it, depth + 1) }
}
