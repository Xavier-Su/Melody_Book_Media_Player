package com.melodybook.player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.ui.graphics.vector.ImageVector
import com.melodybook.player.data.PlayMode

val PlayMode.icon: ImageVector
    get() = when (this) {
        PlayMode.LIST_LOOP -> Icons.Rounded.Repeat
        PlayMode.SINGLE_LOOP -> Icons.Rounded.RepeatOne
        PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
    }
