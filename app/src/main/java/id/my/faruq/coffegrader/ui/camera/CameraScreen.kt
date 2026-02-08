package id.my.faruq.coffegrader.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.material.icons.filled.Camera
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff



data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String
)
data class ModelBox(

    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String
)

data class ScreenBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String
)

private fun mapBoxesToScreen(
    modelBoxes: List<ModelBox>,
    modelWidth: Float,
    modelHeight: Float,
    canvasWidth: Float,
    canvasHeight: Float
): List<ScreenBox> {
    val scaleX = canvasWidth / modelWidth
    val scaleY = canvasHeight / modelHeight

    return modelBoxes.map { b ->
        ScreenBox(
            left = b.left * scaleX,
            top = b.top * scaleY,
            right = b.right * scaleX,
            bottom = b.bottom * scaleY,
            label = b.label
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
//    onNavigateToHistory: () -> Unit,
//    onNavigateToAbout: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }


    //cek punya flash

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted
        }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ImageCapture use case
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    //  Bitmap hasil capture
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val modelBoxes = listOf(
        ModelBox(80f, 120f, 260f, 360f, "Broken Bean"),
        ModelBox(320f, 160f, 520f, 420f, "Black Bean")
    )

    fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)
        isFlashOn = !isFlashOn
    }


    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Color.White,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick =  onNavigateToHome ) {
                        Icon(Icons.Default.Home, contentDescription = "Beranda")
                    }

                    IconButton(
                        onClick = { capturePhotoToBitmap(
                        context = context,
                        imageCapture = imageCapture,
                        onBitmapReady = { bmp ->
                            capturedBitmap = bmp
                        }
                    )},
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Color(0xFF7CFF00),
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = "Capture")
                    }

                    if (hasFlash) {
                        IconButton(onClick = { toggleFlash() }) {
                            Icon(
                                imageVector = if (isFlashOn)
                                    Icons.Filled.FlashOn
                                else
                                    Icons.Filled.FlashOff,
                                contentDescription = "Flash"
                            )
                        }
                    }


                }
            }
        }
//
//                topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text("Coffee Grader") },
//                actions = {
//                    IconButton(onClick = onNavigateToHistory) {
//                        Icon(Icons.Filled.List, contentDescription = "History")
//                    }
//                    IconButton(onClick = onNavigateToAbout) {
//                        Icon(Icons.Filled.Info, contentDescription = "About")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = {
//                    capturePhotoToBitmap(
//                        context = context,
//                        imageCapture = imageCapture,
//                        onBitmapReady = { bmp ->
//                            capturedBitmap = bmp
//                        }
//                    )
//                }
//            ) {
//                Icon(Icons.Filled.Camera, contentDescription = "Scan")
//            }
//        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {

                // 1) Kamera (background)
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )

                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // 2) Overlay deteksi (Canvas)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val screenBoxes = mapBoxesToScreen(
                        modelBoxes = modelBoxes,
                        modelWidth = 640f,
                        modelHeight = 640f,
                        canvasWidth = size.width,
                        canvasHeight = size.height
                    )

                    screenBoxes.forEach { box ->
                        drawRect(
                            color = Color.Green,
                            topLeft = androidx.compose.ui.geometry.Offset(box.left, box.top),
                            size = androidx.compose.ui.geometry.Size(
                                box.right - box.left,
                                box.bottom - box.top
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            box.label,
                            box.left,
                            box.top - 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GREEN
                                textSize = 36f
                                isFakeBoldText = true
                            }
                        )
                    }
                }

                // 3) Mask gelap di luar grid + 4) Grid putih (di atas kamera)
                val gridSizeFraction = 0.75f

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    with(this) {
                        val overlayColor = Color.Black.copy(alpha = 0.35f)
                        val sideW = maxWidth * (1 - gridSizeFraction) / 2
                        val topH = maxHeight * (1 - gridSizeFraction) / 2

                        // TOP
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topH)
                                .background(overlayColor)
                        )

                        // BOTTOM
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topH)
                                .align(Alignment.BottomCenter)
                                .background(overlayColor)
                        )

                        // LEFT
                        Box(
                            modifier = Modifier
                                .width(sideW)
                                .fillMaxHeight(gridSizeFraction)
                                .align(Alignment.CenterStart)
                                .background(overlayColor)
                        )

                        // RIGHT
                        Box(
                            modifier = Modifier
                                .width(sideW)
                                .fillMaxHeight(gridSizeFraction)
                                .align(Alignment.CenterEnd)
                                .background(overlayColor)
                        )

                        // GRID putih
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(gridSizeFraction)
                                .aspectRatio(1f)
                                .border(3.dp, Color.White, RoundedCornerShape(12.dp))
                        )
                    }
                }

                val infiniteTransition = rememberInfiniteTransition(label = "wave")

                val waveOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "waveOffset"
                )

                // (Nanti di sini kita tambahkan wave animation layer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(gridSizeFraction)
                        .aspectRatio(1f)
                        .align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.25f)
                            .offset(y = (waveOffset * 200).dp)
                            .background(
                                Color(0xFF7CFF00).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }

            } else {
                Text(
                    "Camera permission required",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }


        //  Overlay preview Bitmap (sementara)
            capturedBitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured Bitmap",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )

                    Button(
                        onClick = { capturedBitmap = null },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }


private fun capturePhotoToBitmap(
    context: Context,
    imageCapture: ImageCapture,
    onBitmapReady: (Bitmap) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions =
        ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(
                outputFileResults: ImageCapture.OutputFileResults
            ) {
                val bitmap =
                    BitmapFactory.decodeFile(photoFile.absolutePath)

                if (bitmap != null) {
                    onBitmapReady(bitmap)
                } else {
                    Toast.makeText(
                        context,
                        "Failed to decode bitmap",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    context,
                    "Capture failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}