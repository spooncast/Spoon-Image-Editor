package com.spoonlabs.imageeditor.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.spoonlabs.imageeditor.ImageEditorTheme
import kotlin.math.abs

@Immutable
data class TickStyle(
    val height: Float,
    val color: Color,
    val strokeWidth: Float,
)

@Immutable
data class TickConfig(
    val tickCount: Int,
    val centerIndex: Int,
    val isMajor: (Int) -> Boolean,
    val isMid: (Int) -> Boolean,
    val centerStyle: TickStyle,
    val majorStyle: TickStyle,
    val midStyle: TickStyle,
    val minorStyle: TickStyle,
) {
    companion object {
        val Default = TickConfig(
            tickCount = 61,
            centerIndex = 30,
            isMajor = { it % 10 == 0 },
            isMid = { it % 5 == 0 },
            centerStyle = TickStyle(56f, Color.White.copy(alpha = 0.9f), 2.5f),
            majorStyle = TickStyle(48f, Color.White.copy(alpha = 0.75f), 2f),
            midStyle = TickStyle(40f, Color.White.copy(alpha = 0.6f), 1.5f),
            minorStyle = TickStyle(36f, Color.White.copy(alpha = 0.5f), 1.2f),
        )

        val Rotation = Default
        val Brightness = Default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    tickConfig: TickConfig,
    modifier: Modifier = Modifier,
) {
    val brandColor = ImageEditorTheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
        ) {
            val spacing = size.width / (tickConfig.tickCount - 1)
            val centerY = size.height / 2f
            val halfCount = tickConfig.tickCount / 2f

            for (i in 0 until tickConfig.tickCount) {
                val isCenter = i == tickConfig.centerIndex
                val isMajor = !isCenter && tickConfig.isMajor(i)
                val isMid = !isCenter && !isMajor && tickConfig.isMid(i)

                val style = when {
                    isCenter -> tickConfig.centerStyle
                    isMajor -> tickConfig.majorStyle
                    isMid -> tickConfig.midStyle
                    else -> tickConfig.minorStyle
                }

                val distFromCenter = abs(i - tickConfig.centerIndex) / halfCount
                val fadeAlpha = (1f - distFromCenter * 0.5f).coerceIn(0.15f, 1f)
                val fadedColor = style.color.copy(alpha = style.color.alpha * fadeAlpha)

                val x = i * spacing
                val halfH = style.height / 2f
                drawLine(
                    color = fadedColor,
                    start = Offset(x, centerY - halfH),
                    end = Offset(x, centerY + halfH),
                    strokeWidth = style.strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = brandColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    colors = SliderDefaults.colors(thumbColor = brandColor),
                    thumbSize = DpSize(20.dp, 20.dp),
                )
            },
        )
    }
}
