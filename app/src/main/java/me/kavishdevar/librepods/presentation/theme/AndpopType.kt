// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** AnDPoP 2.0 type scale (see docs/v2-ui/README.md). Sizes scale with the Android font size. */
internal object AndpopType {
    val display = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.4).sp)
    val pageTitle = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.2).sp)
    val title = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.1).sp)
    val titleSmall = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp)
    val body = TextStyle(fontFamily = andpopRegular, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp)
    val bodySmall = TextStyle(fontFamily = andpopRegular, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val supporting = TextStyle(fontFamily = andpopRegular, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp)
    val caption = TextStyle(fontFamily = andpopRegular, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp)
    val section = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
    val label = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp)
    val tiny = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp)
    val button = TextStyle(fontFamily = andpopSemiBold, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
    val value = TextStyle(fontFamily = andpopRegular, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val battery = TextStyle(fontFamily = andpopBold, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, fontFeatureSettings = "tnum")
}
