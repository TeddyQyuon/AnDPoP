// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from the docs/v2-ui prototype icon set (24 × 24 viewport, 1.8 stroke, round caps).

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal object AndpopIcons {
    private class Part(val data: String, val filled: Boolean = false, val alpha: Float = 1f)

    private fun icon(name: String, vararg parts: Part): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            for (part in parts) {
                addPath(
                    pathData = addPathNodes(part.data),
                    fill = if (part.filled) SolidColor(Color.Black) else null,
                    fillAlpha = part.alpha,
                    stroke = if (part.filled) null else SolidColor(Color.Black),
                    strokeAlpha = part.alpha,
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Tune: ImageVector by lazy {
        icon(
        "tune",
        Part("M4 7h9M17 7h3M4 17h3M11 17h9"),
        Part("M12.8,7a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0z"),
        Part("M6.8,17a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0z"),
        )
    }

    val Person: ImageVector by lazy {
        icon(
        "person",
        Part("M8.2,8.5a3.8,3.8 0 1,0 7.6,0a3.8,3.8 0 1,0 -7.6,0z"),
        Part("M4.5 20c.6-3.6 3.7-5.6 7.5-5.6s6.9 2 7.5 5.6"),
        )
    }

    val Refresh: ImageVector by lazy {
        icon(
        "refresh",
        Part("M19.5 12a7.5 7.5 0 1 1-2.2-5.3"),
        Part("M19.6 4.6v4.2h-4.2"),
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon(
        "chevR",
        Part("M9.5 6l6 6-6 6"),
        )
    }

    val ChevronDown: ImageVector by lazy {
        icon(
        "chevD",
        Part("M6 9.5l6 6 6-6"),
        )
    }

    val Back: ImageVector by lazy {
        icon(
        "back",
        Part("M19 12H5.5M11 5.5L4.5 12l6.5 6.5"),
        )
    }

    val Close: ImageVector by lazy {
        icon(
        "close",
        Part("M6.5 6.5l11 11M17.5 6.5l-11 11"),
        )
    }

    val Check: ImageVector by lazy {
        icon(
        "check",
        Part("M5 12.5l4.3 4.3L19 7.3"),
        )
    }

    val Bolt: ImageVector by lazy {
        icon(
        "bolt",
        Part("M13.4 2.5L5 13.6h6.1l-1.3 7.9 8.5-11.3h-6.2z", filled = true),
        )
    }

    val Bluetooth: ImageVector by lazy {
        icon(
        "bt",
        Part("M7 7.5l10 9-5 4.5V3l5 4.5-10 9"),
        )
    }

    val Mic: ImageVector by lazy {
        icon(
        "mic",
        Part("M12,3H12A3,3 0 0 1 15,6V11A3,3 0 0 1 12,14H12A3,3 0 0 1 9,11V6A3,3 0 0 1 12,3Z"),
        Part("M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21"),
        )
    }

    val Accessibility: ImageVector by lazy {
        icon(
        "access",
        Part("M10.3,4.6a1.7,1.7 0 1,0 3.4,0a1.7,1.7 0 1,0 -3.4,0z"),
        Part("M5 8.6l7 1.4 7-1.4M12 10v4.2M12 14.2l-3.2 6M12 14.2l3.2 6"),
        )
    }

    val Edit: ImageVector by lazy {
        icon(
        "edit",
        Part("M4 20h4.2L19 9.2a2.1 2.1 0 0 0 0-3L17.8 5a2.1 2.1 0 0 0-3 0L4 15.8z"),
        Part("M13.5 6.5l4 4"),
        )
    }

    val Ear: ImageVector by lazy {
        icon(
        "ear",
        Part("M6.5 9.5a5.5 5.5 0 0 1 11 0c0 2.9-1.9 4-2.9 5.4-.9 1.3-.9 2.4-1.6 3.6A3 3 0 0 1 7.6 19"),
        Part("M9.6 9.8a2.4 2.4 0 0 1 4.8 0c0 1.3-1.1 1.7-1.6 2.7"),
        Part("M19.5 6.5a7 7 0 0 1 1 3.5", alpha = 0.7f),
        )
    }

    val Info: ImageVector by lazy {
        icon(
        "info",
        Part("M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0z"),
        Part("M12 11v5.5M12 7.6v.2"),
        )
    }

    val Chip: ImageVector by lazy {
        icon(
        "chip",
        Part("M9,6.5H15A2.5,2.5 0 0 1 17.5,9V15A2.5,2.5 0 0 1 15,17.5H9A2.5,2.5 0 0 1 6.5,15V9A2.5,2.5 0 0 1 9,6.5Z"),
        Part("M9.5 3v3.5M14.5 3v3.5M9.5 17.5V21M14.5 17.5V21M3 9.5h3.5M3 14.5h3.5M17.5 9.5H21M17.5 14.5H21"),
        )
    }

    val Grid: ImageVector by lazy {
        icon(
        "grid",
        Part("M6,4H8.5A2,2 0 0 1 10.5,6V8.5A2,2 0 0 1 8.5,10.5H6A2,2 0 0 1 4,8.5V6A2,2 0 0 1 6,4Z"),
        Part("M15.5,4H18A2,2 0 0 1 20,6V8.5A2,2 0 0 1 18,10.5H15.5A2,2 0 0 1 13.5,8.5V6A2,2 0 0 1 15.5,4Z"),
        Part("M6,13.5H8.5A2,2 0 0 1 10.5,15.5V18A2,2 0 0 1 8.5,20H6A2,2 0 0 1 4,18V15.5A2,2 0 0 1 6,13.5Z"),
        Part("M16.75,13.5H16.75A3.25,3.25 0 0 1 20,16.75V16.75A3.25,3.25 0 0 1 16.75,20H16.75A3.25,3.25 0 0 1 13.5,16.75V16.75A3.25,3.25 0 0 1 16.75,13.5Z"),
        )
    }

    val Equalizer: ImageVector by lazy {
        icon(
        "eq",
        Part("M6 4v5M6 13v7M12 4v11M12 19v1M18 4v2M18 10v10"),
        Part("M4,11a2,2 0 1,0 4,0a2,2 0 1,0 -4,0z"),
        Part("M10,17a2,2 0 1,0 4,0a2,2 0 1,0 -4,0z"),
        Part("M16,8a2,2 0 1,0 4,0a2,2 0 1,0 -4,0z"),
        )
    }

    val Head: ImageVector by lazy {
        icon(
        "head",
        Part("M8 20v-2.5a6 6 0 1 1 8.5-5.4l1.6 2.6-1.8.6V17a2 2 0 0 1-2 2h-1.3v1"),
        )
    }

    val Phone: ImageVector by lazy {
        icon(
        "phone",
        Part("M6.2 3.8l2.6-.6 1.6 3.9-2 1.5a11 11 0 0 0 5 5l1.5-2 3.9 1.6-.6 2.6a2 2 0 0 1-2 1.6A15.6 15.6 0 0 1 4.6 5.8a2 2 0 0 1 1.6-2z"),
        )
    }

    val Hearing: ImageVector by lazy {
        icon(
        "hearing",
        Part("M7 9.5a5 5 0 0 1 10 0c0 2.6-2 3.4-2.7 5.3-.6 1.7-1.4 4.2-3.8 4.2a2.6 2.6 0 0 1-2.4-1.6"),
        Part("M9.7 9.8a2.3 2.3 0 0 1 4.6 0"),
        )
    }

    val Shield: ImageVector by lazy {
        icon(
        "shield",
        Part("M12 3l7 3v5.2c0 4.6-3 8.2-7 9.8-4-1.6-7-5.2-7-9.8V6z"),
        )
    }

    val Chat: ImageVector by lazy {
        icon(
        "chat",
        Part("M4.5 6.5A2.5 2.5 0 0 1 7 4h10a2.5 2.5 0 0 1 2.5 2.5v7A2.5 2.5 0 0 1 17 16h-6l-4.5 4v-4H7a2.5 2.5 0 0 1-2.5-2.5z"),
        Part("M8.5 9.2h7M8.5 12h4.5"),
        )
    }

    val Widget: ImageVector by lazy {
        icon(
        "widget",
        Part("M5.5,3.5H8.5A2,2 0 0 1 10.5,5.5V8.5A2,2 0 0 1 8.5,10.5H5.5A2,2 0 0 1 3.5,8.5V5.5A2,2 0 0 1 5.5,3.5Z"),
        Part("M15.5,3.5H18.5A2,2 0 0 1 20.5,5.5V8.5A2,2 0 0 1 18.5,10.5H15.5A2,2 0 0 1 13.5,8.5V5.5A2,2 0 0 1 15.5,3.5Z"),
        Part("M5.5,13.5H18.5A2,2 0 0 1 20.5,15.5V18.5A2,2 0 0 1 18.5,20.5H5.5A2,2 0 0 1 3.5,18.5V15.5A2,2 0 0 1 5.5,13.5Z"),
        )
    }

    val Popup: ImageVector by lazy {
        icon(
        "popup",
        Part("M8,3H16A3,3 0 0 1 19,6V18A3,3 0 0 1 16,21H8A3,3 0 0 1 5,18V6A3,3 0 0 1 8,3Z"),
        Part("M9.1,13H14.9A1.6,1.6 0 0 1 16.5,14.6V16.4A1.6,1.6 0 0 1 14.9,18H9.1A1.6,1.6 0 0 1 7.5,16.4V14.6A1.6,1.6 0 0 1 9.1,13Z"),
        )
    }

    val Island: ImageVector by lazy {
        icon(
        "island",
        Part("M8,3H16A3,3 0 0 1 19,6V18A3,3 0 0 1 16,21H8A3,3 0 0 1 5,18V6A3,3 0 0 1 8,3Z"),
        Part("M9.8,5.6H14.2A1.3,1.3 0 0 1 15.5,6.9V6.9A1.3,1.3 0 0 1 14.2,8.2H9.8A1.3,1.3 0 0 1 8.5,6.9V6.9A1.3,1.3 0 0 1 9.8,5.6Z"),
        )
    }

    val OpenExternal: ImageVector by lazy {
        icon(
        "link",
        Part("M14 4.5h5.5V10M19.2 4.8L11 13M18 14v4.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 18.5v-11A1.5 1.5 0 0 1 5.5 6H10"),
        )
    }

    val Wrench: ImageVector by lazy {
        icon(
        "wrench",
        Part("M14.6 5.2a4.4 4.4 0 0 0-5.4 5.6L3.8 16.2a1.9 1.9 0 0 0 2.7 2.7l5.4-5.4a4.4 4.4 0 0 0 5.6-5.4l-2.6 2.6-2.4-.6-.6-2.4z"),
        )
    }

    val Lock: ImageVector by lazy {
        icon(
        "lock",
        Part("M7.5,10.5H16.5A2.5,2.5 0 0 1 19,13V18A2.5,2.5 0 0 1 16.5,20.5H7.5A2.5,2.5 0 0 1 5,18V13A2.5,2.5 0 0 1 7.5,10.5Z"),
        Part("M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"),
        )
    }

    val Mail: ImageVector by lazy {
        icon(
        "mail",
        Part("M6,5.5H18A2.5,2.5 0 0 1 20.5,8V16A2.5,2.5 0 0 1 18,18.5H6A2.5,2.5 0 0 1 3.5,16V8A2.5,2.5 0 0 1 6,5.5Z"),
        Part("M4.5 7l7.5 6 7.5-6"),
        )
    }

    val Trash: ImageVector by lazy {
        icon(
        "trash",
        Part("M4.5 7h15M10 3.8h4M6.5 7l.9 11.4A2 2 0 0 0 9.4 20h5.2a2 2 0 0 0 2-1.6L17.5 7M10 11v5M14 11v5"),
        )
    }

    val Logout: ImageVector by lazy {
        icon(
        "logout",
        Part("M14 4.5h3.5A1.5 1.5 0 0 1 19 6v12a1.5 1.5 0 0 1-1.5 1.5H14M10 8l-4 4 4 4M6 12h9"),
        )
    }

    val Copy: ImageVector by lazy {
        icon(
        "copy",
        Part("M10.5,8H17A2.5,2.5 0 0 1 19.5,10.5V17A2.5,2.5 0 0 1 17,19.5H10.5A2.5,2.5 0 0 1 8,17V10.5A2.5,2.5 0 0 1 10.5,8Z"),
        Part("M5 15.5A1.5 1.5 0 0 1 4.5 14.5V6A1.5 1.5 0 0 1 6 4.5h8.5a1.5 1.5 0 0 1 1 .5"),
        )
    }

    val Share: ImageVector by lazy {
        icon(
        "share",
        Part("M15,5.5a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z"),
        Part("M4,12a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z"),
        Part("M15,18.5a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z"),
        Part("M8.7 10.8l6.6-4M8.7 13.2l6.6 4"),
        )
    }

    val Sun: ImageVector by lazy {
        icon(
        "sun",
        Part("M8,12a4,4 0 1,0 8,0a4,4 0 1,0 -8,0z"),
        Part("M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"),
        )
    }

    val Moon: ImageVector by lazy {
        icon(
        "moon",
        Part("M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z"),
        )
    }

    val Auto: ImageVector by lazy {
        icon(
        "auto",
        Part("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0z"),
        Part("M12 3.5a8.5 8.5 0 0 1 0 17z", filled = true),
        )
    }

    val Warning: ImageVector by lazy {
        icon(
        "warning",
        Part("M12 4l9 15.5H3z"),
        Part("M12 10v4.5M12 17v.2"),
        )
    }

    val Document: ImageVector by lazy {
        icon(
        "doc",
        Part("M7 3.5h7l4.5 4.5v11A1.5 1.5 0 0 1 17 20.5H7A1.5 1.5 0 0 1 5.5 19V5A1.5 1.5 0 0 1 7 3.5z"),
        Part("M13.5 3.5V8.5h5M9 13h6M9 16.5h4"),
        )
    }

    val Heart: ImageVector by lazy {
        icon(
        "heart",
        Part("M12 19.5s-7.5-4.4-7.5-10A4.3 4.3 0 0 1 12 6.8a4.3 4.3 0 0 1 7.5 2.7c0 5.6-7.5 10-7.5 10z"),
        )
    }

    val Code: ImageVector by lazy {
        icon(
        "code",
        Part("M8.5 7.5L4 12l4.5 4.5M15.5 7.5L20 12l-4.5 4.5M13.5 5l-3 14"),
        )
    }

    val SpeakerHigh: ImageVector by lazy {
        icon(
        "speakerHi",
        Part("M4.5 9.5h3l4-3.5v12l-4-3.5h-3z"),
        Part("M15 10a3 3 0 0 1 0 4M17.5 7.5a6.5 6.5 0 0 1 0 9"),
        )
    }

    val Layers: ImageVector by lazy {
        icon(
        "layers",
        Part("M12 4l8.5 4.5L12 13 3.5 8.5z"),
        Part("M3.5 12.5L12 17l8.5-4.5M3.5 16.5L12 21l8.5-4.5"),
        )
    }

    val Battery: ImageVector by lazy {
        icon(
        "battery",
        Part("M5.5,7.5H16.5A2.5,2.5 0 0 1 19,10V14A2.5,2.5 0 0 1 16.5,16.5H5.5A2.5,2.5 0 0 1 3,14V10A2.5,2.5 0 0 1 5.5,7.5Z"),
        Part("M21 10.5v3"),
        Part("M5.5 10v4h7v-4z", filled = true),
        )
    }

    val Buds: ImageVector by lazy {
        icon(
        "buds",
        Part("M7.5 4.5a3 3 0 0 1 3 3v1.5a2 2 0 0 1-1.5 1.9V19a1.5 1.5 0 0 1-3 0v-8.5A3 3 0 0 1 7.5 4.5zM16.5 4.5a3 3 0 0 0-3 3v1.5a2 2 0 0 0 1.5 1.9V19a1.5 1.5 0 0 0 3 0v-8.5a3 3 0 0 0-1.5-6z"),
        )
    }

    val Smartphone: ImageVector by lazy {
        icon(
        "smartphone",
        Part("M9,2.5H15A2.5,2.5 0 0 1 17.5,5V19A2.5,2.5 0 0 1 15,21.5H9A2.5,2.5 0 0 1 6.5,19V5A2.5,2.5 0 0 1 9,2.5Z"),
        Part("M10.5 18.5h3"),
        )
    }

    val Sparkle: ImageVector by lazy {
        icon(
        "sparkle",
        Part("M12 3.5l1.8 5.2 5.2 1.8-5.2 1.8L12 17.5l-1.8-5.2L5 10.5l5.2-1.8z"),
        )
    }

    val ModeNoiseCancellation: ImageVector by lazy {
        icon(
        "m_anc",
        Part("M12 3l7.5 3.4v4.8c0 5-3.3 8.4-7.5 9.8-4.2-1.4-7.5-4.8-7.5-9.8V6.4z"),
        Part("M8.6 12h6.8"),
        )
    }

    val ModeTransparency: ImageVector by lazy {
        icon(
        "m_transparency",
        Part("M9.8,12a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0z", filled = true),
        Part("M7.2 6.6a7.6 7.6 0 0 0 0 10.8M16.8 6.6a7.6 7.6 0 0 1 0 10.8M4.6 4a11.3 11.3 0 0 0 0 16M19.4 4a11.3 11.3 0 0 1 0 16"),
        )
    }

    val ModeAdaptive: ImageVector by lazy {
        icon(
        "m_adaptive",
        Part("M4 10v4M8 7.5v9M12 4v16M16 7.5v9M20 10v4"),
        )
    }

    val ModeOff: ImageVector by lazy {
        icon(
        "m_off",
        Part("M3.6,12a8.4,8.4 0 1,0 16.8,0a8.4,8.4 0 1,0 -16.8,0z"),
        Part("M6.2 17.8L17.8 6.2"),
        )
    }

    val Bell: ImageVector by lazy {
        icon(
        "bell",
        Part("M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15zM10 20.5h4"),
        )
    }

    val SpeakerLow: ImageVector by lazy {
        icon(
        "speakerLo",
        Part("M4.5 9.5h3l4-3.5v12l-4-3.5h-3z"),
        Part("M15 10a3 3 0 0 1 0 4"),
        )
    }

    val Eye: ImageVector by lazy {
        icon(
        "eye",
        Part("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"),
        Part("M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0z"),
        )
    }

    val EyeOff: ImageVector by lazy {
        icon(
        "eyeOff",
        Part("M4 4l16 16M10 6a9.5 9.5 0 0 1 2-.5c6 0 9.5 6.5 9.5 6.5a17 17 0 0 1-2.7 3.6M6.4 7.6A16.6 16.6 0 0 0 2.5 12S6 18.5 12 18.5a9 9 0 0 0 4.2-1M9.9 10a3 3 0 0 0 4.1 4.1"),
        )
    }

    val Play: ImageVector by lazy {
        icon(
        "play",
        Part("M8 5.5v13l10.5-6.5z"),
        )
    }

    val Stop: ImageVector by lazy {
        icon(
        "stop",
        Part("M9,6.5H15A2.5,2.5 0 0 1 17.5,9V15A2.5,2.5 0 0 1 15,17.5H9A2.5,2.5 0 0 1 6.5,15V9A2.5,2.5 0 0 1 9,6.5Z"),
        )
    }

    val Plus: ImageVector by lazy {
        icon(
        "plus",
        Part("M12 5v14M5 12h14"),
        )
    }

    val Minus: ImageVector by lazy {
        icon(
        "minus",
        Part("M5 12h14"),
        )
    }

    val CaseIcon: ImageVector by lazy {
        icon(
        "caseI",
        Part("M8,6H16A4.5,4.5 0 0 1 20.5,10.5V14.5A4.5,4.5 0 0 1 16,19H8A4.5,4.5 0 0 1 3.5,14.5V10.5A4.5,4.5 0 0 1 8,6Z"),
        Part("M3.8 10.5h16.4M12 14.2v.2"),
        )
    }

    val Headphones: ImageVector by lazy {
        icon(
        "headphones",
        Part("M4 14.5V12a8 8 0 0 1 16 0v2.5"),
        Part("M5.5,13.5H6A2,2 0 0 1 8,15.5V18.5A2,2 0 0 1 6,20.5H5.5A2,2 0 0 1 3.5,18.5V15.5A2,2 0 0 1 5.5,13.5Z"),
        Part("M18,13.5H18.5A2,2 0 0 1 20.5,15.5V18.5A2,2 0 0 1 18.5,20.5H18A2,2 0 0 1 16,18.5V15.5A2,2 0 0 1 18,13.5Z"),
        )
    }
}
