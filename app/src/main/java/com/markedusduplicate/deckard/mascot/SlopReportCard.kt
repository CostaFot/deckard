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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markedusduplicate.deckard.slop.SlopLabel
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.design.theme.stampInks
import java.util.Locale
import kotlin.math.roundToInt

private const val CARD_WIDTH_DP = 300

/**
 * Deckard's verdict, printed like a marked-up document: the judgement lands as a stamp across the
 * top, his [note] sits in the margin under it, the passage he judged follows, and the machine's own
 * readings — composition, word count, model version, confidence — run along the bottom in monospace.
 *
 * The stamp carries the whole verdict on its own, so the card still reads at thumbnail size.
 * Closed via the overlay's close control, not by tapping the card.
 *
 * [note] arrives as a string rather than being looked up here, so the card stays ignorant of what
 * Deckard sounds like and a preview can drive it.
 */
@Composable
fun SlopReportCard(
    verdict: UiSlopVerdict,
    note: String,
    onViewAnalysis: (String) -> Unit,
    onCopyLink: (String) -> Unit,
) {
    val stamp = MaterialTheme.stampInks.forLabel(verdict.label)
    val percentHuman = (verdict.fractionHuman * 100).roundToInt()

    Surface(
        shape = SpeechBubbleShape(corner = 18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
        modifier = Modifier.widthIn(max = CARD_WIDTH_DP.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Stamp(
                    label = verdict.headline.ifBlank { verdict.label.stampText },
                    percentHuman = percentHuman,
                    stamp = stamp,
                )
                MarginNote(text = note)
            }

            Excerpt(text = verdict.analyzedText, stamp = stamp)

            CompositionBar(verdict = verdict)

            Text(
                text = metaLine(verdict),
                style = MetaTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val link = verdict.dashboardLink
            if (!link.isNullOrBlank()) {
                HorizontalDivider()
                Actions(
                    stamp = stamp,
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
 *
 * The detector's own phrase, over the human share — the one number that means the same thing under
 * every label, so the word and the figure can never contradict each other. It is the pairing the
 * site's own Pangram badge uses.
 */
@Composable
private fun Stamp(label: String, percentHuman: Int, stamp: Color) {
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
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "$percentHuman%", style = StampNumberStyle, color = stamp)
            Text(text = "HUMAN", style = MetaTextStyle, color = stamp)
        }
    }
}

/**
 * What Deckard makes of it, written in the margin under the stamp.
 *
 * Muted rather than stamp-inked, so it reads as a hand annotating the page instead of a second
 * verdict arguing with the first; sans rather than monospace, because monospace on this card means
 * the machine's readings and this is the one line that isn't data.
 */
@Composable
private fun MarginNote(text: String) {
    Text(
        text = "— $text",
        style = BodyTextStyle.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The passage under examination, ruled down the side the way a quoted excerpt is marked. */
@Composable
private fun Excerpt(text: String, stamp: Color) {
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
private fun CompositionBar(verdict: UiSlopVerdict) {
    val inks = MaterialTheme.stampInks
    val segments = listOf(
        verdict.fractionAi.toFloat() to inks.ai,
        verdict.fractionAiAssisted.toFloat() to inks.assisted,
        verdict.fractionHuman.toFloat() to inks.human,
    ).filter { it.first > 0f }

    if (segments.size < 2) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
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
private fun Actions(
    stamp: Color,
    onViewAnalysis: () -> Unit,
    onCopyLink: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onViewAnalysis,
            shape = RoundedCornerShape(10.dp),
            color = stamp,
            // Filled in the stamp's ink, lettered in the paper it was pressed onto.
            contentColor = MaterialTheme.colorScheme.surface,
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
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * Every verdict the card can show, with the line Deckard writes under each. This is where the voice
 * is read and reviewed — the alternative is three installs and three Pangram calls.
 */
@Preview(name = "Verdict — AI")
@Composable
private fun SlopReportCardAiPreview() = CardPreview(SlopLabel.AI)

@Preview(name = "Verdict — assisted")
@Composable
private fun SlopReportCardAssistedPreview() = CardPreview(SlopLabel.ASSISTED)

@Preview(name = "Verdict — human")
@Composable
private fun SlopReportCardHumanPreview() = CardPreview(SlopLabel.HUMAN)

@Composable
private fun CardPreview(label: SlopLabel) {
    val verdict = previewVerdict(label)
    AppTheme {
        SlopReportCard(
            verdict = verdict,
            note = DeckardVoice.remark(verdict),
            onViewAnalysis = {},
            onCopyLink = {},
        )
    }
}

private fun previewVerdict(label: SlopLabel): UiSlopVerdict {
    val (ai, assisted, human) = when (label) {
        SlopLabel.AI -> Triple(0.92, 0.08, 0.0)
        SlopLabel.ASSISTED -> Triple(0.21, 0.54, 0.25)
        SlopLabel.HUMAN -> Triple(0.0, 0.06, 0.94)
    }
    return UiSlopVerdict(
        label = label,
        summary = label.stampText,
        predictionShort = label.name,
        headline = label.stampText,
        prediction = "",
        fractionAi = ai,
        fractionAiAssisted = assisted,
        fractionHuman = human,
        numAiSegments = 1,
        numAiAssistedSegments = 1,
        numHumanSegments = 1,
        dashboardLink = "https://www.pangram.com/history/abc",
        version = "4.0",
        wordCount = 137,
        analyzedText = "After the conclusion of Posidonia 2026, we continue to reflect on a " +
            "highly productive and rewarding experience for the Department of Maritime Studies",
        confidence = "High",
    )
}
