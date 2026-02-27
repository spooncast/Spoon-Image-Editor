package com.spoonlabs.imageeditor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoonlabs.imageeditor.R
import java.util.Locale
import kotlin.math.roundToInt

private val AccentColor = Color(0xFFF06B24)

@Composable
fun RotationPanel(
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
        // 각도 표시
        Text(
            text = String.format(Locale.US, "%.1f°", fineRotation),
            color = if (fineRotation != 0f) AccentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        // 슬라이더 + 리셋 버튼 + 90° 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 리셋 버튼 (X)
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

            // 눈금자 + 슬라이더 (공통 컴포넌트)
            TickSlider(
                value = fineRotation,
                onValueChange = { onFineRotationChange((it * 2f).roundToInt() / 2f) },
                valueRange = -45f..45f,
                tickConfig = TickConfig.Rotation,
                modifier = Modifier.weight(1f),
            )

            // 90° 회전 버튼
            IconButton(
                onClick = onRotate90,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_rotate),
                    contentDescription = "Rotate 90°",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
