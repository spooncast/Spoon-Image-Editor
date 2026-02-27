package com.spoonlabs.imageeditor.component

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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val AccentColor = Color(0xFFF06B24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationSlider(
    fineRotation: Float,
    onFineRotationChange: (Float) -> Unit,
    onRotate90: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Angle text display
        Text(
            text = String.format(Locale.US, "%.1f°", fineRotation),
            color = if (fineRotation != 0f) AccentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        // Slider with scale marks + side buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Reset button (X icon)
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Reset rotation",
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
                // Tick marks background
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 10.dp),
                ) {
                    val tickCount = 91 // -45 to +45
                    val spacing = size.width / (tickCount - 1)
                    for (i in 0 until tickCount) {
                        val isMajor = i % 15 == 0  // every 15° (= -45, -30, -15, 0, 15, 30, 45)
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

                // Actual slider
                Slider(
                    value = fineRotation,
                    onValueChange = { onFineRotationChange(Math.round(it * 10f) / 10f) },
                    valueRange = -45f..45f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                )
            }

            // Rotate 90° button
            IconButton(
                onClick = onRotate90,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Rotate 90°",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
