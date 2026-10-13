package com.melodybook.player.ui.nowplaying

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.melodybook.player.ui.theme.MelodyTokens

/** 长按唱片后显示的专辑大图。点任意位置或按返回键关闭。 */
@Composable
fun AlbumArtViewer(
    artworkUri: Uri?,
    title: String,
    album: String,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val spacing = MelodyTokens.spacing
    var failed by remember(artworkUri) { mutableStateOf(artworkUri == null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "关闭专辑图片",
                onClick = onDismiss,
            )
            .systemBarsPadding()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (failed) {
                Text(
                    "这首歌没有专辑图片",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            } else {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = "专辑图片：$album",
                    contentScale = ContentScale.Fit,
                    onError = { failed = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large),
                )
            }
            Spacer(Modifier.height(spacing.lg))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                album,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
