package com.costafotiadis.deckard.mascot

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costafotiadis.deckard.R
import com.costafotiadis.deckard.slop.ScreenReadFailure
import com.costafotiadis.design.theme.AppTheme
import com.costafotiadis.textresource.asString

private const val MASCOT_EMOJI = "🧙"
private const val PLATE_SIZE_DP = 52
private const val CLOSE_SIZE_DP = 22

/**
 * Deckard himself, on a plate.
 *
 * The overlay sits over arbitrary apps, so a bare emoji has no ground to stand on and disappears
 * against half the screens it lands on. The plate is drawn in the theme's ink (`onSurface`) —
 * near-black on a light device, near-white on a dark one — which puts him in contrast with his
 * surroundings either way, and the ring keeps his edge legible over busy content.
 *
 * The dismiss control rides on the plate's corner rather than floating over the content beside it,
 * so it sits in the same place whatever Deckard is currently showing.
 */
@Composable
fun DeckardMascot(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.size(PLATE_SIZE_DP.dp)) {
        DeckardPlate(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        onDrag(amount.x, amount.y)
                    }
                },
        )

        Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = colors.onSurface,
            contentColor = colors.surface,
            border = BorderStroke(1.5.dp, colors.surface.copy(alpha = 0.9f)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .size(CLOSE_SIZE_DP.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.mascot_dismiss),
                modifier = Modifier.padding(5.dp),
            )
        }
    }
}

/**
 * Deckard's face on its ink plate, with no behaviour attached — the mascot wears it in the overlay,
 * the setup screen wears it as the app's mark.
 */
@Composable
fun DeckardPlate(
    modifier: Modifier = Modifier,
    size: Dp = PLATE_SIZE_DP.dp,
    emojiSize: TextUnit = 26.sp,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colors.onSurface)
            .border(width = 1.5.dp, color = colors.surface.copy(alpha = 0.9f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = MASCOT_EMOJI, fontSize = emojiSize)
    }
}

/**
 * What Deckard says when he has no report to give — he's still looking, there wasn't enough to go
 * on, or he couldn't reach the detector. Tailed so it reads as coming from the mascot above it.
 */
@Composable
fun DeckardBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = SpeechBubbleShape(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = modifier.widthIn(max = 240.dp),
    ) {
        Text(
            text = text,
            style = BodyTextStyle,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
        )
    }
}

/**
 * The looking-at-your-screen state. The screenshot-OCR read takes seconds, so this needs to show
 * progress rather than a frozen "…".
 */
@Composable
fun DeckardThinkingBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = SpeechBubbleShape(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
        ) {
            Text(
                text = text,
                style = BodyTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PulsingDots()
        }
    }
}

@Composable
private fun PulsingDots() {
    val ink = MaterialTheme.colorScheme.onSurface
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(ink),
            )
        }
    }
}

/**
 * Everything Deckard says when he has no report — the two reads he can be part-way through, and
 * the seven ways he can come back with nothing. One preview per line, because a voice you can't
 * read side by side is a voice that drifts.
 */
@Preview(name = "Thinking")
@Composable
private fun DeckardThinkingPreview() {
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            ReadMethod.entries.forEach { how ->
                DeckardThinkingBubble(text = DeckardVoice.thinking(how).asString())
            }
        }
    }
}

@Preview(name = "Nothing to report")
@Composable
private fun DeckardSetbackPreview() {
    val setbacks = ScreenReadFailure.entries.map { NoVerdict.CouldNotRead(it) } +
        listOf(NoVerdict.NotEnoughText, NoVerdict.DetectorUnreachable)
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            setbacks.forEach { DeckardBubble(text = DeckardVoice.setback(it).asString()) }
        }
    }
}
