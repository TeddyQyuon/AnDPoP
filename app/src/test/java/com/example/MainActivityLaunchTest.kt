package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.CAPodDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h720dp-mdpi")
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesMainActivityAndNavigatesAllCoreScreensWithoutCrashing() {
        composeRule.waitForIdle()

        // 1. Overview renders top bar actions and empty state
        composeRule.onNodeWithTag("top_settings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_devices_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_discover_ble_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_analytics_button").assertIsDisplayed()

        // 2. Navigate to Settings, verify Automatic Ear Detection GATT toggle works, and return
        composeRule.onNodeWithTag("top_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("automatic_ear_detection_switch").assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("ear_detection_gatt_status").assertIsDisplayed()
        composeRule.onNodeWithTag("automatic_ear_detection_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("automatic_ear_detection_switch").assertIsOff()
            }.isSuccess
        }
        composeRule.onNodeWithTag("automatic_ear_detection_switch").assertIsOff()
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        // 3. Navigate to BLE Discovery and back
        composeRule.onNodeWithTag("top_discover_ble_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discovery_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("discovery_back_button").performClick()
        composeRule.waitForIdle()

        // 4. Navigate to Devices screen and back
        composeRule.onNodeWithTag("top_devices_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("devices_fab_add_profile").assertIsDisplayed()
        composeRule.onNodeWithTag("devices_back_button").performClick()
        composeRule.waitForIdle()

        // 5. Create a profile from Overview empty state
        composeRule.onNodeWithTag("empty_add_profile_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_name_input").performTextInput("My AirPods Pro")
        composeRule.onNodeWithTag("save_profile_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            try {
                composeRule.onNodeWithTag("device_card_1").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // 6. Verify ANC Quick Settings Tile controls on the device status card and cycle modes
        composeRule.onNodeWithTag("anc_qs_tile_toggle_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("anc_qs_tile_pin_1").performScrollTo().assertIsDisplayed()

        // Default for AirPods Pro 2 is Adaptive -> tap cycles Noise Cancellation -> Transparency -> Off
        composeRule.onNodeWithTag("anc_qs_tile_toggle_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("anc_qs_tile_toggle_1")
                    .assertContentDescriptionContains("Noise Cancellation. Tap to switch to Transparency")
            }.isSuccess
        }
        composeRule.onNodeWithTag("anc_qs_tile_toggle_1")
            .assertContentDescriptionContains("Noise Cancellation. Tap to switch to Transparency")

        composeRule.onNodeWithTag("anc_qs_tile_toggle_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("anc_qs_tile_toggle_1")
                    .assertContentDescriptionContains("Transparency. Tap to switch to Off")
            }.isSuccess
        }
        composeRule.onNodeWithTag("anc_qs_tile_toggle_1")
            .assertContentDescriptionContains("Transparency. Tap to switch to Off")

        composeRule.onNodeWithTag("anc_qs_tile_toggle_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            runCatching {
                composeRule.onNodeWithTag("anc_qs_tile_toggle_1")
                    .assertContentDescriptionContains("Off. Tap to switch to Noise Cancellation")
            }.isSuccess
        }
        composeRule.onNodeWithTag("anc_qs_tile_toggle_1")
            .assertContentDescriptionContains("Off. Tap to switch to Noise Cancellation")

        // 7. Verify device card opens Device Detail, rename via DataStore dialog, and run Firmware Check
        composeRule.onNodeWithTag("device_card_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail_rename_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rename_device_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("rename_device_input").performTextClearance()
        composeRule.onNodeWithTag("rename_device_input").performTextInput("Studio Pods")
        composeRule.onNodeWithTag("rename_device_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            try {
                composeRule.onNodeWithTag("detail_device_title").assertTextEquals("Studio Pods")
                true
            } catch (_: AssertionError) {
                false
            }
        }
        composeRule.onNodeWithTag("detail_lazy_column")
            .performScrollToNode(hasTestTag("firmware_status_card"))
        composeRule.onNodeWithTag("firmware_status_card").assertIsDisplayed()
        composeRule.onNodeWithTag("check_firmware_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail_lazy_column")
            .performScrollToNode(hasTestTag("audio_controls_card"))
        composeRule.onNodeWithTag("audio_controls_card").assertIsDisplayed()
        composeRule.onNodeWithTag("mic_mode_left").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("anc_one_pod_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("conversation_awareness_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("personalized_volume_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail_back_button").performClick()
        composeRule.waitForIdle()

        // 8. Open Battery Trends Analytics with an active profile
        composeRule.onNodeWithTag("top_analytics_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("analytics_range_6h").assertIsDisplayed()
        composeRule.onNodeWithTag("analytics_back_button").performClick()
        composeRule.waitForIdle()

        // 9. Verify Devices card opens detail -> edit -> delete returns cleanly to Overview
        composeRule.onNodeWithTag("top_devices_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_profile_card_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail_edit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_delete_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_removal_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("empty_add_profile_button").performScrollTo().assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun migratesLegacyV1AndV2DatabasesWithoutCrashing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        for (legacyVersion in listOf(1, 2)) {
            val dbName = "legacy_v${legacyVersion}_migration_test.db"
            context.deleteDatabase(dbName)

            val dbFile = context.getDatabasePath(dbName)
            dbFile.parentFile?.mkdirs()
            val sqlite = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            sqlite.execSQL(
                """
                CREATE TABLE `device_profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `modelId` TEXT NOT NULL,
                    `linkedBluetoothAddress` TEXT,
                    `linkedBluetoothName` TEXT,
                    `minRssiDbm` INTEGER NOT NULL,
                    `identityResolvingKeyHex` TEXT,
                    `autoPauseEnabled` INTEGER NOT NULL,
                    `autoResumeEnabled` INTEGER NOT NULL,
                    `onePodModeEnabled` INTEGER NOT NULL,
                    `preferredListeningModeId` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `device_profiles`
                (`id`, `name`, `modelId`, `minRssiDbm`, `autoPauseEnabled`, `autoResumeEnabled`, `onePodModeEnabled`, `preferredListeningModeId`, `createdAtMillis`)
                VALUES (1, 'Legacy Pods v$legacyVersion', 'airpods_pro_2', -85, 1, 0, 0, 'adaptive', 1000)
                """.trimIndent()
            )
            sqlite.version = legacyVersion
            sqlite.close()

            val roomDb = CAPodDatabase.buildForName(context, dbName)
            try {
                val profiles = roomDb.deviceProfileDao().observeAll().first()
                assertEquals(1, profiles.size)
                assertEquals("Legacy Pods v$legacyVersion", profiles.first().name)
                assertEquals(50, profiles.first().adaptiveNoiseLevel)
                assertNull(profiles.first().lastFirmwareVersion)
            } finally {
                roomDb.close()
                context.deleteDatabase(dbName)
            }
        }
    }

    @Test
    fun mainActivityRequestsScanAndConnectPermissionsOnAndroid12Plus() {
        val required = composeRule.activity.requiredBluetoothPermissions().toSet()
        assertEquals(
            setOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ),
            required
        )
    }
}
