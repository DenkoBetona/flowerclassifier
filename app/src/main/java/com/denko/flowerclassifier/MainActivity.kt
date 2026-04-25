package com.denko.flowerclassifier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.denko.flowerclassifier.ui.theme.FlowerClassifierTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

enum class Screen { Main, Camera, History }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowerClassifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.Main) }

    when (screen) {
        Screen.Main -> MainScreen(
            onOpenCamera = { screen = Screen.Camera },
            onOpenHistory = { screen = Screen.History }
        )
        Screen.Camera -> CameraScreen(
            onCapture = { bitmap ->
                // след снимка, връщаме се на Main и показваме резултата
                capturedBitmap = bitmap
                screen = Screen.Main
            },
            onBack = { screen = Screen.Main }
        )
        Screen.History -> HistoryScreen(onBack = { screen = Screen.Main })
    }
}

// Споделено между Camera → Main (за да предадем bitmap-а след снимка)
var capturedBitmap: Bitmap? = null

@Composable
fun MainScreen(
    onOpenCamera: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val classifier = remember { FlowerClassifier(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ClassificationStore.init(context)
    }

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var predictions by remember { mutableStateOf<List<Prediction>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    // Ако имаме bitmap от камерата - класифицирай го веднага
    LaunchedEffect(Unit) {
        capturedBitmap?.let { bmp ->
            capturedBitmap = null
            currentBitmap = bmp
            runClassification(
                bitmap = bmp,
                context = context,
                classifier = classifier,
                scope = scope,
                onLoading = { loading = it },
                onResult = { predictions = it }
            )
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
            if (bitmap != null) {
                currentBitmap = bitmap
                runClassification(
                    bitmap = bitmap,
                    context = context,
                    classifier = classifier,
                    scope = scope,
                    onLoading = { loading = it },
                    onResult = { predictions = it }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Разпознаване на цветя",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenCamera) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.width(6.dp))
                Text("Снимай")
            }
            Button(onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(6.dp))
                Text("Галерия")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onOpenHistory) {
            Icon(Icons.Default.History, null)
            Spacer(Modifier.width(6.dp))
            Text("История")
        }

        Spacer(Modifier.height(16.dp))

        currentBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            predictions.forEachIndexed { index, p ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = index * 80)) +
                            slideInVertically(
                                animationSpec = tween(400, delayMillis = index * 80),
                                initialOffsetY = { it / 4 }
                            )
                ) {
                    PredictionRow(p, isTop = index == 0)
                }
            }
        }
    }
}

@Composable
fun PredictionRow(p: Prediction, isTop: Boolean) {
    val accent = colorForClass(p.className)
    val borderColor = Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTop) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(50)
                    )
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    p.className,
                    style = (if (isTop) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium).copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${"%.1f".format(p.confidence * 100)}%",
                    style = (if (isTop) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium).copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTop) 10.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                LinearProgressIndicator(
                    progress = { p.confidence },
                    modifier = Modifier.fillMaxSize(),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private fun runClassification(
    bitmap: Bitmap,
    context: android.content.Context,
    classifier: FlowerClassifier,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoading: (Boolean) -> Unit,
    onResult: (List<Prediction>) -> Unit
) {
    onLoading(true)
    scope.launch {
        val result = withContext(Dispatchers.Default) {
            classifier.classify(bitmap)
        }
        val top = result.first()

        val imagePath = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        }

        ClassificationStore.add(
            context = context,
            topClass = top.className,
            confidence = top.confidence,
            imagePath = imagePath
        )

        onResult(result)
        onLoading(false)
    }
}