package com.costafotiadis.deckard.mascot

import androidx.compose.ui.graphics.Color
import com.costafotiadis.deckard.R
import com.costafotiadis.deckard.slop.SlopLabel
import com.costafotiadis.design.theme.StampInks
import com.costafotiadis.textresource.TextResource

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

/**
 * What the stamp reads when the detector sent no phrase of its own.
 *
 * Stored mixed-case, because the stamp uppercases whatever it is handed — so this fallback and
 * Pangram's own headline go through the same call rather than one of them arriving pre-shouted.
 */
val SlopLabel.stampText: TextResource
    get() = when (this) {
        SlopLabel.AI -> TextResource.simple(R.string.stamp_ai)
        SlopLabel.ASSISTED -> TextResource.simple(R.string.stamp_assisted)
        SlopLabel.HUMAN -> TextResource.simple(R.string.stamp_human)
    }
