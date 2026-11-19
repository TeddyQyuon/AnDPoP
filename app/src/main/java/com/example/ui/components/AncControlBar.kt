package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ListeningMode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Interactive Quick Settings-style Active Noise Cancellation tile + Pin-to-Quick-Settings button
 * placed directly on the device status card, followed by direct mode selector pills.
 */
@Composable
fun AncQuickSettingsTileControl(
    profileId: Long,
    selectedMode: ListeningMode,
    supportsAdaptive: Boolean,
    isUpdating: Boolean,
    isQuickTileLinked: Boolean,
    onQuickToggleMode: () -> Unit,
    onSelectMode: (ListeningMode) -> Unit,
    onPinQuickSettingsTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextMode = selectedMode.nextQuickToggleMode()
    val currentLabel = listeningModeLabel(selectedMode)
    val nextLabel = listeningModeLabel(nextMode)
    val isActive = selectedMode != ListeningMode.OFF

    val tileContainerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "anc_qs_tile_bg_$profileId"
    )
    val tileContentColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "anc_qs_tile_fg_$profileId"
    )
    val tileSecondaryTextColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "anc_qs_tile_sub_$profileId"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Quick Settings Tile Toggle Pill (cycles Noise Cancellation -> Transparency -> Off)
            val toggleDescription = "$currentLabel. ${stringResource(R.string.qs_tile_anc_tap_to_cycle, nextLabel)}"
            Surface(
                onClick = onQuickToggleMode,
                enabled = !isUpdating,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics { contentDescription = toggleDescription }
                    .testTag("anc_qs_tile_toggle_$profileId"),
                shape = RoundedCornerShape(16.dp),
                color = tileContainerColor,
                border = if (isActive) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        } else {
                            Icon(
                                imageVector = listeningModeIcon(selectedMode),
                                contentDescription = null,
                                tint = if (isActive) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = tileContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.qs_tile_anc_tap_to_cycle, nextLabel),
                            style = MaterialTheme.typography.labelSmall,
                            color = tileSecondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Pin / Link Android Quick Settings Tile directly from the device status card
            val pinDescription = stringResource(R.string.cd_pin_anc_qs_tile)
            Surface(
                onClick = onPinQuickSettingsTile,
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .widthIn(min = 72.dp)
                    .semantics { contentDescription = pinDescription }
                    .testTag("anc_qs_tile_pin_$profileId"),
                shape = RoundedCornerShape(16.dp),
                color = if (isQuickTileLinked) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isQuickTileLinked) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DashboardCustomize,
                        contentDescription = null,
                        tint = if (isQuickTileLinked) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isQuickTileLinked) {
                            stringResource(R.string.qs_tile_pinned_badge)
                        } else {
                            stringResource(R.string.qs_tile_pin_action)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isQuickTileLinked) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )
                }
            }
        }

        AncControlBar(
            selectedMode = selectedMode,
            supportsAdaptive = supportsAdaptive,
            isUpdating = isUpdating,
            onSelectMode = onSelectMode,
        )
    }
}

/**
 * CAPod-style ANC Listening Mode selector bar (Off / Noise Cancellation / Transparency / Adaptive).
 * Active mode smoothly expands into an accented pill button showing icon + label.
 */
@Composable
fun AncControlBar(
    selectedMode: ListeningMode,
    supportsAdaptive: Boolean,
    isUpdating: Boolean,
    onSelectMode: (ListeningMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = if (supportsAdaptive) {
        listOf(
            ListeningMode.NOISE_CANCELLATION,
            ListeningMode.TRANSPARENCY,
            ListeningMode.OFF,
            ListeningMode.ADAPTIVE,
        )
    } else {
        listOf(
            ListeningMode.NOISE_CANCELLATION,
            ListeningMode.TRANSPARENCY,
            ListeningMode.OFF,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selectedMode
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                },
                label = "anc_bg_${mode.id}"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "anc_fg_${mode.id}"
            )

            val label = listeningModeLabel(mode)
            val icon = listeningModeIcon(mode)

            Box(
                modifier = Modifier
                    .then(if (isSelected) Modifier.weight(1f) else Modifier)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .clickable(enabled = !isUpdating) { onSelectMode(mode) }
                    .testTag("anc_mode_${mode.id}")
                    .padding(horizontal = if (isSelected) 16.dp else 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected && isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun listeningModeLabel(mode: ListeningMode): String = when (mode) {
    ListeningMode.OFF -> stringResource(R.string.anc_off)
    ListeningMode.NOISE_CANCELLATION -> stringResource(R.string.anc_noise_cancellation)
    ListeningMode.TRANSPARENCY -> stringResource(R.string.anc_transparency)
    ListeningMode.ADAPTIVE -> stringResource(R.string.anc_adaptive)
}

fun listeningModeIcon(mode: ListeningMode): ImageVector = when (mode) {
    ListeningMode.OFF -> Icons.Rounded.DoNotDisturbOn
    ListeningMode.NOISE_CANCELLATION -> Icons.Rounded.Headphones
    ListeningMode.TRANSPARENCY -> Icons.Rounded.Hearing
    ListeningMode.ADAPTIVE -> Icons.Rounded.AutoAwesome
}
