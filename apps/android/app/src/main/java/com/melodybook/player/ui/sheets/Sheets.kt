package com.melodybook.player.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import android.net.Uri
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import com.melodybook.player.data.AppSettings
import com.melodybook.player.data.BackgroundMode
import com.melodybook.player.data.DarkMode
import com.melodybook.player.data.PresetBackgrounds
import kotlin.math.roundToInt
import com.melodybook.player.data.ScanFilter
import com.melodybook.player.playback.SleepTimer
import com.melodybook.player.ui.theme.MelodyTokens
import com.melodybook.player.ui.theme.Skin
import com.melodybook.player.ui.theme.Skins
import com.melodybook.player.ui.theme.colorScheme
import com.melodybook.player.ui.theme.LocalIsDarkTheme

/** 所有弹层共用的外壳：统一标题样式和内边距。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MelodySheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl)
                .navigationBarsPadding()
                .padding(bottom = spacing.lg),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(spacing.lg))
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = MelodyTokens.spacing.sm, bottom = MelodyTokens.spacing.sm),
    )
}

// ------------------------------------------------------------------ 定时关闭

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimer.State,
    remainingLabel: String?,
    onPick: (Int) -> Unit,
    onAfterCurrent: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    MelodySheet("定时关闭", onDismiss) {
        if (remainingLabel != null) {
            Text(
                remainingLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.md))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            SleepTimer.PRESET_MINUTES.forEach { m ->
                FilterChip(
                    selected = false,
                    onClick = { onPick(m); onDismiss() },
                    label = { Text(minutesLabel(m)) },
                )
            }
            FilterChip(
                selected = state is SleepTimer.State.AfterCurrent,
                onClick = { onAfterCurrent(); onDismiss() },
                label = { Text("播完当前这首") },
            )
        }
        if (state !is SleepTimer.State.Off) {
            Spacer(Modifier.height(spacing.lg))
            OutlinedButton(onClick = { onCancel(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text("取消定时")
            }
        }
    }
}

private fun minutesLabel(m: Int): String = when {
    m < 60 -> "$m 分钟"
    m % 60 == 0 -> "${m / 60} 小时"
    else -> "${m / 60} 小时 ${m % 60} 分"
}

// ------------------------------------------------------------------ 扫描

@Composable
fun ScanSheet(
    current: ScanFilter,
    onScan: (ScanFilter, restoreHidden: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    var selected by remember { mutableStateOf(current) }
    var restore by remember { mutableStateOf(false) }

    MelodySheet("扫描本地音乐", onDismiss) {
        Text(
            "从系统媒体库读取，几秒内即可完成。新下载的歌曲如果没出现，稍等片刻再扫描一次。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel("扫描范围")
        Column(Modifier.selectableGroup()) {
            ScanFilter.entries.forEach { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(MelodyTokens.sizes.touchTarget)
                        .selectable(selected = f == selected, role = Role.RadioButton) { selected = f },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = f == selected, onClick = null)
                    Spacer(Modifier.width(spacing.md))
                    Text(f.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(spacing.sm))
        SwitchRow("恢复之前移除的歌曲", restore) { restore = it }
        Spacer(Modifier.height(spacing.lg))
        Button(
            onClick = { onScan(selected, restore); onDismiss() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("开始扫描") }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(MelodyTokens.sizes.touchTarget)
            .selectable(selected = checked, role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

// ------------------------------------------------------------------ 外观（变身）

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(
    settings: AppSettings,
    presets: List<PresetBackgrounds.Preset>,
    supportsDynamic: Boolean,
    onSkin: (String) -> Unit,
    onDarkMode: (DarkMode) -> Unit,
    onRandomSkin: (Boolean) -> Unit,
    onBackgroundMode: (BackgroundMode) -> Unit,
    onPreset: (String) -> Unit,
    onStrength: (Float) -> Unit,
    onRandomPreset: (Boolean) -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onPickLyricsFolder: () -> Unit,
    onClearLyricsFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    MelodySheet("变身", onDismiss) {
        SectionLabel("显示模式")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            DarkMode.entries.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = settings.darkMode == mode,
                    onClick = { onDarkMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(i, DarkMode.entries.size),
                ) { Text(mode.label) }
            }
        }

        SectionLabel("皮肤")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (supportsDynamic) {
                DynamicSwatch(selected = settings.skinId == Skins.DYNAMIC_ID) { onSkin(Skins.DYNAMIC_ID) }
            }
            Skins.all.forEach { skin ->
                SkinSwatch(skin, selected = settings.skinId == skin.id) { onSkin(skin.id) }
            }
        }
        Spacer(Modifier.height(spacing.sm))
        SwitchRow("每次启动随机换一套皮肤", settings.randomSkinOnLaunch, onRandomSkin)

        SectionLabel("背景图")
        val modes = BackgroundMode.entries.filter { it != BackgroundMode.PRESET || presets.isNotEmpty() }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = settings.backgroundMode == mode,
                    onClick = {
                        // 选“相册图片”但还没选过图，直接打开相册
                        if (mode == BackgroundMode.GALLERY && settings.wallpaperUri == null) onPickWallpaper()
                        else onBackgroundMode(mode)
                    },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(mode.label) }
            }
        }

        when (settings.backgroundMode) {
            BackgroundMode.NONE -> Unit
            BackgroundMode.PRESET -> {
                Spacer(Modifier.height(spacing.md))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(presets, key = { it.file }) { p ->
                        PresetThumb(p, selected = p.file == settings.presetBackground) { onPreset(p.file) }
                    }
                }
                SwitchRow("每次启动随机换一张", settings.randomPresetOnLaunch, onRandomPreset)
            }
            BackgroundMode.GALLERY -> {
                Spacer(Modifier.height(spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedButton(onClick = onPickWallpaper) { Text("换一张") }
                    OutlinedButton(onClick = onClearWallpaper) { Text("移除相册图片") }
                }
            }
        }

        if (settings.backgroundMode != BackgroundMode.NONE) {
            Spacer(Modifier.height(spacing.md))
            StrengthSlider(settings.backgroundStrength, onStrength)
        }

        SectionLabel("歌词")
        Text(
            settings.lyricsFolderUri?.let { "已设置歌词文件夹：${folderLabel(it)}" }
                ?: "歌曲内嵌歌词会自动读取。如果你的 .lrc 歌词放在单独的文件夹里，在这里选择它。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedButton(onClick = onPickLyricsFolder) {
                Text(if (settings.lyricsFolderUri == null) "选择歌词文件夹" else "更换文件夹")
            }
            if (settings.lyricsFolderUri != null) {
                OutlinedButton(onClick = onClearLyricsFolder) { Text("不用了") }
            }
        }
    }
}

/** 从 SAF tree Uri 里取出人能看懂的路径，例如 "primary:Music/Lyrics" -> "Music/Lyrics"。 */
private fun folderLabel(treeUri: String): String =
    Uri.decode(treeUri.substringAfterLast("/tree/", treeUri)).substringAfter(':').ifBlank { "根目录" }

/**
 * 背景浓度：拖动时只改本地值并实时预览，松手才写入设置，
 * 避免每一帧都写 DataStore。
 */
@Composable
private fun StrengthSlider(value: Float, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("背景浓度", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "${(local / 0.85f * 100).roundToInt().coerceIn(0, 100)}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = 0.05f..0.85f,
            modifier = Modifier.semantics { contentDescription = "背景浓度" },
        )
        Row {
            Text(
                "更淡（文字更清晰）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "更浓（图片更明显）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresetThumb(preset: PresetBackgrounds.Preset, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val sizes = MelodyTokens.sizes
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(sizes.presetThumbWidth)
            .clickable(role = Role.RadioButton, onClickLabel = "使用背景 ${preset.name}", onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = preset.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = sizes.presetThumbWidth, height = sizes.presetThumbHeight)
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        BorderStroke(
                            if (selected) sizes.strokeThick else sizes.strokeThin,
                            if (selected) scheme.primary else scheme.outlineVariant,
                        ),
                        MaterialTheme.shapes.small,
                    ),
            )
            if (selected) {
                Box(
                    Modifier
                        .size(sizes.icon)
                        .background(scheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "已选择",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(sizes.iconSm),
                    )
                }
            }
        }
        Spacer(Modifier.height(MelodyTokens.spacing.xs))
        Text(preset.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun SkinSwatch(skin: Skin, selected: Boolean, onClick: () -> Unit) {
    val scheme = skin.colorScheme(LocalIsDarkTheme.current)
    Swatch(
        label = skin.name,
        selected = selected,
        color = scheme.primary,
        onClick = onClick,
    )
}

@Composable
private fun DynamicSwatch(selected: Boolean, onClick: () -> Unit) {
    val s = MaterialTheme.colorScheme
    Swatch(
        label = "随壁纸",
        selected = selected,
        color = s.secondaryContainer,
        onClick = onClick,
        icon = true,
    )
}

@Composable
private fun Swatch(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    icon: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val sizes = MelodyTokens.sizes
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(sizes.touchTarget + MelodyTokens.spacing.sm)
            .clickable(role = Role.RadioButton, onClickLabel = "使用皮肤 $label", onClick = onClick),
    ) {
        // 选中态用外圈描边表示，色块本身是纯色
        Box(
            Modifier
                .size(sizes.touchTarget)
                .border(
                    BorderStroke(
                        if (selected) sizes.strokeThick else sizes.strokeThin,
                        if (selected) scheme.onSurface else scheme.outlineVariant,
                    ),
                    CircleShape,
                )
                .padding(MelodyTokens.spacing.xs)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val onColor = if (icon) scheme.onSecondaryContainer else scheme.onPrimary
            when {
                selected -> Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = onColor)
                icon -> Icon(Icons.Rounded.Palette, contentDescription = null, tint = onColor)
            }
        }
        Spacer(Modifier.height(MelodyTokens.spacing.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
