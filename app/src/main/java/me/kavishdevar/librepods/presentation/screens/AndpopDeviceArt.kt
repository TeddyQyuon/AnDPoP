// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal enum class DeviceArtKind { Brand, Settings, Buds, Left, Right, Case, Headphones }

@Composable
internal fun DeviceArt(kind: DeviceArtKind, modifier: Modifier, ink: Color) {
    Canvas(modifier) {
        val u = size.minDimension / 96f
        fun pt(x: Float, y: Float) = Offset(x * u, y * u)
        fun dim(w: Float, h: Float) = Size(w * u, h * u)
        fun capsule(x: Float, y: Float, w: Float, h: Float, radius: Float, opacity: Float = 1f) {
            drawRoundRect(ink.copy(alpha = opacity), pt(x, y), dim(w, h), CornerRadius(radius * u))
        }

        fun bud(centerX: Float, centerY: Float, right: Boolean, angle: Float) {
            val direction = if (right) -1 else 1
            val tipX = centerX + direction * 16 - 10
            val stemX = centerX - direction * 5 - 6
            rotate(angle, pivot = pt(centerX, centerY)) {
                capsule(stemX, centerY, 12f, 43f, 6f, 0.75f)
                capsule(tipX, centerY - 7, 20f, 14f, 7f, 0.55f)
                capsule(centerX - 17, centerY - 13, 34f, 26f, 13f)
                capsule(centerX - direction * 11 - 2, centerY - 4, 4f, 8f, 2f, 0.7f)
            }
        }

        when (kind) {
            DeviceArtKind.Brand -> {
                listOf(25f, 52f, 76f, 45f, 25f).forEachIndexed { index, height ->
                    capsule(7f + index * 18, 48f - height / 2, 10f, height, 5f)
                }
            }
            DeviceArtKind.Settings -> {
                listOf(38f, 63f, 47f).forEachIndexed { index, x ->
                    val y = 19f + index * 29f
                    drawLine(ink, pt(7f, y), pt(x - 11f, y), 6f * u, StrokeCap.Round)
                    drawLine(ink, pt(x + 11f, y), pt(89f, y), 6f * u, StrokeCap.Round)
                    drawCircle(ink, 10f * u, pt(x, y), style = Stroke(6f * u))
                }
            }
            DeviceArtKind.Buds -> {
                bud(25f, 35f, false, -15f)
                bud(71f, 32f, true, 15f)
            }
            DeviceArtKind.Left -> bud(48f, 34f, false, -10f)
            DeviceArtKind.Right -> bud(48f, 34f, true, 10f)
            DeviceArtKind.Case -> {
                drawRoundRect(ink, pt(12f, 23f), dim(72f, 52f), CornerRadius(14 * u), style = Stroke(6 * u))
                drawLine(ink, pt(14f, 43f), pt(82f, 43f), 5 * u)
                drawCircle(ink, 4 * u, pt(48f, 55f))
            }
            DeviceArtKind.Headphones -> {
                val band = Path().apply {
                    moveTo(16 * u, 57 * u)
                    cubicTo(16 * u, 5 * u, 80 * u, 5 * u, 80 * u, 57 * u)
                }
                drawPath(band, ink, style = Stroke(7 * u, cap = StrokeCap.Round))
                capsule(9f, 50f, 22f, 32f, 9f)
                capsule(65f, 50f, 22f, 32f, 9f)
            }
        }
    }
}

