package com.melodybook.player.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.melodybook.player.orbit.OrbitConfig
import com.melodybook.player.orbit.OrbitSwitchRule
import com.melodybook.player.orbit.OrbitTimeMode
import com.melodybook.player.ui.theme.MelodyTokens
import kotlin.math.roundToInt

/** 日月轨道的设置与调试入口。预览的暂停 / 继续 / 重置也在这里。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrbitSheet(
    config: OrbitConfig,
    previewRunning: Boolean,
    onConfig: (OrbitConfig) -> Unit,
    onTogglePreview: () -> Unit,
    onResetPreview: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    MelodySheet("日月轨道", onDismiss) {
        SwitchRow("在唱片外围显示日月轨道", config.enabled) { onConfig(config.copy(enabled = it)) }

        SectionLabel("时间来源")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            OrbitTimeMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = config.mode == m,
                    onClick = { onConfig(config.copy(mode = m)) },
                    shape = SegmentedButtonDefaults.itemShape(i, OrbitTimeMode.entries.size),
                ) { Text(m.label) }
            }
        }
        Text(
            when (config.mode) {
                OrbitTimeMode.TRACK_PROGRESS -> "一首歌走完一整圈，暂停停住，拖动进度条立即跳转。"
                OrbitTimeMode.REAL_TIME -> "24 小时一圈，00:00 在下、12:00 在上。简化映射，不代表真实日出日落。与播放无关。"
                OrbitTimeMode.PREVIEW -> "按固定周期自转，调试用，不影响真实播放。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (config.mode == OrbitTimeMode.PREVIEW) {
            SectionLabel("预览周期")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OrbitConfig.PREVIEW_PERIODS_SEC.forEach { sec ->
                    FilterChip(
                        selected = config.previewPeriodSec == sec,
                        onClick = { onConfig(config.copy(previewPeriodSec = sec)) },
                        label = { Text("$sec 秒") },
                    )
                }
            }
            Spacer(Modifier.height(spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedButton(onClick = onTogglePreview) { Text(if (previewRunning) "暂停" else "继续") }
                OutlinedButton(onClick = onResetPreview) { Text("重置") }
            }
        }

        if (config.mode != OrbitTimeMode.REAL_TIME) {
            OrbitSlider("起始角度", config.startDeg, 0f..355f, 70, "${config.startDeg.roundToInt()}°") {
                onConfig(config.copy(startDeg = it))
            }
        }

        SwitchRow("逆时针运行", config.reverse) { onConfig(config.copy(reverse = it)) }

        SectionLabel("日月切换")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OrbitSwitchRule.entries.forEach { r ->
                FilterChip(
                    selected = config.switchRule == r,
                    onClick = { onConfig(config.copy(switchRule = r)) },
                    label = { Text(r.label) },
                )
            }
        }

        OrbitSlider(
            "轨道大小", config.scale, OrbitConfig.SCALE_MIN..OrbitConfig.SCALE_MAX, 0,
            "${(config.scale * 100).roundToInt()}%",
        ) { onConfig(config.copy(scale = it)) }

        SwitchRow("显示刻度", config.showTicks) { onConfig(config.copy(showTicks = it)) }
    }
}

/** 拖动时只改本地值，松手才写入设置。 */
@Composable
private fun OrbitSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    text: String,
    onCommit: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(top = MelodyTokens.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local) },
            valueRange = range,
            steps = steps,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
