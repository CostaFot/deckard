package com.costafotiadis.deckard.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costafotiadis.deckard.R
import com.costafotiadis.deckard.mascot.BodyTextStyle
import com.costafotiadis.deckard.mascot.DisplayTextStyle
import com.costafotiadis.deckard.mascot.TitleTextStyle
import com.costafotiadis.deckard.shutter.ShutterEffect
import com.costafotiadis.deckard.shutter.ShutterLoop
import com.costafotiadis.deckard.shutter.ShutterSurface
import com.costafotiadis.deckard.ui.component.DeckardPage
import com.costafotiadis.deckard.ui.component.SectionLabel
import com.costafotiadis.textresource.asString
import kotlinx.coroutines.delay

/**
 * The things about Deckard you get to decide. One section today — how he announces the photograph
 * — and it is a list of sections so that the second one costs a section and nothing else.
 *
 * It is its own destination rather than the foot of the setup screen because the two are different
 * kinds of screen: setup is a checklist you finish once, this is a preference you come back to.
 */
@Composable
internal fun SettingsScreen(
    shutter: ShutterEffect,
    onPickShutter: (ShutterEffect) -> Unit,
    onBack: () -> Unit,
) {
    var playing by remember { mutableStateOf<ShutterEffect?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            DeckardPage {
                Heading(onBack = onBack)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(text = stringResource(R.string.settings_section_the_shutter))
                    SectionNote(text = stringResource(R.string.settings_shutter_detail))
                    ShutterEffect.entries.forEach { effect ->
                        ShutterRow(
                            effect = effect,
                            selected = effect == shutter,
                            onClick = {
                                onPickShutter(effect)
                                playing = effect
                            },
                        )
                    }
                }

                // Not a choice, a notice: the terms of the Prompt API want the user told that Google
                // receives metrics when the phone's model reads, and this is the page a user comes to
                // for how he works.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(text = stringResource(R.string.settings_section_his_eyes))
                    SectionNote(text = stringResource(R.string.settings_eyes_detail))
                }
            }

            // Played at the size it is really drawn at: no model, no Pangram call, no other app.
            playing?.let { effect ->
                var running by remember(effect) { mutableStateOf(true) }
                LaunchedEffect(effect) {
                    delay(PREVIEW_MILLIS)
                    running = false
                }
                ShutterSurface(
                    effect = effect,
                    running = running,
                    modifier = Modifier.matchParentSize(),
                    onFinished = { playing = null },
                )
            }
        }
    }
}

/**
 * The way back, and the screen's name under it.
 *
 * The title sits on its own line rather than beside the control: at [DisplayTextStyle] it is wider
 * than what is left of the row, and a title that runs past the page's gutter is worse than one that
 * wraps. On its own line the column constrains it, so a long name or a large font scale wraps
 * instead of overflowing.
 */
@Composable
private fun Heading(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val back = stringResource(R.string.settings_back)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            contentColor = colors.onSurface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = back },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "←", style = TitleTextStyle)
            }
        }
        Text(text = stringResource(R.string.settings_title), style = DisplayTextStyle)
    }
}

/** The paragraph under a section's label, in the quieter ink. */
@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        style = BodyTextStyle.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * One effect to choose from, with the effect itself running in the box beside its name. The
 * miniature is not a picture of the variant, it is the variant — the same painter, told it is being
 * drawn small — which is the only honest way to pick between four things that only exist in motion.
 */
@Composable
private fun ShutterRow(
    effect: ShutterEffect,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, if (selected) colors.onSurface else colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = effect.label.asString(),
                style = TitleTextStyle.copy(fontSize = 14.sp),
                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surfaceContainerHigh)
                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(5.dp)),
            ) {
                ShutterLoop(
                    effect = effect,
                    modifier = Modifier.matchParentSize(),
                    scale = MINIATURE_SCALE,
                )
            }
        }
    }
}

/** How long a tapped effect holds its working state before letting go. */
private const val PREVIEW_MILLIS = 2_400L

/** A 68dp-wide screen against a phone: everything a painter measures in dp shrinks by this. */
private const val MINIATURE_SCALE = 0.2f
