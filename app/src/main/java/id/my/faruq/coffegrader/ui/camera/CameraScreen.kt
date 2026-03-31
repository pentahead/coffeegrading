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
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.hilt.navigation.compose.hiltViewModel
import id.my.faruq.coffegrader.ui.scan.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.FileOutputStream



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
    sampleInfoId: Long? = null,
    onNavigateToHome: () -> Unit,
    onSaveAndShowDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraVm: CameraViewModel = hiltViewModel()
    val scanVm: ScanViewModel = hiltViewModel()
    val batchName by cameraVm.batchName.collectAsState()
    val scope = rememberCoroutineScope()

    var showGuideDialog by remember(sampleInfoId) { mutableStateOf(sampleInfoId != null) }

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
    var isAnalyzing by remember { mutableStateOf(false) }
    var lastDetections by remember { mutableStateOf<List<YoloSegDetection>>(emptyList()) }
    var lastScanDurationMs by remember { mutableStateOf(0L) }

    // Pastikan urutan label sesuai class index di model Anda.
    // Kalau kelas model Anda lebih dari 2, sisanya akan otomatis jadi `class_{index}`.
    val labels = listOf(
        "broken",
        "foreign_matter",
        "full_black",
        "full_sour",
        "fungus",
        "good",
        "immature",
        "insect_severe",
        "insect_slight",
        "partial_black",
        "partial_sour",
        "withered",
    )
    val segmenter = remember { YoloSegmentationTflite(context = context, labels = labels) }
    DisposableEffect(Unit) {
        onDispose {
            segmenter.close()
        }
    }

    val modelBoxes = listOf(
        ModelBox(80f, 120f, 260f, 360f, "Broken Bean"),
        ModelBox(320f, 160f, 520f, 420f, "Black Bean")
    )

    fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)
        isFlashOn = !isFlashOn
    }


    Scaffold(

    ) { padding ->
        if (showGuideDialog) {
            AlertDialog(
                onDismissRequest = { showGuideDialog = false },
                title = { Text("Panduan Scan Biji") },
                text = {
                    Text(
                        "Biji kopi harus dipaparkan (tidak boleh bertumpuk) dan berjumlah 300 gram agar hasil scan akurat."
                    )
                },
                confirmButton = {
                    Button(onClick = { showGuideDialog = false }) {
                        Text("Oke")
                    }
                }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()

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
                // NOTE: bounding box hijau dummy dihapus.

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

                // ( wave animation layer)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.9f), // Beri sedikit transparansi agar estetik
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToHome) {
                    Icon(Icons.Default.Home, contentDescription = "Beranda")
                }

                IconButton(
                    onClick = {
                        capturePhotoToBitmap(
                            context = context,
                            imageCapture = imageCapture,
                            onBitmapReady = { bmp ->
                                scope.launch {
                                    isAnalyzing = true
                                    try {
                                        val scanStart = System.currentTimeMillis()
                                        val result = withContext(Dispatchers.Default) {
                                            segmenter.segment(
                                                bitmap = bmp,
                                                maxDetections = 20
                                            )
                                        }
                                        lastScanDurationMs = System.currentTimeMillis() - scanStart
                                        lastDetections = result.detections
                                        capturedBitmap = result.overlay
                                    } catch (e: Exception) {
                                        android.util.Log.e("CameraScreen", "Segmentation failed", e)
                                        capturedBitmap = bmp
                                        lastDetections = emptyList()
                                        lastScanDurationMs = 0L
                                    } finally {
                                        isAnalyzing = false
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF7CFF00), shape = CircleShape)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(Icons.Default.Camera, contentDescription = "Capture")
                    }
                }

                if (hasFlash) {
                    IconButton(onClick = { toggleFlash() }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flash"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }

        //  Overlay preview Bitmap + Simpan & Lihat Hasil
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

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { capturedBitmap = null },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            border = BorderStroke(2.dp, Color(0xFFB7F23A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tutup Preview", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val name = batchName.ifBlank { "Sampel" }
                                scope.launch {
                                    val (imagePath, thumbnailPath) = saveBitmapAndThumbnail(context, bmp)
                                    val totalBeansDetected = lastDetections.size
                                    val classIndices = lastDetections.map { it.classIndex }
                                    scanVm.finishScanFromMl(
                                        batchName = name,
                                        totalBeans = totalBeansDetected,
                                        detectedClassIndices = classIndices,
                                        scanDurationMs = lastScanDurationMs,
                                        sampleInfoId = sampleInfoId,
                                        imagePath = imagePath,
                                        thumbnailPath = thumbnailPath,
                                        onDone = { historyId ->
                                            onSaveAndShowDetail(historyId)
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB7F23A),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simpan & Lihat Hasil", fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
        }
    }



/** Simpan gambar penuh + thumbnail ke folder scans; return (imagePath, thumbnailPath) */
private suspend fun saveBitmapAndThumbnail(context: Context, bitmap: Bitmap): Pair<String?, String?> {
    return withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val scansDir = File(context.filesDir, "scans").apply { if (!exists()) mkdirs() }
        val imageFile = File(scansDir, "img_$timestamp.jpg")
        val thumbFile = File(scansDir, "thumb_$timestamp.jpg")
        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val maxThumb = 400
            val scale = minOf(maxThumb.toFloat() / bitmap.width, maxThumb.toFloat() / bitmap.height).coerceAtMost(1f)
            val thumbW = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val thumbH = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val thumb = Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true)
            FileOutputStream(thumbFile).use { out ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (thumb != bitmap) thumb.recycle()
            Pair(imageFile.absolutePath, thumbFile.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e("CameraScreen", "Save image failed", e)
            Pair(null, null)
        }
    }
}

/** Rotasi bitmap sesuai EXIF orientation agar tampil tegak sesuai perangkat */
private fun rotateBitmapByExif(bitmap: Bitmap, path: String): Bitmap {
    val exif = ExifInterface(path)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )
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
                var bitmap =
                    BitmapFactory.decodeFile(photoFile.absolutePath)

                if (bitmap != null) {
                    bitmap = rotateBitmapByExif(bitmap, photoFile.absolutePath)
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