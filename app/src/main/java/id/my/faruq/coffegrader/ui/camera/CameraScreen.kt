package id.my.faruq.coffegrader.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.my.faruq.coffegrader.ml.CoffeeInferenceEngine
import id.my.faruq.coffegrader.ml.DefectAggregator
import id.my.faruq.coffegrader.ml.GradePolicy
import id.my.faruq.coffegrader.ui.scan.ScanViewModel
import id.my.faruq.coffegrader.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    sampleInfoId: Long? = null,
    onNavigateToHome: () -> Unit,
    onSaveAndShowDetail: (Long) -> Unit = {},
    // Engine di-inject sebagai parameter agar testable; di produksi diambil via hiltViewModel/entryPoint
    inferenceEngine: CoffeeInferenceEngine,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraVm: CameraViewModel = hiltViewModel()
    val scanVm: ScanViewModel = hiltViewModel()
    val batchName by cameraVm.batchName.collectAsState()
    val coffeeType by cameraVm.coffeeType.collectAsState()
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

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

    // [BARU] State inferensi — tidak mengubah UI yang ada, hanya loading indicator
    var isInferring by remember { mutableStateOf(false) }

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

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImageFromGallery = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmapFromUri(context, uri)
            }
            if (bitmap == null) {
                Toast.makeText(context, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show()
                return@launch
            }
            capturedBitmap = bitmap
        }
    }

    fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)
        isFlashOn = !isFlashOn
    }

    Scaffold { padding ->
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
                    Button(onClick = { showGuideDialog = false }) { Text("Oke") }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {

                // 1) Kamera (background) — tidak diubah
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
                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner, cameraSelector, preview, imageCapture
                                )
                            } catch (exc: Exception) {
                                android.util.Log.e("CameraScreen", "CameraX bind failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // 2) Grid overlay — tidak diubah (diasumsikan kode grid ada di sini)

                // 3) Row tombol bawah kamera — tidak diubah
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            pickImageFromGallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri")
                    }

                    IconButton(
                        onClick = {
                            capturePhotoToBitmap(context, imageCapture) { bmp ->
                                capturedBitmap = bmp
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF7CFF00), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = "Capture")
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

        // Overlay preview Bitmap + Simpan & Lihat Hasil — struktur UI tidak diubah
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

                // [BARU] Loading indicator saat inferensi berjalan
                if (isInferring) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFB7F23A)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { capturedBitmap = null },
                        enabled = !isInferring,
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
                            val startMs = System.currentTimeMillis()

                            scope.launch {
                                isInferring = true
                                try {
                                    // [BARU] Simpan gambar dulu (IO thread)
                                    val (imagePath, thumbnailPath) = withContext(Dispatchers.IO) {
                                        saveBitmapAndThumbnail(context, bmp)
                                    }

                                    // [BARU] Inferensi di Default thread (bukan Main)
                                    val inferenceResult = withContext(Dispatchers.Default) {
                                        inferenceEngine.runInference(bmp)
                                    }

                                    val durationMs = System.currentTimeMillis() - startMs

                                    inferenceResult
                                        .onSuccess { detections ->
                                            val aggregated = DefectAggregator.aggregate(detections)
                                            val gradeText  = GradePolicy.gradeFromScore(
                                                aggregated.totalScore, coffeeType
                                            )
                                            // [BARU] Simpan hasil penuh ke Room via ScanViewModel
                                            scanVm.finishScanFromDetections(
                                                batchName      = name,
                                                aggregated     = aggregated,
                                                gradeText      = gradeText,
                                                scanDurationMs = durationMs,
                                                sampleInfoId   = sampleInfoId,
                                                imagePath      = imagePath,
                                                thumbnailPath  = thumbnailPath,
                                                onDone         = { historyId ->
                                                    onSaveAndShowDetail(historyId)
                                                }
                                            )
                                        }
                                        .onFailure { err ->
                                            android.util.Log.e("CameraScreen", "Inferensi gagal", err)
                                            Toast.makeText(
                                                context,
                                                "Gagal menjalankan model: ${err.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } finally {
                                    isInferring = false
                                }
                            }
                        },
                        enabled = !isInferring,
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
}


// =============================================================================
// Helper functions (tidak diubah dari versi asli)
// =============================================================================

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
            val thumb = BitmapUtils.scaleToMaxSide(bitmap, 400)
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

/** Muat bitmap dari Uri galeri + rotasi EXIF. */
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        if (exif == null) return bitmap
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } catch (e: Exception) {
        android.util.Log.e("CameraScreen", "loadBitmapFromUri failed", e)
        null
    }
}

/** Rotasi bitmap sesuai EXIF orientation. */
private fun rotateBitmapByExif(bitmap: Bitmap, path: String): Bitmap {
    val exif = ExifInterface(path)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    )
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun capturePhotoToBitmap(
    context: Context,
    imageCapture: ImageCapture,
    onBitmapReady: (Bitmap) -> Unit,
) {
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    bitmap = rotateBitmapByExif(bitmap, photoFile.absolutePath)
                    onBitmapReady(bitmap)
                } else {
                    Toast.makeText(context, "Failed to decode bitmap", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
