package com.example.model

data class FirmwareRelease(
    val latestVersion: String,
    val releaseHighlights: String,
    val updateInstructions: String,
)

enum class FirmwareCheckState {
    NOT_CHECKED,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
}

data class FirmwareEvaluation(
    val currentVersion: String?,
    val latestVersion: String,
    val state: FirmwareCheckState,
    val releaseHighlights: String,
    val updateInstructions: String,
    val lastCheckedMillis: Long?,
) {
    val isUpdateAvailable: Boolean
        get() = state == FirmwareCheckState.UPDATE_AVAILABLE
}

/**
 * Reference catalog of latest Apple AirPods & Beats firmware builds and version comparison logic.
 */
object FirmwareCatalog {

    private const val APPLE_UPDATE_GUIDE =
        "Place your earbuds in their charging case near a Wi-Fi connected iPhone, iPad, or Mac while charging, or visit an authorized service center to update firmware."
    private const val BEATS_UPDATE_GUIDE =
        "Connect your Beats headphones to the official Beats companion updater or charge them near a paired iPhone, iPad, or Mac to install the firmware update."

    fun latestReleaseFor(model: PodModel): FirmwareRelease = when (model) {
        PodModel.AIRPODS_PRO_2_USBC,
        PodModel.AIRPODS_PRO_2 -> FirmwareRelease(
            latestVersion = "7E93",
            releaseHighlights = "Improved Adaptive Audio transitions, Conversation Awareness reliability, and Bluetooth stability fixes.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_4_ANC,
        PodModel.AIRPODS_4 -> FirmwareRelease(
            latestVersion = "7E93",
            releaseHighlights = "Voice Isolation refinements, head gesture responsiveness, and bug fixes.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_PRO_1 -> FirmwareRelease(
            latestVersion = "6F21",
            releaseHighlights = "Audio switching improvements and Bluetooth security updates.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_3 -> FirmwareRelease(
            latestVersion = "6F21",
            releaseHighlights = "Spatial Audio stability and general Bluetooth reliability improvements.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_2 -> FirmwareRelease(
            latestVersion = "6F21",
            releaseHighlights = "Connectivity stability and bug fixes.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_1 -> FirmwareRelease(
            latestVersion = "6.8.8",
            releaseHighlights = "Final stability and battery reporting release for AirPods (1st Gen).",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_MAX_USBC -> FirmwareRelease(
            latestVersion = "7E101",
            releaseHighlights = "Lossless USB-C audio support, ultra-low latency mode, and general bug fixes.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.AIRPODS_MAX -> FirmwareRelease(
            latestVersion = "6F25",
            releaseHighlights = "Active Noise Cancellation stability and connection reliability improvements.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
        PodModel.BEATS_FIT_PRO -> FirmwareRelease(
            latestVersion = "7B24",
            releaseHighlights = "ANC calibration updates and improved Bluetooth switching.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.BEATS_STUDIO_BUDS_PLUS -> FirmwareRelease(
            latestVersion = "2C318",
            releaseHighlights = "Call clarity enhancements and Android Fast Pair stability fixes.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.BEATS_STUDIO_BUDS -> FirmwareRelease(
            latestVersion = "10M4372",
            releaseHighlights = "General stability and pairing improvements.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.POWERBEATS_PRO -> FirmwareRelease(
            latestVersion = "6F8",
            releaseHighlights = "Connection stability and battery level reporting fixes.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.BEATS_STUDIO_PRO -> FirmwareRelease(
            latestVersion = "2C301",
            releaseHighlights = "Personalized Spatial Audio and USB-C audio stability fixes.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.BEATS_SOLO_PRO -> FirmwareRelease(
            latestVersion = "6F8",
            releaseHighlights = "Bluetooth reliability and ANC stability updates.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.BEATS_FLEX -> FirmwareRelease(
            latestVersion = "6F8",
            releaseHighlights = "Audio switching and magnetic auto-pause stability fixes.",
            updateInstructions = BEATS_UPDATE_GUIDE,
        )
        PodModel.UNKNOWN -> FirmwareRelease(
            latestVersion = "7E93",
            releaseHighlights = "Latest general stability improvements.",
            updateInstructions = APPLE_UPDATE_GUIDE,
        )
    }

    fun evaluate(
        model: PodModel,
        currentVersion: String?,
        lastCheckedMillis: Long?,
    ): FirmwareEvaluation {
        val release = latestReleaseFor(model)
        val cleaned = currentVersion?.trim()?.takeIf { it.isNotEmpty() }
        val state = when {
            cleaned == null -> FirmwareCheckState.NOT_CHECKED
            compareVersions(cleaned, release.latestVersion) < 0 -> FirmwareCheckState.UPDATE_AVAILABLE
            else -> FirmwareCheckState.UP_TO_DATE
        }
        return FirmwareEvaluation(
            currentVersion = cleaned,
            latestVersion = release.latestVersion,
            state = state,
            releaseHighlights = release.releaseHighlights,
            updateInstructions = release.updateInstructions,
            lastCheckedMillis = lastCheckedMillis,
        )
    }

    /**
     * Compares two Apple/Beats firmware versions (supports alphanumeric builds like
     * "6F21" < "7B21" < "7E93" < "7E101" and dotted semver strings like "6.8.8" < "6.9.0").
     * Returns negative if current < latest, 0 if equal, positive if current > latest.
     */
    fun compareVersions(current: String, latest: String): Int {
        val a = current.trim().uppercase()
        val b = latest.trim().uppercase()
        if (a == b) return 0

        // Dotted numeric versions (e.g. "6.8.8")
        if (a.contains('.') && b.contains('.')) {
            val partsA = a.split('.').map { it.toIntOrNull() ?: 0 }
            val partsB = b.split('.').map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(partsA.size, partsB.size)
            for (i in 0 until maxLen) {
                val cmp = partsA.getOrElse(i) { 0 }.compareTo(partsB.getOrElse(i) { 0 })
                if (cmp != 0) return cmp
            }
            return 0
        }

        // Apple alphanumeric build tokens: <digits><letters><digits> (e.g., 7E93, 10M4372)
        val buildRegex = Regex("^(\\d+)([A-Z]+)(\\d+)([A-Z]*)$")
        val matchA = buildRegex.matchEntire(a)
        val matchB = buildRegex.matchEntire(b)
        if (matchA != null && matchB != null) {
            val majorCmp = matchA.groupValues[1].toInt().compareTo(matchB.groupValues[1].toInt())
            if (majorCmp != 0) return majorCmp

            val letterCmp = matchA.groupValues[2].compareTo(matchB.groupValues[2])
            if (letterCmp != 0) return letterCmp

            val minorCmp = matchA.groupValues[3].toInt().compareTo(matchB.groupValues[3].toInt())
            if (minorCmp != 0) return minorCmp

            return matchA.groupValues[4].compareTo(matchB.groupValues[4])
        }

        return a.compareTo(b)
    }
}
