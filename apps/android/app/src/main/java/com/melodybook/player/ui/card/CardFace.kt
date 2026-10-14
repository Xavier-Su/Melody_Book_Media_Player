package com.melodybook.player.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.melodybook.player.data.card.CardContent
import com.melodybook.player.data.card.CardText
import com.melodybook.player.data.card.LunarInfo
import com.melodybook.player.data.card.Quote
import com.melodybook.player.ui.CardUiState
import com.melodybook.player.ui.theme.MelodyTokens

private val Soft = Color.White.copy(alpha = 0.85f)

/**
 * 卡片本体：背景图 + 上下加深的渐变遮罩 + 白字，保证任何图片上文字都看得清。
 * 顶部是日期，中间是正文（美句 / 灵签 / 星座），底部是宜忌。
 */
@Composable
internal fun CardFace(state: CardUiState) {
    val spacing = MelodyTokens.spacing
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(MaterialTheme.shapes.large)
            // 图片还没到（或加载失败）时的底色
            .background(Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.secondaryContainer))),
    ) {
        state.imageUrl?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.45f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        )

        val content = state.content
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            state.error != null -> Text(
                state.error,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(spacing.xl),
            )
            content != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                state.lunar?.let { DateHeader(it) }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (content) {
                        is CardContent.Words -> WordsBody(content.quote)
                        is CardContent.Stick -> StickBody(content)
                        is CardContent.Horoscope -> HoroscopeBody(content)
                    }
                }
                Footer(content, state.lunar)
            }
        }
    }
}

@Composable
private fun DateHeader(lunar: LunarInfo) {
    Column {
        Text(lunar.solarLine, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(lunar.lunarLine, color = Soft, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 美句带农历宜忌；星座用接口给的宜忌；灵签自己内容已经满了，不放。 */
@Composable
private fun Footer(content: CardContent, lunar: LunarInfo?) {
    val lines = when (content) {
        is CardContent.Words -> lunar?.let { listOf("宜  ${it.yi.joinToString(" ")}", "忌  ${it.ji.joinToString(" ")}") }.orEmpty()
        is CardContent.Horoscope -> listOfNotNull(content.yi, content.ji)
        is CardContent.Stick -> emptyList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(MelodyTokens.spacing.xs)) {
        lines.forEachIndexed { i, line ->
            Text(line, color = if (i == 0) Color.White else Soft, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WordsBody(quote: Quote) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            quote.text,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        if (quote.source.isNotEmpty()) {
            Spacer(Modifier.height(MelodyTokens.spacing.sm))
            Text(
                "—— ${quote.source}",
                color = Soft,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StickBody(stick: CardContent.Stick) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stick.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(MelodyTokens.spacing.sm))
        stick.poem.forEach {
            Text(it, color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(MelodyTokens.spacing.md))
        Text(
            stick.explain,
            color = Soft,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HoroscopeBody(h: CardContent.Horoscope) {
    val lucky = listOfNotNull(
        h.luckyColor?.let { "幸运色 $it" },
        h.luckyNumber?.let { "幸运数字 $it" },
        h.luckyConstellation?.let { "速配 $it" },
    ).joinToString("  ")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MelodyTokens.spacing.sm)) {
        Text(h.sign, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Text(
            CardText.stars(h.stars),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "综合运势 ${h.stars} 星" },
        )
        h.shortComment?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
        h.text?.let {
            Text(it, color = Soft, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
        if (lucky.isNotEmpty()) Text(lucky, color = Soft, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}
