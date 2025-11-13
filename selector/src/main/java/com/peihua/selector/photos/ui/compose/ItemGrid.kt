package com.peihua.selector.photos.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fz.common.utils.dLog
import com.peihua.photopicker.R
import com.peihua.selector.data.model.Item

@Composable
fun ItemGrid(item: Item, isSelected: Boolean, isSelectMultiple: Boolean, onItemClick: (Item) -> Unit) {
    Box {
        if (item.isVideo) {
            ItemVideoGrid(item)
        } else if (item.isAudio) {
            ItemAudioGrid(item)
        } else {
            ItemPhotoGrid(item)
        }
        if (isSelectMultiple) {
            Icon(
                modifier = Modifier.align(Alignment.TopEnd),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                painter = painterResource(
                    id = if (isSelected) R.drawable.picker_ic_check_circle_filled
                    else R.drawable.picker_ic_radio_button_unchecked
                ),
                contentDescription = null
            )
        }
    }
}

@Composable
fun ItemPhotoGrid(item: Item) {
    Box {
        AsyncImage(
            modifier = Modifier.aspectRatio(1f),
            contentScale = ContentScale.Crop,
            model = item.contentUri, contentDescription = null
        )
    }
}

@Composable
fun ItemVideoGrid(item: Item) {
    Box {
        AsyncImage(model = item.videoThumbnail, contentDescription = null)
        dLog { "item>>>>$item" }
        Row(modifier = Modifier.align(Alignment.BottomStart)) {
            Icon(
                imageVector = Icons.Default.Videocam,
                tint = Color.White,
                contentDescription = null
            )
            Text(text = item.durationText, color = Color.White)
        }
    }
}

@Composable
fun ItemAudioGrid(item: Item) {
    Box {

    }
}