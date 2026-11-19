package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BatteryLogEntity
import com.example.model.AnalyticsTimeRange
import com.example.model.BatteryAnalyticsCalculator
import com.example.model.PodFormFactor
import com.example.model.formatDurationMinutes
import com.example.ui.components.DeviceModelIcon
import com.example.ui.components.InteractiveBatteryDrainChart
import com.example.ui.components.NetDrainChargeBarChart
import com.example.ui.theme.CapodMint
import com.example.viewmodel.MatchedDeviceState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryAnalyticsScreen(
    matchedDevices: List<MatchedDeviceState>,
    selectedProfileId: Long?,
    selectedRange: AnalyticsTimeRange,
    logsForSelectedProfile: List<BatteryLogEntity>,
    onSelectProfile: (Long) -> Unit,
    onSelectRange: (AnalyticsTimeRange) -> Unit,
    onAddProfile: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val activeDevice = remember(matchedDevices, selectedProfileId) {
        matchedDevices.firstOrNull { it.profile.id == selectedProfileId }
            ?: matchedDevices.firstOrNull()
    }

    val summary = remember(logsForSelectedProfile, activeDevice, selectedRange) {
        if (activeDevice == null) {
            null
        } else {
            BatteryAnalyticsCalculator.compute(
                logs = logsForSelectedProfile,
                model = activeDevice.profile.model,
                range = selectedRange,
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_battery_analytics),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (activeDevice == null || summary == null) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.analytics_no_profile_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.analytics_no_profile_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAddProfile,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("analytics_add_profile_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_first_profile))
                    }
                }
                return@Box
            }

            val profile = activeDevice.profile
            val model = profile.model

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Device Profile Switcher Strip
                if (matchedDevices.size > 1) {
                    item(key = "device_switcher") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(
                                items = matchedDevices,
                                key = { it.profile.id }
                            ) { item ->
                                val isSelected = item.profile.id == profile.id
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onSelectProfile(item.profile.id) }
                                        .testTag("analytics_device_chip_${item.profile.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DeviceModelIcon(
                                            model = item.profile.model,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            ventTint = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainer
                                            },
                                            iconSize = 22.dp
                                        )
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.isLive || item.isBluetoothConnected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(CapodMint)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Time Range Segmented Selector (1H / 6H / 24H / 7D)
                item(key = "range_selector") {
                    val ranges = AnalyticsTimeRange.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ranges.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = selectedRange == range,
                                onClick = { onSelectRange(range) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ranges.size
                                ),
                                modifier = Modifier.testTag("analytics_range_${range.key}")
                            ) {
                                Text(range.shortLabel)
                            }
                        }
                    }
                }

                // Primary Interactive Battery Drain Curve Card
                item(key = "drain_curve_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeDevice.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${model.displayName} · ${summary.sampleCount} samples",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (activeDevice.isLive || activeDevice.isBluetoothConnected) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = if (activeDevice.isLive) "Live BLE" else "Connected",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            InteractiveBatteryDrainChart(
                                logs = logsForSelectedProfile,
                                formFactor = model.formFactor,
                                timeRange = selectedRange
                            )
                        }
                    }
                }

                // 2x2 Key Drain Telemetry Metrics Grid
                item(key = "kpi_grid") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnalyticsMetricTile(
                                icon = Icons.Rounded.TrendingDown,
                                label = stringResource(R.string.analytics_metric_drain_rate),
                                value = summary.drainRatePerHour?.let {
                                    String.format(java.util.Locale.getDefault(), "%.1f%%/hr", it)
                                } ?: "--",
                                subtitle = if (summary.totalPercentDrained > 0) {
                                    "-${summary.totalPercentDrained}% total drain"
                                } else {
                                    stringResource(R.string.analytics_metric_no_drain)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsMetricTile(
                                icon = Icons.Rounded.Schedule,
                                label = stringResource(R.string.analytics_metric_est_runtime),
                                value = summary.estimatedMinutesRemaining?.let {
                                    formatDurationMinutes(it)
                                } ?: "--",
                                subtitle = if (summary.drainRatePerHour != null) {
                                    "Based on observed drain"
                                } else {
                                    "Model rated estimate"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnalyticsMetricTile(
                                icon = Icons.Rounded.BatteryChargingFull,
                                label = stringResource(R.string.analytics_metric_charge_gain),
                                value = "+${summary.totalPercentCharged}%",
                                subtitle = "Recovered in ${selectedRange.shortLabel}",
                                modifier = Modifier.weight(1f)
                            )
                            if (model.formFactor == PodFormFactor.DUAL_POD) {
                                AnalyticsMetricTile(
                                    icon = Icons.Rounded.Balance,
                                    label = stringResource(R.string.analytics_metric_lr_delta),
                                    value = summary.averageLrImbalance?.let {
                                        "${it.roundToInt()}%"
                                    } ?: "--",
                                    subtitle = summary.fastestDrainingSide?.let { side ->
                                        if (side == "Balanced") "Balanced wear" else "$side drains faster"
                                    } ?: "Avg L/R spread",
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                AnalyticsMetricTile(
                                    icon = Icons.Rounded.Bolt,
                                    label = stringResource(R.string.analytics_metric_samples),
                                    value = "${summary.sampleCount}",
                                    subtitle = "Logged in ${selectedRange.shortLabel}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Net Drain vs Charge Buckets Diverging Bar Chart Card
                item(key = "drain_buckets_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.analytics_net_change_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.analytics_net_change_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            NetDrainChargeBarChart(buckets = summary.buckets)
                        }
                    }
                }

                item(key = "analytics_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
