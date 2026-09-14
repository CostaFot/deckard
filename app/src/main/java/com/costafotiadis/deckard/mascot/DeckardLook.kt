package com.costafotiadis.deckard.mascot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
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
 * Deckard's own type and shapes.
 *
 * The register is a marked-up document — the palette that goes with it lives in the theme
 * ([com.costafotiadis.design.theme.AppTheme]). Here it is the lettering: a heavy, tight,
 * upper-case sans for the stamp, plain sans for what he says, and monospace for the machine's own
 * readings (word count, model version, confidence), because that is what they are.
 */

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
