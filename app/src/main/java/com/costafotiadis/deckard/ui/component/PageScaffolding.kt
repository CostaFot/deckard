package com.costafotiadis.deckard.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.costafotiadis.deckard.mascot.MetaTextStyle

/**
 * The page both destinations are written on: one scrolling column, inset off the system bars, with
 * its sections a fixed distance apart.
 *
 * It is a column and not a Surface so that a caller can put something over it — the settings screen
 * plays an effect full-size on top of its own page — without the page having to know.
 */
@Composable
internal fun DeckardPage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

/** A section heading with a rule running off to the right of it. */
@Composable
internal fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = MetaTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}
