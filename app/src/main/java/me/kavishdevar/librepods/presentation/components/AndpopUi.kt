// SPDX-License-Identifier: GPL-3.0-or-later

package me.kavishdevar.librepods.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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

/** One large page title on screen. The top bar shows its title only after this one scrolls away. */
internal class LargeTitleToken {
    var visible by mutableStateOf(true)
}

/**
 * Tracks the large titles on screen so the top bar can hide its small title while a large title
 * is visible (the prototype's collapsing title). The newest registered title belongs to the
 * screen on top of the back stack.
 */
internal class LargeTitleRegistry {
    private val tokens = mutableStateListOf<LargeTitleToken>()
    var contentTop by mutableFloatStateOf(0f)

    fun register(token: LargeTitleToken) {
        tokens.remove(token)
        tokens.add(token)
    }

    fun unregister(token: LargeTitleToken) {
        tokens.remove(token)
    }

    /** True while the current screen's large title is on screen. */
    val largeTitleShowing: Boolean get() = tokens.lastOrNull()?.visible == true
}

internal val LocalLargeTitleRegistry = staticCompositionLocalOf<LargeTitleRegistry?> { null }

/** Large page header placed at the top of detail screens. */
@Composable
internal fun AndpopPageHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val palette = LocalAndpopPalette.current
    val registry = LocalLargeTitleRegistry.current
    val token = remember { LargeTitleToken() }
    DisposableEffect(registry, token) {
        registry?.register(token)
        onDispose { registry?.unregister(token) }
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            Modifier.semantics { heading() }.onGloballyPositioned { coordinates ->
                if (registry != null) {
                    val bottom = coordinates.positionInWindow().y + coordinates.size.height
                    token.visible = bottom > registry.contentTop + coordinates.size.height / 3f
                }
            },
            color = palette.on,
            style = AndpopType.pageTitle,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = palette.on2, style = AndpopType.bodySmall)
        }
    }
}

/** Scrolling detail page: background, 16 dp margins, a large title and the page content. */
@Composable
internal fun AndpopDetailPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalAndpopPalette.current
    Column(
        modifier.fillMaxSize().background(palette.bg).verticalScroll(scrollState).padding(horizontal = 16.dp),
    ) {
        AndpopPageHeader(title, subtitle)
        content()
        Spacer(Modifier.height(32.dp))
    }
}

/** Helper text under a group, for example the press-and-hold rule. */
@Composable
internal fun AndpopFootnote(text: String, modifier: Modifier = Modifier, color: Color = LocalAndpopPalette.current.on2) {
    Text(text, modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp), color = color, style = AndpopType.bodySmall)
}

/** Outlined text field in the 2.0 colors: tonal container, 16 dp corners and primary focus ring. */
@Composable
internal fun AndpopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    supporting: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val palette = LocalAndpopPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, null, tint = palette.on3) } },
        trailingIcon = trailing,
        supportingText = supporting,
        isError = isError,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.s2,
            unfocusedContainerColor = palette.s2,
            disabledContainerColor = palette.s2,
            focusedBorderColor = palette.pri,
            unfocusedBorderColor = palette.line2,
            focusedLabelColor = palette.pri,
            unfocusedLabelColor = palette.on2,
            cursorColor = palette.pri,
            focusedTextColor = palette.on,
            unfocusedTextColor = palette.on,
            errorBorderColor = palette.error,
            errorLabelColor = palette.error,
            errorSupportingTextColor = palette.error,
            focusedSupportingTextColor = palette.on2,
            unfocusedSupportingTextColor = palette.on2,
        ),
    )
}

/** Full-width primary action, 56 dp tall, used at the end of forms. */
@Composable
internal fun AndpopPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val palette = LocalAndpopPalette.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.pri,
            contentColor = palette.onPri,
            disabledContainerColor = palette.on.copy(alpha = 0.12f),
            disabledContentColor = palette.on.copy(alpha = 0.45f),
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = AndpopType.button)
    }
}

/** Tonal (secondary) full-width action. */
@Composable
internal fun AndpopTonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val palette = LocalAndpopPalette.current
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = palette.pc,
            contentColor = palette.onPc,
            disabledContainerColor = palette.on.copy(alpha = 0.10f),
            disabledContentColor = palette.on.copy(alpha = 0.45f),
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = AndpopType.button)
    }
}

/** Text action in the primary color, at least 48 dp tall. */
@Composable
internal fun AndpopTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color? = null) {
    val palette = LocalAndpopPalette.current
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, color = if (enabled) color ?: palette.pri else palette.on3, style = AndpopType.button)
    }
}

/** Single-select radio row inside an [AndpopGroup]. */
@Composable
internal fun AndpopRadioRow(
    shape: Shape,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val palette = LocalAndpopPalette.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(palette.s1)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(20.dp).border(2.dp, if (selected) palette.pri else palette.on2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(palette.pri, CircleShape))
        }
        RowLeading(icon, chip = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = palette.on, style = AndpopType.body)
            if (!supporting.isNullOrBlank()) Text(supporting, color = palette.on2, style = AndpopType.supporting)
        }
    }
}

/** Multi-select checkbox row inside an [AndpopGroup], supporting locked visual state. */
@Composable
internal fun AndpopCheckboxRow(
    shape: Shape,
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    locked: Boolean = false,
    enabled: Boolean = true,
    checkboxModifier: Modifier = Modifier,
) {
    val palette = LocalAndpopPalette.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(palette.s1)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
            .alpha(if (enabled) 1f else 0.45f).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val boxShape = RoundedCornerShape(6.dp)
        Box(
            checkboxModifier.size(20.dp).alpha(if (locked) 0.55f else 1f).clip(boxShape)
                .then(if (checked) Modifier.background(palette.pri) else Modifier.border(2.dp, palette.on2, boxShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(AndpopIcons.Check, null, Modifier.size(15.dp), tint = palette.onPri)
        }
        RowLeading(icon, chip = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = palette.on, style = AndpopType.body)
            if (!supporting.isNullOrBlank()) Text(supporting, color = palette.on2, style = AndpopType.supporting)
        }
    }
}

/** Pill segmented picker matching the prototype (used for Accessibility speeds and Equalizer mode). */
@Composable
internal fun AndpopSegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAndpopPalette.current
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape).border(BorderStroke(1.dp, palette.line2), shape).selectableGroup(),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            if (index > 0) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(palette.line2))
            }
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .background(if (selected) palette.pc else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Icon(AndpopIcons.Check, null, Modifier.size(16.dp), tint = palette.onPc)
                }
                Text(
                    label,
                    color = if (selected) palette.onPc else palette.on,
                    style = AndpopType.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Shared slider row inside an [AndpopGroup] or [AndpopCard] with value readout and end labels. */
@Composable
internal fun AndpopSliderRow(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int = 0,
    enabled: Boolean = true,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    startLabel: String? = null,
    endLabel: String? = null,
) {
    val palette = LocalAndpopPalette.current
    Column(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = palette.on, style = AndpopType.body)
            Text(valueText, color = palette.pri, style = AndpopType.section)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (startIcon != null) Icon(startIcon, null, Modifier.size(20.dp), tint = palette.on2)
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = palette.pri,
                    activeTrackColor = palette.pri,
                    inactiveTrackColor = palette.s3,
                ),
            )
            if (endIcon != null) Icon(endIcon, null, Modifier.size(20.dp), tint = palette.on2)
        }
        if (!startLabel.isNullOrBlank() || !endLabel.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(startLabel.orEmpty(), color = palette.on3, style = AndpopType.caption)
                Text(endLabel.orEmpty(), color = palette.on3, style = AndpopType.caption)
            }
        }
    }
}
