# Spoon Image Editor

Jetpack Compose 기반 이미지 편집 라이브러리 (Android)

## Features

- **Crop** — 자유 비율 / 1:1 / 4:3 / 3:4 / 16:9 / 9:16 비율 크롭
- **Rotate** — 90° 회전 + 미세 회전 슬라이더 (±45°)
- **Brightness** — 밝기 조절 (-50% ~ +50%)
- **Flip** — 좌우 반전

## Requirements

- Android SDK 24+
- Jetpack Compose (BOM)
- Kotlin 1.9+

## Installation

```kotlin
// settings.gradle.kts
repositories {
    maven { url = uri("https://jitpack.io") }
}

// build.gradle.kts
dependencies {
    implementation("com.spoonlabs:imageeditor:0.1.0")
}
```

## Usage

```kotlin
ImageEditScreen(
    bitmap = yourBitmap,
    onConfirm = { cropRect, rotation, brightness, flipH, flipV ->
        // 편집 결과 처리
    },
    onCancel = { finish() },
)
```

## License

Copyright © Spoon Radio
