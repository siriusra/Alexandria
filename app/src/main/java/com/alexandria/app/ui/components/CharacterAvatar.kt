package com.alexandria.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.alexandria.app.R

const val ICON_TYPE_EMOJI = "emoji"
const val ICON_TYPE_ICON = "icon"

@Composable
fun CharacterAvatar(
    iconType: String,
    iconKey: String,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (iconType == ICON_TYPE_ICON) {
            val resId = CharacterIcons.byKey[iconKey]?.resId ?: R.drawable.ic_char_star
            Icon(
                painter = painterResource(id = resId),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.fillMaxSize().padding(size * 0.06f)
            )
        } else {
            Text(
                text = iconKey.ifBlank { "⭐" },
                textAlign = TextAlign.Center,
                fontSize = (size.value * 0.72f).sp,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
