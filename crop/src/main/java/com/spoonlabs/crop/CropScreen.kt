package com.spoonlabs.crop

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoonlabs.crop.component.AspectRatio
import com.spoonlabs.crop.component.AspectRatioSelector
import com.spoonlabs.crop.component.CropArea
import com.spoonlabs.crop.component.RotationSlider
import com.spoonlabs.crop.component.ScaleSlider

private val AccentColor = Color(0xFFF06B24)
private val SurfaceColor = Color(0xFF1A1A1A)
private val DividerColor = Color.White.copy(alpha = 0.1f)

private enum class CropTab(val label: String) {
    CROP("Crop"),
    ROTATE("Rotate"),
    SCALE("Scale"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CropScreen(
    bitmap: Bitmap,
    aspectRatioX: Float?,
    aspectRatioY: Float?,
    onConfirm: (cropRect: RectF, rotationDegrees: Float) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(CropTab.CROP) }

    // Crop tab state
    var selectedAspectRatio by remember {
        mutableStateOf(
            if (aspectRatioX != null && aspectRatioY != null) {
                AspectRatio.entries.find { it.x == aspectRatioX && it.y == aspectRatioY }
                    ?: AspectRatio.ORIGINAL
            } else {
                AspectRatio.ORIGINAL
            }
        )
    }

    // Rotate tab state
    var fineRotation by remember { mutableFloatStateOf(0f) }
    var rotation90 by remember { mutableIntStateOf(0) }
    val totalRotation = rotation90 + fineRotation

    // Scale tab state
    var externalScale by remember { mutableFloatStateOf(1f) }

    // Crop rect provider
    var getCropRect by remember { mutableStateOf<(() -> RectF)?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Top App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "사진 편집",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val rect = getCropRect?.invoke() ?: return@IconButton
                    onConfirm(rect, totalRotation)
                }) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Confirm",
                        tint = AccentColor,
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black,
            ),
        )

        // Crop Area (takes remaining space)
        CropArea(
            bitmap = bitmap,
            rotationDegrees = totalRotation,
            aspectRatioX = selectedAspectRatio.x,
            aspectRatioY = selectedAspectRatio.y,
            externalScale = externalScale,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onCropStateChanged = { provider -> getCropRect = provider },
        )

        // Control Panel (changes based on selected tab)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor),
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "control_panel",
            ) { tab ->
                when (tab) {
                    CropTab.CROP -> AspectRatioSelector(
                        selected = selectedAspectRatio,
                        onSelect = { selectedAspectRatio = it },
                    )
                    CropTab.ROTATE -> RotationSlider(
                        fineRotation = fineRotation,
                        onFineRotationChange = { fineRotation = it },
                        onRotate90 = { rotation90 = (rotation90 + 90) % 360 },
                        onReset = { fineRotation = 0f },
                    )
                    CropTab.SCALE -> ScaleSlider(
                        scale = externalScale,
                        onScaleChange = { externalScale = it },
                        minScale = 1f,
                        maxScale = 5f,
                    )
                }
            }

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // Bottom Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CropTab.entries.forEach { tab ->
                    TabItem(
                        tab = tab,
                        isSelected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: CropTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.5f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            when (tab) {
                CropTab.CROP -> drawCropIcon(color)
                CropTab.ROTATE -> drawRotateIcon(color)
                CropTab.SCALE -> drawScaleIcon(color)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// Custom drawn icons (no extended icon dependency needed)
private fun DrawScope.drawCropIcon(color: Color) {
    val s = 1.5f // stroke
    val p = 3f   // padding
    // L-shaped crop lines
    // Vertical left line
    drawLine(color, Offset(p + 4f, 0f), Offset(p + 4f, size.height - p), strokeWidth = s)
    // Horizontal bottom line
    drawLine(color, Offset(0f, size.height - p - 4f), Offset(size.width, size.height - p - 4f), strokeWidth = s)
    // Vertical right line
    drawLine(color, Offset(size.width - p - 4f, p), Offset(size.width - p - 4f, size.height), strokeWidth = s)
    // Horizontal top line
    drawLine(color, Offset(p, p + 4f), Offset(size.width - p, p + 4f), strokeWidth = s)
}

private fun DrawScope.drawRotateIcon(color: Color) {
    val cx = size.width / 2
    val cy = size.height / 2
    val r = size.width / 2 - 2f
    // Draw arc (3/4 circle)
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = 1.5f),
    )
    // Arrow head at top
    drawLine(color, Offset(cx, cy - r - 3f), Offset(cx + 4f, cy - r + 1f), strokeWidth = 1.5f)
    drawLine(color, Offset(cx, cy - r - 3f), Offset(cx - 4f, cy - r + 1f), strokeWidth = 1.5f)
}

private fun DrawScope.drawScaleIcon(color: Color) {
    val s = 1.5f
    // Magnifying glass
    val cx = size.width / 2 - 2f
    val cy = size.height / 2 - 2f
    val r = size.width / 3
    drawCircle(color, r, Offset(cx, cy), style = Stroke(width = s))
    // Handle
    val hx = cx + r * 0.7f
    val hy = cy + r * 0.7f
    drawLine(color, Offset(hx, hy), Offset(size.width - 1f, size.height - 1f), strokeWidth = s)
    // Plus sign
    drawLine(color, Offset(cx - r / 2, cy), Offset(cx + r / 2, cy), strokeWidth = s)
    drawLine(color, Offset(cx, cy - r / 2), Offset(cx, cy + r / 2), strokeWidth = s)
}
