package com.costafotiadis.textresource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources

/**
 * Resolves this inside a composition, against the same [android.content.res.Resources] Compose's own
 * `stringResource()` reads.
 *
 * `LocalResources` reads `LocalConfiguration` to compute itself, so callers of this invalidate and
 * re-resolve when the configuration changes — a locale switch, a font-scale change. That is why it
 * is the local to read here and `LocalContext` is not: `LocalContext` is a `staticCompositionLocalOf`
 * and never invalidates its readers.
 */
@Composable
@ReadOnlyComposable
fun TextResource.asString(): String = asString(LocalResources.current)
