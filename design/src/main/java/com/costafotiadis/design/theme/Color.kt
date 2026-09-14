package com.costafotiadis.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * Deckard's palette: a marked-up document. Paper ground, ink text, and a verdict printed like a
 * rubber stamp.
 *
 * Ink and paper invert with the theme, which is what makes the overlay work: the mascot's plate is
 * drawn in ink, so it is near-black over a light app and near-white over a dark one, in contrast
 * either way. There are no accent roles here — everything structural is ink on paper, and the only
 * colour in the product is the stamp.
 */

internal val PaperLight = Color(0xFFFAFAF7)
internal val InkLight = Color(0xFF14181F)
internal val InkMutedLight = Color(0xFF5A6472)
internal val HairlineLight = Color(0xFFE3E4DF)

internal val PaperDark = Color(0xFF15181D)
internal val InkDark = Color(0xFFECEEF0)
internal val InkMutedDark = Color(0xFF98A1AE)
internal val HairlineDark = Color(0xFF272C34)

/**
 * The three inks a verdict can be stamped in.
 *
 * Material gives you one [error][androidx.compose.material3.ColorScheme.error] role and no success
 * or warning counterpart, so a three-way judgement has nowhere honest to sit in the scheme —
 * borrowing `error`/`tertiary`/`secondary` for it would be a naming lie. They ride alongside the
 * scheme instead, provided by [AppTheme] off the same `useDarkTheme` so they can never disagree
 * with it.
 */
@Immutable
data class StampInks(
    val ai: Color,
    val assisted: Color,
    val human: Color,
)

internal val LightStampInks = StampInks(
    ai = Color(0xFFD93A1E),
    assisted = Color(0xFFB5791A),
    human = Color(0xFF1B7A57),
)

internal val DarkStampInks = StampInks(
    ai = Color(0xFFFF6A4D),
    assisted = Color(0xFFE0A33C),
    human = Color(0xFF35C08A),
)
