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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spoonlabs.imageeditor.component.AdjustPanel
import com.spoonlabs.imageeditor.component.AspectRatio
import com.spoonlabs.imageeditor.component.AspectRatioSelector
import com.spoonlabs.imageeditor.component.CropArea
import com.spoonlabs.imageeditor.component.RotationPanel
import net.spooncast.designsystem.foundation.theme.SpoonTheme

private enum class ActivePanel {
    NONE, CROP, ROTATE, ADJUST,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CropScreen(
    bitmap: Bitmap,
    aspectRatioX: Float?,
    aspectRatioY: Float?,
    onConfirm: (cropRect: RectF, rotationDegrees: Float, brightness: Float, flipHorizontal: Boolean, flipVertical: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val brandColor = SpoonTheme.colors.fillBrandDefault
    val iconColor = SpoonTheme.colors.iconFixedWhite
    val borderColor = SpoonTheme.colors.borderAlphaWhite200
    val scrimColor = SpoonTheme.colors.backgroundScrim200

    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }

    // 상세 패널 열린 상태에서 뒤로가기 → 기본 탭바로 복귀
    BackHandler(enabled = activePanel != ActivePanel.NONE) {
        activePanel = ActivePanel.NONE
    }

    // Crop state
    var selectedAspectRatio by remember { mutableStateOf(AspectRatio.ORIGINAL) }
    var isLandscape by remember { mutableStateOf(false) }

    // Rotate state
    var fineRotation by remember { mutableFloatStateOf(0f) }
    var rotation90 by remember { mutableFloatStateOf(0f) }
    val totalRotation = rotation90 + fineRotation

    // Adjust state
    var brightness by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }

    // ORIGINAL: bitmap 실제 비율 사용, 90° 회전 시 틀+이미지 함께 회전 (Samsung 스타일)
    // 그 외 비율: 틀 고정, 이미지만 회전 (UCrop/iOS 스타일)
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
            .systemBarsPadding()
            .background(Color.Black)
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CropArea(
                bitmap = bitmap,
                rotationDegrees = totalRotation,
                aspectRatioX = effectiveRatioX,
                aspectRatioY = effectiveRatioY,
                brightness = brightness,
                flipHorizontal = flipHorizontal,
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

            // Transparent toolbar (overlay on top of image)
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
                IconButton(onClick = {
                    getCropRect?.invoke()?.let { rect ->
                        onConfirm(
                            rect,
                            totalRotation,
                            brightness,
                            flipHorizontal,
                            false,
                        )
                    }
                }) {
                    Icon(Icons.Filled.Check, "Confirm", tint = brandColor)
                }
            }

            // Bottom floating controls (over the image)
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
                        // Pill-shaped tab bar
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
                                onClick = { activePanel = ActivePanel.ROTATE },
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
                                isSelected = flipHorizontal,
                                onClick = { flipHorizontal = !flipHorizontal },
                            )
                        }
                    } else {
                        // Detail panel in rounded box — clickable로 터치 이벤트 소비 (뒤 이미지 탭 방지)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(scrimColor)
                                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
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
                                ActivePanel.ROTATE -> RotationPanel(
                                    fineRotation = fineRotation,
                                    onFineRotationChange = { fineRotation = it },
                                    onRotate90 = {
                                        rotation90 = (rotation90 + 90f).mod(360f)
                                        // 90° 회전 시 비율 방향도 자동 swap
                                        if (!selectedAspectRatio.isSymmetric) {
                                            isLandscape = !isLandscape
                                        }
                                    },
                                    onReset = {
                                        fineRotation = 0f
                                        rotation90 = 0f
                                    },
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

        // Bottom spacer for navigation bar
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
    val brandColor = SpoonTheme.colors.fillBrandDefault
    val iconColor = SpoonTheme.colors.iconFixedWhite
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

// ─── Previews ───────────────────────────────────────────────────────────────

private fun createPreviewBitmap(): Bitmap {
    val w = 800
    val h = 600
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    // 그라데이션 배경
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
private fun PreviewCropScreenDefault() {
    SpoonTheme {
        CropScreen(
            bitmap = createPreviewBitmap(),
            aspectRatioX = null,
            aspectRatioY = null,
            onConfirm = { _, _, _, _, _ -> },
            onCancel = {},
        )
    }
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
private fun PreviewCropScreenCropPanel() {
    SpoonTheme {
        CropScreenPreviewWithPanel(ActivePanel.CROP)
    }
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
private fun PreviewCropScreenAdjustPanel() {
    SpoonTheme {
        CropScreenPreviewWithPanel(ActivePanel.ADJUST)
    }
}

/**
 * 특정 패널이 열린 상태의 CropScreen 프리뷰.
 * 직접 내부 상태를 제어하여 각 패널 UI를 확인할 수 있음.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropScreenPreviewWithPanel(initialPanel: ActivePanel) {
    val bitmap = createPreviewBitmap()
    val brandColor = SpoonTheme.colors.fillBrandDefault
    val iconColor = SpoonTheme.colors.iconFixedWhite
    val borderColor = SpoonTheme.colors.borderAlphaWhite200
    val scrimColor = SpoonTheme.colors.backgroundScrim200
    var activePanel by remember { mutableStateOf(initialPanel) }

    var selectedAspectRatio by remember { mutableStateOf(AspectRatio.ORIGINAL) }
    var isLandscape by remember { mutableStateOf(false) }
    var fineRotation by remember { mutableFloatStateOf(0f) }
    var rotation90 by remember { mutableFloatStateOf(0f) }
    val totalRotation = rotation90 + fineRotation
    var brightness by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    val is90or270Preview = ((rotation90 / 90f).toInt() % 2) != 0
    val effectiveRatioX: Float? = if (selectedAspectRatio == AspectRatio.ORIGINAL) {
        if (is90or270Preview) bitmap.height.toFloat() else bitmap.width.toFloat()
    } else {
        selectedAspectRatio.x(isLandscape)
    }
    val effectiveRatioY: Float? = if (selectedAspectRatio == AspectRatio.ORIGINAL) {
        if (is90or270Preview) bitmap.width.toFloat() else bitmap.height.toFloat()
    } else {
        selectedAspectRatio.y(isLandscape)
    }

    var bottomControlsHeight by remember { mutableFloatStateOf(0f) }
    var topBarHeight by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CropArea(
                bitmap = bitmap,
                rotationDegrees = totalRotation,
                aspectRatioX = effectiveRatioX,
                aspectRatioY = effectiveRatioY,
                brightness = brightness,
                flipHorizontal = flipHorizontal,
                topInset = topBarHeight,
                bottomInset = if (activePanel != ActivePanel.NONE) bottomControlsHeight else 0f,
                fitImageWidth = if (is90or270Preview) bitmap.height.toFloat() else 0f,
                fitImageHeight = if (is90or270Preview) bitmap.width.toFloat() else 0f,
                modifier = Modifier.fillMaxSize(),
                onImageTap = { activePanel = ActivePanel.NONE },
            )

            // Transparent toolbar
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
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Close, "Close", tint = iconColor)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Check, "Confirm", tint = brandColor)
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
                            TabIcon(R.drawable.ic_crop, "Crop", false, { activePanel = ActivePanel.CROP })
                            TabIcon(R.drawable.ic_rotate, "Rotate", false, { activePanel = ActivePanel.ROTATE })
                            TabIcon(R.drawable.ic_brightness, "Adjust", false, { activePanel = ActivePanel.ADJUST })
                            TabIcon(R.drawable.ic_flip, "Flip", flipHorizontal, { flipHorizontal = !flipHorizontal })
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(scrimColor)
                                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
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
                                ActivePanel.ROTATE -> RotationPanel(
                                    fineRotation = fineRotation,
                                    onFineRotationChange = { fineRotation = it },
                                    onRotate90 = {
                                        rotation90 = (rotation90 + 90f).mod(360f)
                                        // 90° 회전 시 비율 방향도 자동 swap
                                        if (!selectedAspectRatio.isSymmetric) {
                                            isLandscape = !isLandscape
                                        }
                                    },
                                    onReset = {
                                        fineRotation = 0f
                                        rotation90 = 0f
                                    },
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
