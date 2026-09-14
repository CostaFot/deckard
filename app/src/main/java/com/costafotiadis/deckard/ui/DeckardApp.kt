package com.costafotiadis.deckard.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.costafotiadis.deckard.shutter.ShutterEffect
import com.costafotiadis.deckard.ui.settings.SettingsScreen
import com.costafotiadis.deckard.ui.setup.SetupScreen
import kotlinx.serialization.Serializable

/**
 * The screens the one Activity can show.
 *
 * They are `@Serializable` because Navigation 3 saves the back stack itself, through kotlinx
 * serialization rather than a `Parcelable` or a route string — so a destination is an object you
 * construct, and a typo in one is a compile error instead of a dead link.
 */
@Serializable
internal sealed interface Destination : NavKey {

    /** Where a fresh launch lands: the permissions, the start button, the gestures. */
    @Serializable
    data object Setup : Destination

    /** What you can change about him. */
    @Serializable
    data object Settings : Destination
}

/**
 * The Activity's whole content: a back stack of [Destination]s and the display that renders the top
 * of it.
 *
 * The shutter choice is hoisted this far up because it is owned by a process-wide
 * [com.costafotiadis.deckard.shutter.ShutterEffectStore] that the overlay service reads too — the
 * settings screen is one of two things looking at the same value, not the place it lives.
 */
@Composable
internal fun DeckardApp(
    shutter: ShutterEffect,
    onPickShutter: (ShutterEffect) -> Unit,
) {
    val backStack = rememberNavBackStack(Destination.Setup)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Destination.Setup> {
                SetupScreen(onOpenSettings = { backStack.add(Destination.Settings) })
            }
            entry<Destination.Settings> {
                SettingsScreen(
                    shutter = shutter,
                    onPickShutter = onPickShutter,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
