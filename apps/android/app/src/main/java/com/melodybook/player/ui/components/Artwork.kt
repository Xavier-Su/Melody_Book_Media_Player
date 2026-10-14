package com.melodybook.player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

/**
 * 封面图。没有封面时显示由主题色生成的渐变 + 音符，
 * 保证列表里每一行视觉重量一致（旧版是固定的播放小图标）。
 *
 * @param size 固定尺寸；传 [Dp.Unspecified] 时由 [modifier] 决定大小。
 */
@Composable
fun Artwork(
    uri: Uri?,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    var failed by remember(uri) { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .then(if (size != Dp.Unspecified) Modifier.size(size) else Modifier)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (uri == null || failed) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxSize(0.45f),
            )
        } else {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
