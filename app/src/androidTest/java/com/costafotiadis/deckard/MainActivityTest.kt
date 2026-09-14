package com.costafotiadis.deckard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.costafotiadis.deckard.ui.activity.MainActivity
import dagger.hilt.EntryPoints
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * Use the primary activity to initialize the app normally.
     */
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // This is how to reach into SingletonComponent for the test application
        // prefer TestInstallIn imo
        EntryPoints.get(
            composeTestRule.activity.application,
            ApplicationEntryPoint::class.java,
        ).appInitializer()
    }

    /**
     * Asserted against the resource rather than a literal: the copy now lives in `strings.xml`, and
     * a test holding its own copy of a string is a test that goes stale the first time the wording
     * changes. (This one had — it asserted on a line the screen stopped saying.)
     */
    @Test
    fun setupScreen_isShown() {
        val context = composeTestRule.activity
        composeTestRule.apply {
            onNodeWithText(context.getString(R.string.setup_tagline)).assertIsDisplayed()
            onNodeWithText(context.getString(R.string.setup_section_what_he_needs))
                .assertIsDisplayed()
        }
    }
}
