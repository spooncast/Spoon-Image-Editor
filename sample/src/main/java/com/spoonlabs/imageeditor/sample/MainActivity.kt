package com.spoonlabs.imageeditor.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spoonlabs.imageeditor.CropConfig
import com.spoonlabs.imageeditor.CropContract
import com.spoonlabs.imageeditor.CropResult
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SampleScreen()
            }
        }
    }
}

private enum class RatioOption(val label: String, val x: Float?, val y: Float?) {
    FREE("Free", null, null),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_3_4("3:4", 3f, 4f),
    RATIO_16_9("16:9", 16f, 9f),
}

@Composable
private fun SampleScreen() {
    val context = LocalContext.current

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var croppedUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRatio by remember { mutableStateOf(RatioOption.FREE) }
    var pendingSourceUri by remember { mutableStateOf<Uri?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(CropContract()) { result ->
        when (result) {
            is CropResult.Success -> {
                croppedUri = result.outputUri
                errorMessage = null
            }
            is CropResult.Error -> errorMessage = result.message
            is CropResult.Cancelled -> Unit
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            sourceUri = uri
            croppedUri = null
            errorMessage = null

            val outputDir = File(context.filesDir, "crop_output").also { it.mkdirs() }
            val outputFile = File(outputDir, "cropped_${System.currentTimeMillis()}.jpg")

            cropLauncher.launch(
                CropConfig(
                    sourceUri = uri,
                    outputUri = Uri.fromFile(outputFile),
                    aspectRatioX = selectedRatio.x,
                    aspectRatioY = selectedRatio.y,
                )
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SpoonCrop",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF06B24),
            )
            Text(
                text = "Image Crop Library Sample",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Aspect Ratio Selection
            Text(
                text = "Aspect Ratio",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RatioOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedRatio == option,
                        onClick = { selectedRatio = option },
                        label = { Text(option.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF06B24)),
            ) {
                Text("Pick Image & Crop", fontWeight = FontWeight.SemiBold)
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Error: $msg", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            if (sourceUri != null || croppedUri != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sourceUri?.let { uri ->
                        ImageCard(title = "Original", uri = uri, modifier = Modifier.weight(1f))
                    }
                    croppedUri?.let { uri ->
                        ImageCard(title = "Cropped", uri = uri, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(title: String, uri: Uri, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        AsyncImage(
            model = uri,
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop,
        )
    }
}
