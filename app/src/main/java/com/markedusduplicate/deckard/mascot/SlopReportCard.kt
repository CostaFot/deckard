package com.markedusduplicate.deckard.mascot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markedusduplicate.design.theme.AppTheme
import java.util.Locale
import kotlin.math.roundToInt

private const val CARD_WIDTH_DP = 300

/**
 * Deckard's verdict, printed like a marked-up document: the judgement lands as a stamp across the
 * top, the passage he judged sits underneath it, and the machine's own readings — composition,
 * word count, model version, confidence — run along the bottom in monospace.
 *
 * The stamp carries the whole verdict on its own, so the card still reads at thumbnail size.
 * Closed via the overlay's close control, not by tapping the card.
 */
@Composable
fun SlopReportCard(
    verdict: UiSlopVerdict,
    onViewAnalysis: (String) -> Unit,
    onCopyLink: (String) -> Unit,
) {
    val colors = deckardColors
    val stamp = colors.stamp(verdict.isAi)
    val percent = (maxOf(verdict.fractionAi, verdict.fractionAiAssisted, verdict.fractionHuman) * 100)
        .roundToInt()

    Surface(
        shape = SpeechBubbleShape(corner = 18.dp),
        color = colors.paper,
        contentColor = colors.ink,
        border = BorderStroke(1.dp, colors.hairline),
        shadowElevation = 8.dp,
        modifier = Modifier.widthIn(max = CARD_WIDTH_DP.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Stamp(label = verdict.dominantLabel, percent = percent, stamp = stamp)

            Excerpt(text = verdict.analyzedText, stamp = stamp, colors = colors)

            CompositionBar(verdict = verdict, colors = colors)

            Text(
                text = metaLine(verdict),
                style = MetaTextStyle,
                color = colors.inkMuted,
            )

            val link = verdict.dashboardLink
            if (!link.isNullOrBlank()) {
                Divider(colors = colors)
                Actions(
                    stamp = stamp,
                    colors = colors,
                    onViewAnalysis = { onViewAnalysis(link) },
                    onCopyLink = { onCopyLink(link) },
                )
            }
        }
    }
}

/**
 * The verdict as a rubber stamp: a hard ruled box, tilted a degree or two off the grid so it reads
 * as something pressed onto the page rather than another Material container.
 */
@Composable
private fun Stamp(label: String, percent: Int, stamp: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(-1.8f)
            .border(width = 2.dp, color = stamp, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = StampTextStyle,
            color = stamp,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "$percent%", style = StampNumberStyle, color = stamp)
    }
}

/** The passage under examination, ruled down the side the way a quoted excerpt is marked. */
@Composable
private fun Excerpt(text: String, stamp: Color, colors: DeckardColors) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(stamp.copy(alpha = 0.35f)),
        )
        Text(
            text = text,
            style = BodyTextStyle,
            color = colors.ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/**
 * How the text breaks down: machine, machine-assisted, human. A single ruled bar, because the three
 * shares are the one comparison worth making and they always sum to the whole.
 */
@Composable
private fun CompositionBar(verdict: UiSlopVerdict, colors: DeckardColors) {
    val segments = listOf(
        verdict.fractionAi.toFloat() to colors.stampAi,
        verdict.fractionAiAssisted.toFloat() to colors.stampAssisted,
        verdict.fractionHuman.toFloat() to colors.stampHuman,
    ).filter { it.first > 0f }

    if (segments.size < 2) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.hairline),
    ) {
        segments.forEach { (fraction, color) ->
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

@Composable
private fun Divider(colors: DeckardColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.hairline),
    )
}

@Composable
private fun Actions(
    stamp: Color,
    colors: DeckardColors,
    onViewAnalysis: () -> Unit,
    onCopyLink: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onViewAnalysis,
            shape = RoundedCornerShape(10.dp),
            color = stamp,
            contentColor = colors.paper,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "View full analysis",
                style = BodyTextStyle.copy(fontSize = 14.sp),
                modifier = Modifier.padding(vertical = 11.dp),
                textAlign = TextAlign.Center,
            )
        }
        Surface(
            onClick = onCopyLink,
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent,
            contentColor = colors.inkMuted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Copy link",
                style = BodyTextStyle.copy(fontSize = 13.sp),
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun metaLine(verdict: UiSlopVerdict): String = buildString {
    append("PANGRAM ${verdict.version}")
    append(" · ${verdict.wordCount} WORDS")
    if (verdict.confidence.isNotBlank()) {
        append(" · ${verdict.confidence.uppercase(Locale.ROOT)}")
    }
}

@Preview
@Composable
private fun SlopReportCardPreview() {
    AppTheme {
        SlopReportCard(
            verdict = UiSlopVerdict(
                isAi = true,
                aiLikelihood = 1.0,
                summary = "AI Generated",
                predictionShort = "AI",
                headline = "AI Generated",
                prediction = "We believe that this document is fully AI-generated",
                fractionAi = 1.0,
                fractionAiAssisted = 0.0,
                fractionHuman = 0.0,
                numAiSegments = 1,
                numAiAssistedSegments = 0,
                numHumanSegments = 0,
                dashboardLink = "https://www.pangram.com/history/abc",
                version = "3.3.2",
                wordCount = 137,
                analyzedText = "After the conclusion of Posidonia 2026, we continue to reflect on a " +
                        "highly productive and rewarding experience for the Department of Maritime Studies",
                confidence = "High",
                dominantLabel = "AI-Generated",
            ),
            onViewAnalysis = {},
            onCopyLink = {},
        )
    }
}
