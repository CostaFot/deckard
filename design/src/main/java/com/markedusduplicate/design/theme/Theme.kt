package com.markedusduplicate.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Deckard's register expressed as a Material scheme, so stock components inherit it instead of
 * having it hand-plumbed onto them at every call site: paper is the surface, ink is what sits on it,
 * and the hairline that edges every card is the outline. Nothing here is an accent — ink is also the
 * primary, because the one filled control in the product is drawn in it.
 */
private val LightColors = lightColorScheme(
    primary = InkLight,
    onPrimary = PaperLight,
    primaryContainer = HairlineLight,
    onPrimaryContainer = InkLight,
    secondary = InkMutedLight,
    onSecondary = PaperLight,
    secondaryContainer = HairlineLight,
    onSecondaryContainer = InkLight,
    tertiary = InkMutedLight,
    onTertiary = PaperLight,
    tertiaryContainer = HairlineLight,
    onTertiaryContainer = InkLight,
    error = LightStampInks.ai,
    onError = PaperLight,
    errorContainer = Color(0xFFFBE2DC),
    onErrorContainer = Color(0xFF7A1B0C),
    background = PaperLight,
    onBackground = InkLight,
    surface = PaperLight,
    onSurface = InkLight,
    surfaceVariant = HairlineLight,
    onSurfaceVariant = InkMutedLight,
    outline = InkMutedLight,
    outlineVariant = HairlineLight,
    scrim = Color(0xFF000000),
    inverseSurface = InkLight,
    inverseOnSurface = PaperLight,
    inversePrimary = PaperLight,
    surfaceDim = Color(0xFFDFDFD9),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFBF9),
    surfaceContainer = Color(0xFFF2F2EE),
    surfaceContainerHigh = Color(0xFFECECE7),
    surfaceContainerHighest = Color(0xFFE6E6E1),
)

private val DarkColors = darkColorScheme(
    primary = InkDark,
    onPrimary = PaperDark,
    primaryContainer = HairlineDark,
    onPrimaryContainer = InkDark,
    secondary = InkMutedDark,
    onSecondary = PaperDark,
    secondaryContainer = HairlineDark,
    onSecondaryContainer = InkDark,
    tertiary = InkMutedDark,
    onTertiary = PaperDark,
    tertiaryContainer = HairlineDark,
    onTertiaryContainer = InkDark,
    error = DarkStampInks.ai,
    onError = PaperDark,
    errorContainer = Color(0xFF4A1B10),
    onErrorContainer = Color(0xFFFFD7CD),
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = HairlineDark,
    onSurfaceVariant = InkMutedDark,
    outline = InkMutedDark,
    outlineVariant = HairlineDark,
    scrim = Color(0xFF000000),
    inverseSurface = InkDark,
    inverseOnSurface = PaperDark,
    inversePrimary = PaperDark,
    surfaceDim = PaperDark,
    surfaceBright = Color(0xFF363C45),
    surfaceContainerLowest = Color(0xFF0E1115),
    surfaceContainerLow = Color(0xFF1A1E24),
    surfaceContainer = Color(0xFF1E222A),
    surfaceContainerHigh = Color(0xFF262B33),
    surfaceContainerHighest = Color(0xFF2F343D),
)

private val LocalStampInks = staticCompositionLocalOf { LightStampInks }

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStampInks provides if (useDarkTheme) DarkStampInks else LightStampInks) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}

/** The verdict inks that go with the current [AppTheme], reached the way the colour scheme is. */
val MaterialTheme.stampInks: StampInks
    @Composable
    @ReadOnlyComposable
    get() = LocalStampInks.current
