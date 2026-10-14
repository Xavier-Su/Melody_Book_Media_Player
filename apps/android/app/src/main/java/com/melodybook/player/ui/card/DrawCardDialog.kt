package com.melodybook.player.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.melodybook.player.data.card.CardKind
import com.melodybook.player.ui.CardUiState
import com.melodybook.player.ui.theme.MelodyTokens

/** 种类名的字距，拉开一点更有题签的味道。 */
private val KIND_LETTER_SPACING = 4.sp

/**
 * 抽卡弹层：上面是卡片，中间是操作，最下面居中放“美句 / 灵签 / 星座”。
 * 美句分类和星座在设置里选，这里不再显示。
 * 农历在本机算，打开就有；正文联网，失败时卡片上给出提示，点“再抽一次”重试。
 */
@Composable
fun DrawCardDialog(
    state: CardUiState,
    onKind: (CardKind) -> Unit,
    onRedraw: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MelodyTokens.spacing
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(spacing.lg)
                .widthIn(max = MelodyTokens.sizes.discMax + spacing.xxl * 2),
        ) {
            // 矮屏幕上内容放不下时可以滚动
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.lg)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("今日抽卡", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "关闭") }
                }
                Spacer(Modifier.height(spacing.sm))
                CardFace(state)
                Spacer(Modifier.height(spacing.md))

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Button(onClick = onRedraw, enabled = !state.loading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, Modifier.size(MelodyTokens.sizes.iconSm))
                        Spacer(Modifier.size(spacing.sm))
                        Text("再抽一次")
                    }
                    OutlinedButton(onClick = onShare, enabled = state.content != null && !state.loading) {
                        Icon(Icons.Rounded.Share, contentDescription = null, Modifier.size(MelodyTokens.sizes.iconSm))
                        Spacer(Modifier.size(spacing.sm))
                        Text("分享")
                    }
                }

                Spacer(Modifier.height(spacing.lg))
                KindPicker(selected = state.kind, onSelect = onKind)
            }
        }
    }
}

/**
 * 文艺一点的种类选择：宋体式衬线字、拉开字距，三项之间一道细竖线，
 * 选中的那项用主色并在下面点一个小圆点，像落款的印。不是按钮的样子。
 */
@Composable
private fun KindPicker(selected: CardKind, onSelect: (CardKind) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val sizes = MelodyTokens.sizes
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardKind.entries.forEachIndexed { i, kind ->
            if (i > 0) {
                Box(
                    Modifier
                        .size(width = sizes.strokeThin, height = sizes.iconSm)
                        .background(scheme.outlineVariant),
                )
            }
            val isSelected = kind == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .heightIn(min = sizes.touchTarget)
                    .selectable(selected = isSelected, role = Role.Tab) { onSelect(kind) }
                    .padding(horizontal = MelodyTokens.spacing.xl),
            ) {
                Text(
                    kind.label,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = KIND_LETTER_SPACING,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MelodyTokens.spacing.xs))
                Box(
                    Modifier
                        .size(sizes.dotActive)
                        .background(if (isSelected) scheme.primary else Color.Transparent, CircleShape),
                )
            }
        }
    }
}
