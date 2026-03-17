package com.spoonlabs.imageeditor.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoonlabs.imageeditor.ImageEditorTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ZoomPanel(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = ImageEditorTheme.primary
    val textColor = ImageEditorTheme.onSurface

    Row(
        modifier = modifier.padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val displayZoom = ((zoom * 10f).roundToInt() / 10f)
        Text(
            text = String.format(Locale.US, "%.1fx", displayZoom),
            color = if (zoom != 1f) primaryColor else textColor.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 36.dp),
        )

        TickSlider(
            value = zoom,
            onValueChange = { onZoomChange((it * 10f).roundToInt() / 10f) },
            valueRange = 1f..6f,
            tickConfig = TickConfig.Zoom,
            modifier = Modifier.weight(1f).height(44.dp),
        )
    }
}
