package com.markedusduplicate.deckard.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markedusduplicate.design.theme.AppTheme
import kotlin.math.roundToInt

private val SlopAiColor = Color(0xFFFF5A1F)
private val SlopHumanColor = Color(0xFF2E7D32)

private fun verdictColor(isAi: Boolean): Color = if (isAi) SlopAiColor else SlopHumanColor

/**
 * Deckard's verdict rendered as a faithful-core replica of Pangram's "short report" card: header
 * (icon + title + subtitle), an excerpt card, a circular composition gauge, the label/confidence
 * row, and — when a public dashboard link is available — the "View full analysis" / "Copy link"
 * actions. Closed via the overlay's X button, not by tapping the card.
 */
@Composable
fun SlopReportCard(
    verdict: UiSlopVerdict,
    onViewAnalysis: (String) -> Unit,
    onCopyLink: (String) -> Unit,
) {
    val accent = verdictColor(verdict.isAi)
    val percent = (maxOf(verdict.fractionAi, verdict.fractionAiAssisted, verdict.fractionHuman) * 100)
        .roundToInt()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 300.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(verdict = verdict, accent = accent)
            ExcerptCard(verdict = verdict)
            Gauge(percent = percent, label = verdict.dominantLabel, accent = accent)
            VerdictRow(verdict = verdict, accent = accent)

            val link = verdict.dashboardLink
            if (!link.isNullOrBlank()) {
                Button(onClick = { onViewAnalysis(link) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View full analysis")
                }
                TextButton(onClick = { onCopyLink(link) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Copy link to result")
                }
            }
        }
    }
}

@Composable
private fun Header(verdict: UiSlopVerdict, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🤖", fontSize = 20.sp)
        }
        Column {
            Text(
                text = verdict.headline,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = verdict.prediction,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExcerptCard(verdict: UiSlopVerdict) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = verdict.analyzedText,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${verdict.wordCount} words · Pangram ${verdict.version}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Gauge(percent: Int, label: String, accent: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = track,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accent,
                    startAngle = 135f,
                    sweepAngle = 270f * (percent / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$percent", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = accent)
                Text(
                    text = "%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Text(
            text = "of this text is $label",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VerdictRow(verdict: UiSlopVerdict, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = verdict.predictionShort, fontWeight = FontWeight.SemiBold, color = accent)
        if (verdict.confidence.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "Confidence ${verdict.confidence}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
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
