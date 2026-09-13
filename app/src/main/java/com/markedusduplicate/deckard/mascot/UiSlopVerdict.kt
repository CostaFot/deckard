package com.markedusduplicate.deckard.mascot

import androidx.compose.ui.graphics.Color
import com.markedusduplicate.deckard.slop.SlopLabel
import com.markedusduplicate.design.theme.StampInks

/**
 * UI-layer representation of a slop verdict, shown in Deckard's speech bubble. Carries the scalar
 * breakdown for now; per-segment windows are omitted as they don't fit the bubble.
 */
data class UiSlopVerdict(
    val label: SlopLabel,
    val summary: String,
    val predictionShort: String,
    val headline: String,
    val prediction: String,
    val fractionAi: Double,
    val fractionAiAssisted: Double,
    val fractionHuman: Double,
    val numAiSegments: Int,
    val numAiAssistedSegments: Int,
    val numHumanSegments: Int,
    val dashboardLink: String?,
    val version: String,
    val wordCount: Int,
    val analyzedText: String,
    val confidence: String,
)

/**
 * The ink a verdict is stamped in. It lives here rather than on [StampInks] because `:design` holds
 * the three inks but knows nothing about what they judge.
 */
fun StampInks.forLabel(label: SlopLabel): Color = when (label) {
    SlopLabel.AI -> ai
    SlopLabel.ASSISTED -> assisted
    SlopLabel.HUMAN -> human
}

/** What the stamp reads when the detector sent no phrase of its own. */
val SlopLabel.stampText: String
    get() = when (this) {
        SlopLabel.AI -> "AI-Generated"
        SlopLabel.ASSISTED -> "AI-Assisted"
        SlopLabel.HUMAN -> "Human-Written"
    }
