package com.melodybook.player.ui.nowplaying

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melodybook.player.ui.components.Artwork

/**
 * 旋转唱片：保留旧版旋转 CD 的个性，但改为“唱片 + 中间封面”。
 *
 * 旧版用 ObjectAnimator，暂停后 resume 偶尔会跳角度；
 * 这里用 Animatable 从当前角度继续转，暂停时停在原位，恢复时无缝衔接。
 */
@Composable
fun SpinningDisc(
    artworkUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val start = rotation.value % 360f
                rotation.snapTo(start)
                // 20 秒一圈，按剩余角度等比例计算时长，保证匀速
                val remaining = 360f - start
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween((remaining / 360f * 20_000).toInt(), easing = LinearEasing),
                )
                rotation.snapTo(0f)
            }
        }
    }

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(24.dp, CircleShape, clip = false)
            .graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center,
    ) {
        // 唱片本体：深色底 + 细纹路 + 一道高光
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Color(0xFF16181A), r)
            val groove = Color.White.copy(alpha = 0.05f)
            var gr = r * 0.95f
            while (gr > r * 0.66f) {
                drawCircle(groove, gr, style = Stroke(width = 1.dp.toPx()))
                gr -= 3.dp.toPx()
            }
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
                radius = r,
            )
            drawCircle(scheme.primary.copy(alpha = 0.55f), r * 0.635f, style = Stroke(width = 2.dp.toPx()))
        }
        // 中间封面
        Artwork(
            uri = artworkUri,
            size = Dp.Unspecified,
            shape = CircleShape,
            modifier = Modifier.fillMaxSize(0.6f),
        )
        // 中心孔
        Canvas(Modifier.fillMaxSize(0.06f)) {
            drawCircle(scheme.surface)
        }
    }
}
