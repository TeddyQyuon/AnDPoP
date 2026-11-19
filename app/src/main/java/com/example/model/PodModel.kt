package com.example.model

enum class PodFormFactor {
    DUAL_POD,
    SINGLE_HEADSET,
}

enum class PodIconStyle {
    AIRPODS_CLASSIC,
    AIRPODS_GEN3,
    AIRPODS_PRO,
    AIRPODS_MAX,
    BEATS_BUDS,
    BEATS_OVER_EAR,
}

enum class PodModel(
    val id: String,
    val displayName: String,
    val manufacturer: String,
    val modelCode: Int,
    val formFactor: PodFormFactor,
    val iconStyle: PodIconStyle,
    val supportsAnc: Boolean,
    val supportsAdaptive: Boolean,
    val ratedListeningMinutes: Int,
    val fullChargeMinutes: Int,
) {
    AIRPODS_PRO_2_USBC(
        id = "airpods_pro_2_usbc",
        displayName = "AirPods Pro 2 (USB-C)",
        manufacturer = "Apple",
        modelCode = 0x2420,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_PRO,
        supportsAnc = true,
        supportsAdaptive = true,
        ratedListeningMinutes = 360,
        fullChargeMinutes = 60,
    ),
    AIRPODS_PRO_2(
        id = "airpods_pro_2",
        displayName = "AirPods Pro 2",
        manufacturer = "Apple",
        modelCode = 0x1420,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_PRO,
        supportsAnc = true,
        supportsAdaptive = true,
        ratedListeningMinutes = 360,
        fullChargeMinutes = 60,
    ),
    AIRPODS_PRO_1(
        id = "airpods_pro_1",
        displayName = "AirPods Pro",
        manufacturer = "Apple",
        modelCode = 0x0E20,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_PRO,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 270,
        fullChargeMinutes = 60,
    ),
    AIRPODS_4_ANC(
        id = "airpods_4_anc",
        displayName = "AirPods 4 (ANC)",
        manufacturer = "Apple",
        modelCode = 0x1B20,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_GEN3,
        supportsAnc = true,
        supportsAdaptive = true,
        ratedListeningMinutes = 300,
        fullChargeMinutes = 55,
    ),
    AIRPODS_4(
        id = "airpods_4",
        displayName = "AirPods 4",
        manufacturer = "Apple",
        modelCode = 0x1920,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_GEN3,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 300,
        fullChargeMinutes = 55,
    ),
    AIRPODS_3(
        id = "airpods_3",
        displayName = "AirPods (3rd Gen)",
        manufacturer = "Apple",
        modelCode = 0x1320,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_GEN3,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 360,
        fullChargeMinutes = 60,
    ),
    AIRPODS_2(
        id = "airpods_2",
        displayName = "AirPods (2nd Gen)",
        manufacturer = "Apple",
        modelCode = 0x0F20,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_CLASSIC,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 300,
        fullChargeMinutes = 60,
    ),
    AIRPODS_1(
        id = "airpods_1",
        displayName = "AirPods (1st Gen)",
        manufacturer = "Apple",
        modelCode = 0x0220,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_CLASSIC,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 300,
        fullChargeMinutes = 60,
    ),
    AIRPODS_MAX_USBC(
        id = "airpods_max_usbc",
        displayName = "AirPods Max (USB-C)",
        manufacturer = "Apple",
        modelCode = 0x1F20,
        formFactor = PodFormFactor.SINGLE_HEADSET,
        iconStyle = PodIconStyle.AIRPODS_MAX,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 1200,
        fullChargeMinutes = 120,
    ),
    AIRPODS_MAX(
        id = "airpods_max",
        displayName = "AirPods Max",
        manufacturer = "Apple",
        modelCode = 0x0A20,
        formFactor = PodFormFactor.SINGLE_HEADSET,
        iconStyle = PodIconStyle.AIRPODS_MAX,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 1200,
        fullChargeMinutes = 120,
    ),
    BEATS_FIT_PRO(
        id = "beats_fit_pro",
        displayName = "Beats Fit Pro",
        manufacturer = "Beats",
        modelCode = 0x1220,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.BEATS_BUDS,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 360,
        fullChargeMinutes = 65,
    ),
    BEATS_STUDIO_BUDS_PLUS(
        id = "beats_studio_buds_plus",
        displayName = "Beats Studio Buds+",
        manufacturer = "Beats",
        modelCode = 0x1620,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.BEATS_BUDS,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 540,
        fullChargeMinutes = 70,
    ),
    BEATS_STUDIO_BUDS(
        id = "beats_studio_buds",
        displayName = "Beats Studio Buds",
        manufacturer = "Beats",
        modelCode = 0x1120,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.BEATS_BUDS,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 480,
        fullChargeMinutes = 70,
    ),
    POWERBEATS_PRO(
        id = "powerbeats_pro",
        displayName = "Powerbeats Pro",
        manufacturer = "Beats",
        modelCode = 0x0B20,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.BEATS_BUDS,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 540,
        fullChargeMinutes = 75,
    ),
    BEATS_STUDIO_PRO(
        id = "beats_studio_pro",
        displayName = "Beats Studio Pro",
        manufacturer = "Beats",
        modelCode = 0x1720,
        formFactor = PodFormFactor.SINGLE_HEADSET,
        iconStyle = PodIconStyle.BEATS_OVER_EAR,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 1440,
        fullChargeMinutes = 120,
    ),
    BEATS_SOLO_PRO(
        id = "beats_solo_pro",
        displayName = "Beats Solo Pro",
        manufacturer = "Beats",
        modelCode = 0x0C20,
        formFactor = PodFormFactor.SINGLE_HEADSET,
        iconStyle = PodIconStyle.BEATS_OVER_EAR,
        supportsAnc = true,
        supportsAdaptive = false,
        ratedListeningMinutes = 1320,
        fullChargeMinutes = 110,
    ),
    BEATS_FLEX(
        id = "beats_flex",
        displayName = "Beats Flex",
        manufacturer = "Beats",
        modelCode = 0x1020,
        formFactor = PodFormFactor.SINGLE_HEADSET,
        iconStyle = PodIconStyle.BEATS_BUDS,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 720,
        fullChargeMinutes = 70,
    ),
    UNKNOWN(
        id = "unknown",
        displayName = "Unknown Audio Device",
        manufacturer = "Apple",
        modelCode = 0x0000,
        formFactor = PodFormFactor.DUAL_POD,
        iconStyle = PodIconStyle.AIRPODS_PRO,
        supportsAnc = false,
        supportsAdaptive = false,
        ratedListeningMinutes = 300,
        fullChargeMinutes = 60,
    );

    companion object {
        val selectableModels: List<PodModel> = entries.filter { it != UNKNOWN }

        fun fromId(id: String): PodModel = entries.firstOrNull { it.id == id } ?: AIRPODS_PRO_2

        fun fromModelCode(code: Int): PodModel =
            entries.firstOrNull { it.modelCode == code } ?: UNKNOWN

        fun guessFromName(name: String): PodModel {
            val lower = name.lowercase()
            return when {
                "airpods pro" in lower && "2" in lower -> AIRPODS_PRO_2
                "airpods pro" in lower -> AIRPODS_PRO_1
                "airpods max" in lower -> AIRPODS_MAX
                "airpods 4" in lower -> AIRPODS_4_ANC
                "airpods 3" in lower -> AIRPODS_3
                "airpods" in lower -> AIRPODS_2
                "beats fit pro" in lower -> BEATS_FIT_PRO
                "studio buds+" in lower || "studio buds plus" in lower -> BEATS_STUDIO_BUDS_PLUS
                "studio buds" in lower -> BEATS_STUDIO_BUDS
                "powerbeats" in lower -> POWERBEATS_PRO
                "studio pro" in lower -> BEATS_STUDIO_PRO
                "solo pro" in lower -> BEATS_SOLO_PRO
                "beats flex" in lower -> BEATS_FLEX
                else -> AIRPODS_PRO_2
            }
        }
    }
}
