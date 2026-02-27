package com.spoonlabs.imageeditor.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AspectRatio(
    val label: String,
    val x: Float?,
    val y: Float?,
) {
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_3_4("3:4", 3f, 4f),
    ORIGINAL("원본", null, null),
    RATIO_3_2("3:2", 3f, 2f),
    RATIO_16_9("16:9", 16f, 9f),
}

private val AccentColor = Color(0xFFF06B24)

@Composable
fun AspectRatioSelector(
    selected: AspectRatio,
    onSelect: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AspectRatio.entries.forEach { ratio ->
            val isSelected = ratio == selected
            RatioChip(
                label = ratio.label,
                isSelected = isSelected,
                onClick = { onSelect(ratio) },
            )
        }
    }
}

@Composable
private fun RatioChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Aspect ratio icon box
        val (boxW, boxH) = when (label) {
            "1:1" -> 20.dp to 20.dp
            "3:4" -> 18.dp to 24.dp
            "3:2" -> 24.dp to 16.dp
            "16:9" -> 26.dp to 14.dp
            else -> 22.dp to 18.dp // 원본
        }
        Box(
            modifier = Modifier
                .size(width = boxW, height = boxH)
                .background(
                    color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp),
                ),
        )

        Text(
            text = label,
            color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
