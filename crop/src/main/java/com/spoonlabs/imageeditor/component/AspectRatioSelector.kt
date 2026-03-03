package com.spoonlabs.imageeditor.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoonlabs.imageeditor.ImageEditorTheme
import com.spoonlabs.imageeditor.R

enum class AspectRatio(
    val label: String,
    val ratioX: Float,
    val ratioY: Float,
) {
    ORIGINAL("원본", 0f, 0f),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_4_3("4:3", 4f, 3f),
    RATIO_16_9("16:9", 16f, 9f),
    RATIO_3_2("3:2", 3f, 2f),
    RATIO_5_3("5:3", 5f, 3f),
    RATIO_5_4("5:4", 5f, 4f),
    RATIO_7_5("7:5", 7f, 5f),
    ;

    fun x(isLandscape: Boolean): Float? {
        if (this == ORIGINAL) return null
        return if (isLandscape) ratioX else ratioY
    }

    fun y(isLandscape: Boolean): Float? {
        if (this == ORIGINAL) return null
        return if (isLandscape) ratioY else ratioX
    }

    val isSymmetric: Boolean get() = ratioX == ratioY
}

@Composable
fun AspectRatioSelector(
    selected: AspectRatio,
    isLandscape: Boolean,
    onSelect: (AspectRatio) -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            OrientationToggle(
                isSelected = !isLandscape,
                isPortrait = true,
                onClick = { if (isLandscape) onToggleOrientation() },
            )
            OrientationToggle(
                isSelected = isLandscape,
                isPortrait = false,
                onClick = { if (!isLandscape) onToggleOrientation() },
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == selected
                RatioChip(
                    ratio = ratio,
                    isLandscape = isLandscape,
                    isSelected = isSelected,
                    onClick = { onSelect(ratio) },
                )
            }
        }
    }
}

@Composable
private fun OrientationToggle(
    isSelected: Boolean,
    isPortrait: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = ImageEditorTheme.primary
    val iconColor = ImageEditorTheme.onSurface
    val iconRes = if (isPortrait) R.drawable.ic_orientation_portrait else R.drawable.ic_orientation_landscape
    val tint = if (isSelected) brandColor else iconColor.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) brandColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = if (isPortrait) "세로" else "가로",
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RatioChip(
    ratio: AspectRatio,
    isLandscape: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val brandColor = ImageEditorTheme.primary
    val textColor = ImageEditorTheme.onSurface

    val label = when {
        ratio == AspectRatio.ORIGINAL -> stringResource(R.string.image_editor_original)
        ratio.isSymmetric || isLandscape -> ratio.label
        else -> "${ratio.ratioY.toInt()}:${ratio.ratioX.toInt()}"
    }

    Text(
        text = label,
        color = if (isSelected) brandColor else textColor.copy(alpha = 0.7f),
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
