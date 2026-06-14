package com.markedusduplicate.deckard.accessibility

import com.markedusduplicate.deckard.accessibility.tree.Bounds
import com.markedusduplicate.deckard.accessibility.tree.ScreenNode

/** Builds a [ScreenNode] with sensible defaults so fixtures only specify the fields under test. */
fun node(
    className: String? = null,
    viewId: String? = null,
    text: String? = null,
    contentDescription: String? = null,
    bounds: Bounds = Bounds(0, 0, 1080, 2000),
    isVisibleToUser: Boolean = true,
    isPassword: Boolean = false,
    children: List<ScreenNode> = emptyList(),
) = ScreenNode(
    className = className,
    viewId = viewId,
    text = text,
    contentDescription = contentDescription,
    bounds = bounds,
    isVisibleToUser = isVisibleToUser,
    isPassword = isPassword,
    children = children,
)
