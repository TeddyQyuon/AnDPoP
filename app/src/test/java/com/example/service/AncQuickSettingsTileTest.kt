package com.example.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.data.DeviceProfileEntity
import com.example.model.ListeningMode
import com.example.model.PodModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AncQuickSettingsTileTest {

    @Test
    fun nextQuickToggleModeCyclesAncTransparencyOff() {
        assertEquals(
            ListeningMode.TRANSPARENCY,
            ListeningMode.NOISE_CANCELLATION.nextQuickToggleMode()
        )
        assertEquals(
            ListeningMode.OFF,
            ListeningMode.TRANSPARENCY.nextQuickToggleMode()
        )
        assertEquals(
            ListeningMode.NOISE_CANCELLATION,
            ListeningMode.OFF.nextQuickToggleMode()
        )
        assertEquals(
            ListeningMode.NOISE_CANCELLATION,
            ListeningMode.ADAPTIVE.nextQuickToggleMode()
        )
    }

    @Test
    fun resolveActiveAncProfilePrefersPinnedAncProfileAndSkipsNonAncModels() {
        val nonAnc = DeviceProfileEntity(
            id = 1L,
            name = "AirPods 2",
            modelId = PodModel.AIRPODS_2.id,
        )
        val pro1 = DeviceProfileEntity(
            id = 2L,
            name = "Work AirPods Pro",
            modelId = PodModel.AIRPODS_PRO_1.id,
        )
        val max = DeviceProfileEntity(
            id = 3L,
            name = "Studio AirPods Max",
            modelId = PodModel.AIRPODS_MAX.id,
        )

        // Returns null when no ANC-capable profile exists
        assertNull(AncQuickSettingsTile.resolveActiveAncProfile(listOf(nonAnc), preferredProfileId = 1L))

        // Prefers pinned ANC profile
        val pinned = AncQuickSettingsTile.resolveActiveAncProfile(
            profiles = listOf(nonAnc, pro1, max),
            preferredProfileId = 3L,
        )
        assertEquals(3L, pinned?.id)

        // Falls back to first ANC-capable profile when pinned ID is missing or non-ANC
        val fallback = AncQuickSettingsTile.resolveActiveAncProfile(
            profiles = listOf(nonAnc, pro1, max),
            preferredProfileId = 1L,
        )
        assertEquals(2L, fallback?.id)
    }

    @Test
    fun syncAndReadCachedSnapshotPersistsSelectedModeAndIcons() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        AncQuickSettingsTile.syncAndRequestRefresh(
            context = context,
            profileId = 42L,
            deviceDisplayName = "Studio Pods",
            mode = ListeningMode.TRANSPARENCY,
            makeTarget = true,
        )

        val cached = AncQuickSettingsTile.readCachedSnapshot(context)
        assertTrue(cached.hasAncDevice)
        assertEquals(42L, cached.profileId)
        assertEquals("Studio Pods", cached.deviceName)
        assertEquals(ListeningMode.TRANSPARENCY, cached.mode)
        assertEquals(42L, AncQuickSettingsTile.readTargetProfileId(context))

        assertEquals(R.drawable.ic_qs_anc_on, AncQuickSettingsTile.iconResForMode(ListeningMode.NOISE_CANCELLATION))
        assertEquals(R.drawable.ic_qs_anc_transparency, AncQuickSettingsTile.iconResForMode(ListeningMode.TRANSPARENCY))
        assertEquals(R.drawable.ic_qs_anc_off, AncQuickSettingsTile.iconResForMode(ListeningMode.OFF))
    }
}
