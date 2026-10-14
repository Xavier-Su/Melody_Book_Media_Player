package com.melodybook.player.ui.card

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.melodybook.player.data.card.CardKind
import com.melodybook.player.data.card.QuoteCategory
import com.melodybook.player.data.card.Zodiac
import com.melodybook.player.ui.CardUiState
import com.melodybook.player.ui.theme.MelodyTokens

/**
 * 抽卡弹层：上面是卡片，下面是操作和选项。
 * 农历在本机算，打开就有；正文联网，失败时卡片上给出提示，点“再抽一次”重试。
 */
@Composable
fun DrawCardDialog(
    state: CardUiState,
    onKind: (CardKind) -> Unit,
    onCategory: (QuoteCategory) -> Unit,
    onZodiac: (Zodiac) -> Unit,
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

                Spacer(Modifier.height(spacing.md))
                ChipRow {
                    CardKind.entries.forEach { k ->
                        FilterChip(selected = state.kind == k, onClick = { onKind(k) }, label = { Text(k.label) })
                    }
                }
                when (state.kind) {
                    CardKind.QUOTE -> ChipRow {
                        QuoteCategory.entries.forEach { c ->
                            FilterChip(selected = state.category == c, onClick = { onCategory(c) }, label = { Text(c.label) })
                        }
                    }
                    CardKind.HOROSCOPE -> ChipRow {
                        Zodiac.entries.forEach { z ->
                            FilterChip(selected = state.zodiac == z, onClick = { onZodiac(z) }, label = { Text(z.label) })
                        }
                    }
                    CardKind.STICK -> Unit
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MelodyTokens.spacing.sm),
    ) { content() }
}
