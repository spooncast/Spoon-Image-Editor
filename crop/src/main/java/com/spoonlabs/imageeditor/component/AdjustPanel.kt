package com.spoonlabs.imageeditor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoonlabs.imageeditor.R
import net.spooncast.designsystem.foundation.theme.SpoonTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdjustPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = SpoonTheme.colors.fillBrandDefault
    val iconColor = SpoonTheme.colors.iconFixedWhite
    val textColor = SpoonTheme.colors.textFixedWhite

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val percentage = (brightness * 100).roundToInt()
        Text(
            text = String.format(Locale.US, "%+d%%", percentage),
            color = if (brightness != 0f) brandColor else textColor.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_reset),
                    contentDescription = "Reset",
                    tint = iconColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }

            TickSlider(
                value = brightness,
                onValueChange = { onBrightnessChange((it * 20f).roundToInt() / 20f) },
                valueRange = -0.5f..0.5f,
                tickConfig = TickConfig.Brightness,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
