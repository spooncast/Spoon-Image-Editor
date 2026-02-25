package com.spoonlabs.crop.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val AccentColor = Color(0xFFF06B24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleSlider(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 5f,
) {
    val percentage = ((scale / minScale - 1f) / (maxScale / minScale - 1f) * 100f).roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Percentage text
        Text(
            text = String.format(Locale.US, "%d%%", percentage),
            color = if (scale > minScale) AccentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        // Slider with tick marks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Tick marks background
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 26.dp),
            ) {
                val tickCount = 51
                val spacing = size.width / (tickCount - 1)
                for (i in 0 until tickCount) {
                    val isMajor = i % 10 == 0
                    val isMid = i % 5 == 0
                    val tickHeight = when {
                        isMajor -> 12f
                        isMid -> 8f
                        else -> 4f
                    }
                    val color = when {
                        isMajor -> Color.White.copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.25f)
                    }
                    val x = i * spacing
                    drawLine(
                        color = color,
                        start = Offset(x, size.height / 2 - tickHeight / 2),
                        end = Offset(x, size.height / 2 + tickHeight / 2),
                        strokeWidth = if (isMajor) 1.5f else 1f,
                    )
                }
            }

            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = minScale..maxScale,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = AccentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}
