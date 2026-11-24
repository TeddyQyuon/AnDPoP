// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.AndpopIcons
import me.kavishdevar.librepods.presentation.theme.AndpopType
import me.kavishdevar.librepods.presentation.theme.LocalAndpopPalette

/** Sentence-case section header placed above a card or group. */
@Composable
internal fun AndpopSectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable (RowScope.() -> Unit)? = null) {
    val palette = LocalAndpopPalette.current
    Row(
        modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f).semantics { heading() }, color = palette.on2, style = AndpopType.section)
        trailing?.invoke(this)
    }
}

/** Corner shape for a row in a connected list group: 24 dp outside, 6 dp between rows. */
internal fun groupShape(index: Int, count: Int): Shape {
    val outer = 24.dp
    val inner = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(outer, outer, inner, inner)
        index == count - 1 -> RoundedCornerShape(inner, inner, outer, outer)
        else -> RoundedCornerShape(inner)
    }
}

internal class AndpopGroupScope {
    val rows = mutableListOf<@Composable (Shape) -> Unit>()

    fun row(content: @Composable (shape: Shape) -> Unit) {
        rows += content
    }
}

/** Connected list group (Android 16 settings style): rows separated by a 2 dp gap. */
@Composable
internal fun AndpopGroup(modifier: Modifier = Modifier, content: AndpopGroupScope.() -> Unit) {
    val scope = AndpopGroupScope().apply(content)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        scope.rows.forEachIndexed { index, row -> row(groupShape(index, scope.rows.size)) }
    }
}

/** Tonal card used for one-off content such as the listening-mode control. */
@Composable
internal fun AndpopCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = LocalAndpopPalette.current.s1,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().clip(shape).background(color), content = content)
}

@Composable
private fun RowLeading(icon: ImageVector?, chip: Boolean) {
    val palette = LocalAndpopPalette.current
    if (icon == null) return
    if (chip) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(palette.pc), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = palette.onPc)
        }
    } else {
        Icon(icon, null, Modifier.size(24.dp), tint = palette.pri)
    }
}

/**
 * One list row for every AnDPoP list: leading icon, title, supporting text, value and a chevron
 * when the row opens another screen. At font scale 1.6 or more the value moves under the title.
 */
@Composable
internal fun AndpopRow(
    shape: Shape,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconChip: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val palette = LocalAndpopPalette.current
    val large = LocalDensity.current.fontScale >= 1.6f
    val tap = if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(palette.s1).then(tap)
            .alpha(if (enabled) 1f else 0.45f).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowLeading(icon, iconChip)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = palette.on, style = AndpopType.body)
            if (!supporting.isNullOrBlank()) Text(supporting, color = palette.on2, style = AndpopType.supporting)
            if (large && !value.isNullOrBlank()) Text(value, color = palette.on2, style = AndpopType.supporting)
        }
        if (!large && !value.isNullOrBlank()) {
            Text(
                value, Modifier.widthIn(max = 180.dp), color = palette.on2, style = AndpopType.value,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke(this)
        if (showChevron) {
            Icon(AndpopIcons.ChevronRight, null, Modifier.size(20.dp), tint = palette.on3)
        }
    }
}

/** Row with a trailing switch. The whole row toggles. */
@Composable
internal fun AndpopSwitchRow(
    shape: Shape,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    iconChip: Boolean = false,
    enabled: Boolean = true,
) {
    val palette = LocalAndpopPalette.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(palette.s1)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else 0.45f).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowLeading(icon, iconChip)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = palette.on, style = AndpopType.body)
            if (!supporting.isNullOrBlank()) Text(supporting, color = palette.on2, style = AndpopType.supporting)
        }
        AndpopSwitch(checked, enabled)
    }
}

/** Material 3 switch with a check in the thumb. Display only; the row handles input. */
@Composable
internal fun AndpopSwitch(checked: Boolean, enabled: Boolean = true) {
    val palette = LocalAndpopPalette.current
    val thumb: (@Composable () -> Unit)? = if (checked) {
        { Icon(AndpopIcons.Check, null, Modifier.size(SwitchDefaults.IconSize), tint = palette.pri) }
    } else {
        null
    }
    Switch(
        checked = checked,
        onCheckedChange = null,
        enabled = enabled,
        thumbContent = thumb,
        colors = SwitchDefaults.colors(
            checkedThumbColor = palette.onPri,
            checkedTrackColor = palette.pri,
            uncheckedThumbColor = palette.on3,
            uncheckedTrackColor = palette.s3,
            uncheckedBorderColor = palette.on3,
        ),
    )
}

enum class PillTone { Success, Neutral, Warning, Error }

/** Small status pill with a dot, for example “Connected”. */
@Composable
internal fun StatusPill(text: String, tone: PillTone = PillTone.Neutral, pulsing: Boolean = false) {
    val palette = LocalAndpopPalette.current
    val ink = when (tone) {
        PillTone.Success -> palette.success
        PillTone.Warning -> palette.warning
        PillTone.Error -> palette.error
        PillTone.Neutral -> palette.on2
    }
    val container = when (tone) {
        PillTone.Neutral -> palette.s3
        else -> ink.copy(alpha = if (palette.dark) 0.14f else 0.10f)
    }
    val dotAlpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "Pill pulse")
        val alpha by transition.animateFloat(1f, 0.25f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "Pill dot")
        alpha
    } else 1f
    Row(
        Modifier.clip(CircleShape).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).alpha(dotAlpha).background(ink, CircleShape))
        Text(text, color = ink, style = AndpopType.label)
    }
}

/** Inline banner for hints and warnings. */
@Composable
internal fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AndpopIcons.Info,
    warning: Boolean = false,
    action: @Composable (RowScope.() -> Unit)? = null,
) {
    val palette = LocalAndpopPalette.current
    val container = if (warning) palette.warning.copy(alpha = if (palette.dark) 0.14f else 0.10f) else palette.s2
    val ink = if (warning) palette.warning else palette.pri
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(container).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = ink)
        Text(text, Modifier.weight(1f), color = palette.on2, style = AndpopType.supporting)
        action?.invoke(this)
    }
}
