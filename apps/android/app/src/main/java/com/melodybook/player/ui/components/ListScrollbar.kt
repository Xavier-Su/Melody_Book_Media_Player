package com.melodybook.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.melodybook.player.ui.theme.MelodyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 停止滚动后多久淡出。 */
private const val HIDE_DELAY_MS = 1200L

/** 滑块最短长度，列表很长时也要好按。 */
private val MIN_THUMB = 40.dp

/**
 * 右侧纵向滚动条：滚动列表时淡入，显示当前看到的是第几到第几首；
 * 按住滑块或轨道上下拖动可以直接跳到对应位置。停下 [HIDE_DELAY_MS] 后淡出。
 *
 * 只按条目序号计算位置，不需要知道每行的高度，几千首也不卡。
 * 读屏用户直接用列表本身滚动，这里不暴露给无障碍服务。
 */
@Composable
fun ListScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val sizes = MelodyTokens.sizes
    val spacing = MelodyTokens.spacing
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var heightPx by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    // 一屏能放下全部时不需要滚动条
    val visibleCount by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }
    if (itemCount <= visibleCount) return

    // 0~1 的滚动进度：第一个可见条目的序号加上它被滚出去的比例
    val progress by remember(itemCount) {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f
            val partial = if (first.size > 0) -first.offset.toFloat() / first.size else 0f
            val scrollable = (itemCount - visibleCount).coerceAtLeast(1)
            ((first.index + partial) / scrollable).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(listState.isScrollInProgress, dragging) {
        if (listState.isScrollInProgress || dragging) {
            visible = true
        } else {
            delay(HIDE_DELAY_MS)
            visible = false
        }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "scrollbarAlpha")

    val thumbPx = with(density) {
        (heightPx * visibleCount.toFloat() / itemCount).coerceAtLeast(MIN_THUMB.toPx()).coerceAtMost(heightPx.toFloat())
    }
    val thumbTop = (heightPx - thumbPx) * progress

    fun jumpTo(y: Float) {
        // 手指落在滑块中间，体验和拖滑块一致
        val fraction = ((y - thumbPx / 2) / (heightPx - thumbPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val target = (fraction * (itemCount - visibleCount)).roundToInt().coerceIn(0, itemCount - 1)
        scope.launch { listState.scrollToItem(target) }
    }

    // 外层只负责摆放，不挂触摸处理，空白处的点击会落到下面的列表上
    Box(modifier.fillMaxHeight()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(sizes.scrollbarGutter)
                .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
                // 隐藏时不拦截触摸，避免误触；可见时点轨道或拖动都能跳转
                .pointerInput(itemCount, visibleCount, visible) {
                    if (!visible) return@pointerInput
                    detectTapGestures { jumpTo(it.y) }
                }
                .pointerInput(itemCount, visibleCount, visible) {
                    if (!visible) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = true
                            jumpTo(it.y)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        jumpTo(change.position.y)
                    }
                }
                .alpha(alpha)
                .clearAndSetSemantics { },
        ) {
            // 轨道 + 滑块，贴右边画一根细条，触摸区域比它宽
            Canvas(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = spacing.xs)
                    .fillMaxHeight()
                    .width(sizes.scrollbarTrack),
            ) {
                val corner = CornerRadius(size.width / 2)
                drawRoundRect(scheme.outlineVariant.copy(alpha = 0.5f), cornerRadius = corner)
                drawRoundRect(
                    color = if (dragging) scheme.primary else scheme.primary.copy(alpha = 0.7f),
                    topLeft = Offset(0f, thumbTop),
                    size = Size(size.width, thumbPx),
                    cornerRadius = corner,
                )
            }
        }

        // 范围提示：跟着滑块走，显示“第几到第几首 / 总数”
        if (visible) {
            val first = (listState.firstVisibleItemIndex + 1).coerceAtMost(itemCount)
            val last = (listState.firstVisibleItemIndex + visibleCount).coerceAtMost(itemCount)
            Text(
                "$first–$last / $itemCount",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, (thumbTop + thumbPx / 2).roundToInt()) }
                    .graphicsLayer {
                        this.alpha = alpha
                        translationY = -size.height / 2
                    }
                    .padding(end = sizes.scrollbarGutter)
                    .background(scheme.inverseSurface, MaterialTheme.shapes.small)
                    .padding(horizontal = spacing.sm, vertical = spacing.xs)
                    .clearAndSetSemantics { },
            )
        }
    }
}
