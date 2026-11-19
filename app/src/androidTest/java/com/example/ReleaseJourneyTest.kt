package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class ReleaseJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun scrollTo(matcher: SemanticsMatcher) {
        ui.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(matcher)
    }

    @Test fun preservesDraftAcrossRecreationAndValidatesIdentityKey() {
        ui.onNodeWithTag("top_devices_button").performClick()
        ui.onNodeWithTag("devices_fab_add_profile").performClick()
        ui.onNodeWithTag("profile_name_input").performTextInput("Travel earbuds")
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("profile_name_input").assertTextContains("Travel earbuds")
        scrollTo(hasTestTag("irk_hex_input"))
        ui.onNodeWithTag("irk_hex_input").performTextInput("123")
        ui.onNodeWithTag("save_profile_button").assertIsNotEnabled()
        ui.onNodeWithTag("irk_hex_input").performTextClearance()
        ui.onNodeWithTag("save_profile_button").assertIsEnabled()
    }

    @Test fun opensDiscoveryPrivacyAndHistoryConfirmation() {
        ui.onNodeWithTag("top_discover_ble_button").performClick()
        ui.onNodeWithTag("discovery_back_button").performClick()
        ui.onNodeWithTag("top_settings_button").performClick()
        scrollTo(hasTestTag("clear_all_history_button"))
        ui.onNodeWithTag("clear_all_history_button").performClick()
        ui.onNodeWithText("Cancel").performClick()
        scrollTo(hasText("Privacy policy"))
        ui.onNodeWithText("Privacy policy").performClick()
        ui.onNodeWithText("CAPod uses nearby Bluetooth signals", substring = true).assertIsDisplayed()
    }
}
