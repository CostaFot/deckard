package com.costafotiadis.deckard.shutter

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which [ShutterEffect] is in force, shared between the setup screen that picks it and the overlay
 * service that plays it.
 *
 * They are the same process, so the flow is what actually keeps them in step — a change on the
 * setup screen is in force on the very next summon, with no restart. The preferences file only
 * carries the choice across one.
 *
 * A single enum does not earn DataStore (which the version catalog has but nothing in this repo
 * uses) and its coroutine plumbing: the value has to be readable the instant the service is created,
 * which is exactly what DataStore will not do.
 */
@Singleton
class ShutterEffectStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val current = MutableStateFlow(ShutterEffect.fromKey(preferences.getString(KEY, null)))

    val selected: StateFlow<ShutterEffect> = current.asStateFlow()

    fun select(effect: ShutterEffect) {
        current.value = effect
        preferences.edit { putString(KEY, effect.key) }
    }

    private companion object {
        const val FILE = "deckard_shutter"
        const val KEY = "effect"
    }
}
