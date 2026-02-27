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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.spooncast.designsystem.foundation.theme.SpoonTheme

/**
 * 눈금자(tick mark) 스타일을 정의하는 데이터 클래스.
 * 각 눈금 유형(center, major, mid, minor)별 높이·색상·두께를 지정한다.
 */
@Immutable
data class TickStyle(
    val height: Float,
    val color: Color,
    val strokeWidth: Float,
)

/**
 * 눈금자 설정.
 * @param tickCount 전체 눈금 개수
 * @param centerIndex 중앙 눈금 인덱스
 * @param isMajor 해당 인덱스가 major 눈금인지 판별
 * @param isMid 해당 인덱스가 mid 눈금인지 판별
 * @param centerStyle 중앙 눈금 스타일
 * @param majorStyle major 눈금 스타일
 * @param midStyle mid 눈금 스타일
 * @param minorStyle minor(기본) 눈금 스타일
 */
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
        /** 회전 슬라이더용 기본 설정: 91 ticks (-45° ~ +45°) */
        val Rotation = TickConfig(
            tickCount = 91,
            centerIndex = 45,
            isMajor = { it % 15 == 0 },
            isMid = { it % 5 == 0 },
            centerStyle = TickStyle(20f, Color.White, 2.5f),
            majorStyle = TickStyle(16f, Color.White.copy(alpha = 0.8f), 2f),
            midStyle = TickStyle(10f, Color.White.copy(alpha = 0.5f), 1.5f),
            minorStyle = TickStyle(6f, Color.White.copy(alpha = 0.35f), 1.2f),
        )

        /** 밝기 슬라이더용 설정: 41 ticks (-50% ~ +50%, 2.5% 단위) */
        val Brightness = TickConfig(
            tickCount = 41,
            centerIndex = 20,
            isMajor = { it % 10 == 0 },  // 0%, ±25%, ±50%
            isMid = { it % 5 == 0 },     // ±12.5% 등
            centerStyle = TickStyle(20f, Color.White, 2.5f),
            majorStyle = TickStyle(16f, Color.White.copy(alpha = 0.8f), 2f),
            midStyle = TickStyle(10f, Color.White.copy(alpha = 0.5f), 1.5f),
            minorStyle = TickStyle(6f, Color.White.copy(alpha = 0.35f), 1.2f),
        )
    }
}

/**
 * 눈금자(tick ruler) + Slider를 결합한 재사용 가능한 컴포넌트.
 *
 * @param value 현재 슬라이더 값
 * @param onValueChange 값 변경 콜백 (snap 처리는 호출 측에서)
 * @param valueRange 슬라이더 범위
 * @param tickConfig 눈금자 설정
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    tickConfig: TickConfig,
    modifier: Modifier = Modifier,
) {
    val brandColor = SpoonTheme.colors.fillBrandDefault

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 눈금자 Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 10.dp),
        ) {
            val spacing = size.width / (tickConfig.tickCount - 1)

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

                val x = i * spacing
                drawLine(
                    color = style.color,
                    start = Offset(x, size.height / 2 - style.height / 2),
                    end = Offset(x, size.height / 2 + style.height / 2),
                    strokeWidth = style.strokeWidth,
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
        )
    }
}
