package com.costafotiadis.deckard.ui.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.costafotiadis.deckard.R
import com.costafotiadis.deckard.accessibility.DeckardAccessibilityService
import com.costafotiadis.deckard.mascot.BodyTextStyle
import com.costafotiadis.deckard.mascot.DeckardOverlayService
import com.costafotiadis.deckard.mascot.DeckardPlate
import com.costafotiadis.deckard.mascot.DeckardVoice
import com.costafotiadis.deckard.mascot.DisplayTextStyle
import com.costafotiadis.deckard.mascot.MetaTextStyle
import com.costafotiadis.deckard.mascot.TitleTextStyle
import com.costafotiadis.deckard.ui.component.DeckardPage
import com.costafotiadis.deckard.ui.component.SectionLabel
import com.costafotiadis.design.theme.stampInks
import com.costafotiadis.textresource.asString

/**
 * Hand Deckard the two permissions he can't grant himself, start him, and learn the gestures that
 * summon him. Everything after this happens in the overlay.
 *
 * It is a checklist, which is why the preferences are not on it: you work down this screen once and
 * never come back, where [com.costafotiadis.deckard.ui.settings.SettingsScreen] is the one you
 * return to.
 */
@Composable
internal fun SetupScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var canDrawOverlays by remember { mutableStateOf(false) }
    var isDeckardRunning by remember { mutableStateOf(false) }

    // Re-read status every time the Activity resumes, so returning from system
    // settings refreshes the indicators.
    LifecycleResumeEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        canDrawOverlays = Settings.canDrawOverlays(context)
        isDeckardRunning = DeckardOverlayService.isRunning
        onPauseOrDispose {}
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        DeckardPage {
            Masthead()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(text = stringResource(R.string.setup_section_what_he_needs))
                Step(
                    index = "01",
                    title = stringResource(R.string.setup_step_screen_reading_title),
                    detail = stringResource(R.string.setup_step_screen_reading_detail),
                    done = isAccessibilityEnabled,
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
                Step(
                    index = "02",
                    title = stringResource(R.string.setup_step_overlay_title),
                    detail = stringResource(R.string.setup_step_overlay_detail),
                    done = canDrawOverlays,
                    onAction = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )
            }

            StartButton(
                running = isDeckardRunning,
                enabled = canDrawOverlays && isAccessibilityEnabled,
                onClick = {
                    if (isDeckardRunning) {
                        DeckardOverlayService.stop(context)
                        isDeckardRunning = false
                    } else {
                        DeckardOverlayService.start(context)
                        isDeckardRunning = true
                    }
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(text = stringResource(R.string.setup_section_summoning_him))
                Gesture(
                    gesture = stringResource(R.string.setup_gesture_swipe_title),
                    detail = stringResource(R.string.setup_gesture_swipe_detail),
                )
                Gesture(
                    gesture = stringResource(R.string.setup_gesture_hold_title),
                    detail = stringResource(R.string.setup_gesture_hold_detail),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(text = stringResource(R.string.setup_section_how_he_works))
                SettingsDoor(
                    title = stringResource(R.string.setup_settings_title),
                    detail = stringResource(R.string.setup_settings_detail),
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

/**
 * The way through to the settings. It wears the same bordered box as a [Step] because it is the
 * same kind of thing — one row you press that takes you somewhere else — and the arrow is the only
 * part that says the somewhere else is inside the app rather than in Android's settings.
 */
@Composable
private fun SettingsDoor(
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = TitleTextStyle)
                Text(
                    text = detail,
                    style = BodyTextStyle.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "→", style = TitleTextStyle, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Masthead() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DeckardPlate(size = 64.dp, emojiSize = 34.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = stringResource(R.string.setup_wordmark), style = DisplayTextStyle)
            Text(
                text = stringResource(R.string.setup_tagline),
                style = MetaTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = DeckardVoice.CATCHPHRASE.asString(),
                style = BodyTextStyle.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One thing Deckard needs before he can work. The two steps are a real sequence — he can't be
 * started until both are granted — so they carry their position, and a granted one steps back to a
 * struck-through mark rather than shouting for attention it no longer needs.
 */
@Composable
private fun Step(
    index: String,
    title: String,
    detail: String,
    done: Boolean,
    onAction: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val granted = MaterialTheme.stampInks.human
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) granted else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (done) granted else colors.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (done) "✓" else index,
                style = MetaTextStyle.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                color = if (done) Color.White else colors.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = TitleTextStyle,
                color = if (done) colors.onSurfaceVariant else colors.onSurface,
            )
            Text(
                text = detail,
                style = BodyTextStyle.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.onSurfaceVariant,
            )
            if (!done) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = onAction,
                    shape = RoundedCornerShape(9.dp),
                    color = Color.Transparent,
                    contentColor = colors.onSurface,
                    border = BorderStroke(1.dp, colors.onSurface),
                ) {
                    Text(
                        text = stringResource(R.string.setup_open_settings),
                        style = BodyTextStyle.copy(fontSize = 13.sp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StartButton(
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        !enabled -> colors.outlineVariant
        running -> Color.Transparent
        else -> colors.primary
    }
    val content = when {
        !enabled -> colors.onSurfaceVariant
        running -> colors.onSurface
        else -> colors.onPrimary
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = background,
        contentColor = content,
        border = if (running && enabled) BorderStroke(1.dp, colors.onSurface) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(
                if (running) R.string.setup_stop_deckard else R.string.setup_start_deckard,
            ),
            style = TitleTextStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun Gesture(gesture: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = gesture, style = TitleTextStyle.copy(fontSize = 14.sp))
        Text(
            text = detail,
            style = BodyTextStyle.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, DeckardAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}
