package com.spoonlabs.crop.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val AccentColor = Color(0xFFF06B24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Brightness value text
        val percentage = (brightness * 100).roundToInt()
        Text(
            text = String.format(Locale.US, "%+d%%", percentage),
            color = if (brightness != 0f) AccentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        // Brightness slider with tick marks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Reset button
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Reset",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Slider with tick marks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 10.dp),
                ) {
                    val tickCount = 21
                    val spacing = size.width / (tickCount - 1)
                    for (i in 0 until tickCount) {
                        val isMajor = i % 5 == 0
                        val isCenter = i == 10
                        val isMid = i % 2 == 0
                        val tickHeight = when {
                            isCenter -> 14f
                            isMajor -> 12f
                            isMid -> 8f
                            else -> 4f
                        }
                        val color = when {
                            isCenter -> Color.White.copy(alpha = 0.8f)
                            isMajor -> Color.White.copy(alpha = 0.6f)
                            else -> Color.White.copy(alpha = 0.25f)
                        }
                        val x = i * spacing
                        drawLine(
                            color = color,
                            start = Offset(x, size.height / 2 - tickHeight / 2),
                            end = Offset(x, size.height / 2 + tickHeight / 2),
                            strokeWidth = if (isCenter || isMajor) 1.5f else 1f,
                        )
                    }
                }

                Slider(
                    value = brightness,
                    onValueChange = { onBrightnessChange((it * 20f).roundToInt() / 20f) },
                    valueRange = -0.5f..0.5f,
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
}
