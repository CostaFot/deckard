package com.markedusduplicate.deckard.mascot

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Deckard's own look, separate from the app-wide Material palette.
 *
 * The overlay draws on top of arbitrary apps, so it can't inherit its surroundings — it has to carry
 * its own contrast. The register is a marked-up document: paper ground, ink text, and a verdict
 * printed like a rubber stamp, with the machine's own readings (word count, model version,
 * confidence) set in monospace because that is what they are.
 */
@Immutable
data class DeckardColors(
    val paper: Color,
    val ink: Color,
    val inkMuted: Color,
    val hairline: Color,
    val stampAi: Color,
    val stampHuman: Color,
    val stampAssisted: Color,
) {
    /** The stamp ink for a verdict: red for machine, green for human. */
    fun stamp(isAi: Boolean): Color = if (isAi) stampAi else stampHuman
}

private val LightDeckardColors = DeckardColors(
    paper = Color(0xFFFAFAF7),
    ink = Color(0xFF14181F),
    inkMuted = Color(0xFF5A6472),
    hairline = Color(0xFFE3E4DF),
    stampAi = Color(0xFFD93A1E),
    stampHuman = Color(0xFF1B7A57),
    stampAssisted = Color(0xFFB5791A),
)

private val DarkDeckardColors = DeckardColors(
    paper = Color(0xFF15181D),
    ink = Color(0xFFECEEF0),
    inkMuted = Color(0xFF98A1AE),
    hairline = Color(0xFF272C34),
    stampAi = Color(0xFFFF6A4D),
    stampHuman = Color(0xFF35C08A),
    stampAssisted = Color(0xFFE0A33C),
)

val deckardColors: DeckardColors
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DarkDeckardColors else LightDeckardColors

/** Deckard's own name, and nothing else. */
val DisplayTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    letterSpacing = (-1.2).sp,
)

/** Headings inside the setup screen. */
val TitleTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
)

/** The verdict itself: heavy, tight and upper-case, so it reads as a mark rather than a sentence. */
val StampTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 19.sp,
    letterSpacing = (-0.4).sp,
)

/** The number that goes with the stamp. */
val StampNumberStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 30.sp,
    letterSpacing = (-1.2).sp,
)

/** Machine readings — word count, model version, confidence. Monospace because they are data. */
val MetaTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 1.1.sp,
)

/** The text Deckard actually judged, and anything he says in his own words. */
val BodyTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)

/**
 * A speech bubble with a tail pointing back at the mascot above it, so the two read as one object
 * instead of a floating box next to an emoji.
 */
class SpeechBubbleShape(
    private val corner: Dp = 14.dp,
    private val tailWidth: Dp = 16.dp,
    private val tailHeight: Dp = 8.dp,
    private val tailInset: Dp = 16.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val tail = with(density) { tailHeight.toPx() }
        val width = with(density) { tailWidth.toPx() }
        val inset = with(density) { tailInset.toPx() }
        val radius = with(density) { corner.toPx() }

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = tail,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(radius),
                ),
            )
            // Overlap the body by a pixel so the join doesn't hairline on some densities.
            moveTo(inset, tail + 1f)
            lineTo(inset + width / 2f, 0f)
            lineTo(inset + width, tail + 1f)
            close()
        }
        return Outline.Generic(path)
    }
}
