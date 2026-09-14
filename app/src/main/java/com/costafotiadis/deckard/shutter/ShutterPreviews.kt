package com.costafotiadis.deckard.shutter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.costafotiadis.deckard.mascot.MetaTextStyle
import com.costafotiadis.design.theme.AppTheme
import com.costafotiadis.textresource.asString

/**
 * Each effect stopped at four moments: the shutter half-closed, two points of the wait a half-lap
 * apart, and the release half-played.
 *
 * A preview cannot draw over another app and cannot move, which is why these are fixed frames rather
 * than a running [ShutterSurface] — the shapes and the weights are what is readable here, and they
 * are readable side by side. The motion is judged on the setup screen, and the effect over a real
 * app is judged over a real app.
 */
private val FRAMES = listOf(
    ShutterFrame(ShutterPhase.Snap, progress = 0.5f, loop = 0.1f, scale = PREVIEW_SCALE),
    ShutterFrame(ShutterPhase.Working, progress = 0f, loop = 0.25f, scale = PREVIEW_SCALE),
    ShutterFrame(ShutterPhase.Working, progress = 0f, loop = 0.75f, scale = PREVIEW_SCALE),
    ShutterFrame(ShutterPhase.Release, progress = 0.5f, loop = 0.9f, scale = PREVIEW_SCALE),
)

@Preview(name = "Shutter — crop marks")
@Composable
private fun CropMarksPreview() = Filmstrip(ShutterEffect.CropMarks)

@Preview(name = "Shutter — edge bloom")
@Composable
private fun EdgeBloomPreview() = Filmstrip(ShutterEffect.Bloom)

@Preview(name = "Shutter — travelling highlight")
@Composable
private fun TravellingHighlightPreview() = Filmstrip(ShutterEffect.Highlight)

@Preview(name = "Shutter — stamp")
@Composable
private fun ScreenStampPreview() = Filmstrip(ShutterEffect.Stamp)

@Composable
private fun Filmstrip(effect: ShutterEffect) {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = effect.label.asString(),
                style = MetaTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FRAMES.forEach { frame -> Pane(effect = effect, frame = frame) }
            }
        }
    }
}

/** A stand-in for the display: paper, a hairline round it, and the effect drawn on top. */
@Composable
private fun Pane(effect: ShutterEffect, frame: ShutterFrame) {
    val ink = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .width(78.dp)
            .height(150.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .drawBehind { with(effect.painter) { paint(frame, ink) } },
    )
}

/** A 78dp-wide stand-in for a phone, so every dp a painter measures shrinks to match. */
private const val PREVIEW_SCALE = 0.24f
