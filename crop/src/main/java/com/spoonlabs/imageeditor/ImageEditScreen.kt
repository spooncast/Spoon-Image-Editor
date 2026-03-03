package com.spoonlabs.imageeditor

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spoonlabs.imageeditor.component.AdjustPanel
import com.spoonlabs.imageeditor.component.AspectRatio
import com.spoonlabs.imageeditor.component.AspectRatioSelector
import com.spoonlabs.imageeditor.component.CropArea

private enum class ActivePanel {
    NONE, CROP, ADJUST,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditScreen(
    bitmap: Bitmap,
    onConfirm: (cropRect: RectF, rotationDegrees: Float, brightness: Float, flipHorizontal: Boolean, flipVertical: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val brandColor = ImageEditorTheme.primary
    val iconColor = ImageEditorTheme.onSurface
    val borderColor = ImageEditorTheme.outline
    val scrimColor = ImageEditorTheme.scrim
    val panelColor = Color(0xFF262626)

    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }

    BackHandler(enabled = activePanel != ActivePanel.NONE) {
        activePanel = ActivePanel.NONE
    }

    var selectedAspectRatio by remember { mutableStateOf(AspectRatio.ORIGINAL) }
    var isLandscape by remember { mutableStateOf(false) }

    var rotation90 by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }

    val is90or270 = ((rotation90 / 90f).toInt() % 2) != 0
    val effectiveRatioX: Float? = if (selectedAspectRatio == AspectRatio.ORIGINAL) {
        if (is90or270) bitmap.height.toFloat() else bitmap.width.toFloat()
    } else {
        selectedAspectRatio.x(isLandscape)
    }
    val effectiveRatioY: Float? = if (selectedAspectRatio == AspectRatio.ORIGINAL) {
        if (is90or270) bitmap.width.toFloat() else bitmap.height.toFloat()
    } else {
        selectedAspectRatio.y(isLandscape)
    }

    var getCropRect by remember { mutableStateOf<(() -> RectF)?>(null) }
    var bottomControlsHeight by remember { mutableFloatStateOf(0f) }
    var topBarHeight by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp,
                )
                .weight(1f)
        ) {
            CropArea(
                bitmap = bitmap,
                rotationDegrees = rotation90,
                aspectRatioX = effectiveRatioX,
                aspectRatioY = effectiveRatioY,
                brightness = brightness,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
                topInset = topBarHeight,
                bottomInset = if (activePanel != ActivePanel.NONE) bottomControlsHeight else 0f,
                showCropOverlay = activePanel != ActivePanel.ADJUST,
                fitImageWidth = if (is90or270) bitmap.height.toFloat() else 0f,
                fitImageHeight = if (is90or270) bitmap.width.toFloat() else 0f,
                modifier = Modifier.fillMaxSize(),
                onCropStateChanged = { provider -> getCropRect = provider },
                onImageTap = {
                    if (activePanel != ActivePanel.NONE) {
                        activePanel = ActivePanel.NONE
                    }
                },
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .onSizeChanged { topBarHeight = it.height.toFloat() }
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, "Close", tint = iconColor)
                }
                TextButton(onClick = {
                    getCropRect?.invoke()?.let { rect ->
                        onConfirm(
                            rect,
                            rotation90,
                            brightness,
                            flipHorizontal,
                            flipVertical,
                        )
                    }
                }) {
                    Text(
                        text = stringResource(R.string.image_editor_done),
                        color = brandColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomControlsHeight = it.height.toFloat() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = activePanel,
                    transitionSpec = {
                        if (targetState == ActivePanel.NONE) {
                            fadeIn() togetherWith (fadeOut() + slideOutVertically { it })
                        } else if (initialState == ActivePanel.NONE) {
                            (fadeIn() + slideInVertically { it }) togetherWith fadeOut()
                        } else {
                            fadeIn() togetherWith fadeOut()
                        } using SizeTransform(clip = false)
                    },
                    label = "bottom_panel",
                ) { panel ->
                    if (panel == ActivePanel.NONE) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(50))
                                .background(scrimColor)
                                .border(1.dp, borderColor, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TabIcon(
                                iconRes = R.drawable.ic_crop,
                                contentDescription = "Crop",
                                isSelected = false,
                                onClick = { activePanel = ActivePanel.CROP },
                            )
                            TabIcon(
                                iconRes = R.drawable.ic_rotate,
                                contentDescription = "Rotate",
                                isSelected = false,
                                onClick = {
                                    rotation90 = (rotation90 + 90f).mod(360f)
                                    if (!selectedAspectRatio.isSymmetric) {
                                        isLandscape = !isLandscape
                                    }
                                },
                            )
                            TabIcon(
                                iconRes = R.drawable.ic_brightness,
                                contentDescription = "Adjust",
                                isSelected = false,
                                onClick = { activePanel = ActivePanel.ADJUST },
                            )
                            TabIcon(
                                iconRes = R.drawable.ic_flip,
                                contentDescription = "Flip",
                                isSelected = if (is90or270) flipVertical else flipHorizontal,
                                onClick = {
                                    if (is90or270) {
                                        flipVertical = !flipVertical
                                    } else {
                                        flipHorizontal = !flipHorizontal
                                    }
                                },
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(panelColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                ),
                        ) {
                            when (panel) {
                                ActivePanel.CROP -> AspectRatioSelector(
                                    selected = selectedAspectRatio,
                                    isLandscape = isLandscape,
                                    onSelect = {
                                        selectedAspectRatio = it
                                        activePanel = ActivePanel.NONE
                                    },
                                    onToggleOrientation = { isLandscape = !isLandscape },
                                )
                                ActivePanel.ADJUST -> AdjustPanel(
                                    brightness = brightness,
                                    onBrightnessChange = { brightness = it },
                                    onReset = { brightness = 0f },
                                )
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun TabIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val brandColor = ImageEditorTheme.primary
    val iconColor = ImageEditorTheme.onSurface
    val tint = if (isSelected) brandColor else iconColor
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun createPreviewBitmap(): Bitmap {
    val w = 360
    val h = 270
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint()
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, w.toFloat(), h.toFloat(),
        intArrayOf(0xFF1A237E.toInt(), 0xFF4A148C.toInt(), 0xFFE65100.toInt()),
        null,
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    return bmp
}

@Preview(
    name = "Default (Pill Bar)",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    heightDp = 780,
    showSystemUi = true,
)
@Composable
private fun PreviewImageEditScreenDefault() {
    ImageEditScreen(
        bitmap = createPreviewBitmap(),
        onConfirm = { _, _, _, _, _ -> },
        onCancel = {},
    )
}

@Preview(
    name = "Crop Panel",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    heightDp = 780,
    showSystemUi = true,
)
@Composable
private fun PreviewImageEditScreenCropPanel() {
    ImageEditScreen(
        bitmap = createPreviewBitmap(),
        onConfirm = { _, _, _, _, _ -> },
        onCancel = {},
    )
}

@Preview(
    name = "Adjust Panel",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    heightDp = 780,
    showSystemUi = true,
)
@Composable
private fun PreviewImageEditScreenAdjustPanel() {
    ImageEditScreen(
        bitmap = createPreviewBitmap(),
        onConfirm = { _, _, _, _, _ -> },
        onCancel = {},
    )
}
