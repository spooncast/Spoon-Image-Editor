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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spoonlabs.imageeditor.ImageEditConfig
import com.spoonlabs.imageeditor.ImageEditContract
import com.spoonlabs.imageeditor.ImageEditResult
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

@Composable
private fun SampleScreen() {
    val context = LocalContext.current
    val brandColor = Color(0xFFF06B24)

    var editedUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(ImageEditContract()) { result ->
        when (result) {
            is ImageEditResult.Success -> {
                editedUri = result.outputUri
                errorMessage = null
            }
            is ImageEditResult.Error -> errorMessage = result.message
            is ImageEditResult.Cancelled -> Unit
        }
    }

    fun launchEditor(uri: Uri) {
        val outputDir = File(context.filesDir, "crop_output").also { it.mkdirs() }
        val outputFile = File(outputDir, "edited_${System.currentTimeMillis()}.jpg")
        cropLauncher.launch(
            ImageEditConfig(
                sourceUri = uri,
                outputUri = Uri.fromFile(outputFile),
            ),
        )
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            launchEditor(uri)
        }
    }

    fun pickImage() {
        pickMedia.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Color.Black),
    ) {
        // 편집된 이미지 — 배경으로 가득 차게
        editedUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Edited image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // 오버레이 UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = if (editedUri == null) Arrangement.Center else Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (editedUri == null) {
                // 초기 상태
                Text(
                    text = "Spoon Image Editor",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = brandColor,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { pickImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                ) {
                    Text("Edit Image", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // 결과 상태 — 하단 버튼
                Button(
                    onClick = { pickImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .padding(bottom = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                ) {
                    Text("Edit Again", fontWeight = FontWeight.SemiBold)
                }
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: $msg",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}
