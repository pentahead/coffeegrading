package id.my.faruq.coffegrader.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Color as AndroidColor
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import id.my.faruq.coffegrader.ml.AggregatedDefects
import id.my.faruq.coffegrader.ml.CoffeeInferenceEngine
import id.my.faruq.coffegrader.ml.DefectAggregator
import id.my.faruq.coffegrader.ml.GradePolicy
import id.my.faruq.coffegrader.ml.InferenceOutput
import id.my.faruq.coffegrader.ml.ModelConfig
import id.my.faruq.coffegrader.ml.SegmentationDecoder
import id.my.faruq.coffegrader.ui.scan.ScanViewModel
import id.my.faruq.coffegrader.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

val BOX_COLORS_ARGB = listOf(
    AndroidColor.rgb(255, 80,  80),
    AndroidColor.rgb(80,  200, 80),
    AndroidColor.rgb(80,  160, 255),
    AndroidColor.rgb(255, 200, 0),
    AndroidColor.rgb(200, 80,  255),
)

private data class ScanResult(
    val overlayBitmap : Bitmap,
    val aggregated    : AggregatedDefects,
    val gradeText     : String,
    val durationMs    : Long,
    val imagePath     : String?,
    val thumbnailPath : String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    sampleInfoId       : Long? = null,
    onNavigateToHome   : () -> Unit,
    onSaveAndShowDetail: (Long) -> Unit = {},
    inferenceEngine    : CoffeeInferenceEngine,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraVm: CameraViewModel = hiltViewModel()
    val scanVm  : ScanViewModel   = hiltViewModel()
    val batchName  by cameraVm.batchName.collectAsState()
    val coffeeType by cameraVm.coffeeType.collectAsState()
    val scope = rememberCoroutineScope()

    var showGuide    by remember(sampleInfoId) { mutableStateOf(sampleInfoId != null) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
    }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera    by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var previewLayoutSize by remember { mutableStateOf(IntSize.Zero) }
    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isInferring    by remember { mutableStateOf(false) }
    var scanResult     by remember { mutableStateOf<ScanResult?>(null) }
    var isSaving       by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) { if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA) }

    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    // Preview + capture memakai ViewPort yang sama agar FOV WYSIWYG
    LaunchedEffect(previewView, previewLayoutSize, hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect
        val pv = previewView ?: return@LaunchedEffect
        bindPreviewAndCapture(
            context        = context,
            lifecycleOwner = lifecycleOwner,
            previewView    = pv,
            imageCapture   = imageCapture,
            onCameraBound  = { camera = it },
        )
    }

    fun reset() { capturedBitmap = null; scanResult = null; isInferring = false; isSaving = false }

    fun runInference(bmp: Bitmap) {
        scope.launch {
            isInferring = true
            try {
                val startMs = System.currentTimeMillis()
                val result  = withContext(Dispatchers.Default) { inferenceEngine.runInference(bmp) }
                val dur     = System.currentTimeMillis() - startMs

                result
                    .onSuccess { output: InferenceOutput ->
                        val maskCoeffs = output.detections
                            .flatMap { it.maskCoeffs?.toList() ?: List(ModelConfig.MASK_COEFF_DIM) { 0f } }
                            .toFloatArray()

                        // Gambar overlay bbox + segmentasi
                        val overlay = withContext(Dispatchers.Default) {
                            SegmentationDecoder.drawWithMasks(
                                src         = bmp,
                                detections  = output.detections,
                                maskCoeffs  = maskCoeffs,
                                protoOutput = output.protoFlat,
                                letterbox   = output.letterbox,
                                boxColors   = BOX_COLORS_ARGB,
                            )
                        }

                        // Simpan gambar OVERLAY (bbox + segmentasi) ke disk
                        val (imgPath, thumbPath) = withContext(Dispatchers.IO) {
                            saveBitmapAndThumbnail(context, overlay)
                        }

                        val agg   = DefectAggregator.aggregate(output.detections)
                        val grade = GradePolicy.gradeFromScore(agg.totalScore, coffeeType)
                        scanResult = ScanResult(
                            overlayBitmap = overlay,
                            aggregated    = agg,
                            gradeText     = grade,
                            durationMs    = dur,
                            imagePath     = imgPath,
                            thumbnailPath = thumbPath,
                        )
                    }
                    .onFailure { err ->
                        android.util.Log.e("CameraScreen", "Inferensi gagal", err)
                        Toast.makeText(context, "Gagal: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
            } finally { isInferring = false }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { loadBitmapFromUri(context, uri) } ?: run {
                Toast.makeText(context, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                return@launch
            }
            reset(); capturedBitmap = bmp; runInference(bmp)
        }
    }

    Scaffold { _ ->
        if (showGuide) {
            AlertDialog(
                onDismissRequest = { showGuide = false },
                title   = { Text("Panduan Scan Biji") },
                text    = { Text("Biji kopi harus dipaparkan (tidak boleh bertumpuk) dan berjumlah 300 gram agar hasil scan akurat.") },
                confirmButton = { Button(onClick = { showGuide = false }) { Text("Oke") } }
            )
        }

        // ── Kamera ────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasPermission) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val size = coords.size
                            if (size != previewLayoutSize) previewLayoutSize = size
                        },
                    factory  = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }.also { previewView = it }
                    },
                )
            }

            if (capturedBitmap == null && hasPermission) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) { Icon(Icons.Default.PhotoLibrary, null, tint = Color.White) }

                    IconButton(
                        onClick  = {
                            capturePhotoToBitmap(context, imageCapture, previewView) { bmp ->
                                reset(); capturedBitmap = bmp; runInference(bmp)
                            }
                        },
                        modifier = Modifier.size(64.dp).background(Color(0xFF7CFF00), CircleShape)
                    ) { Icon(Icons.Default.Camera, null) }

                    if (hasFlash) {
                        IconButton(onClick = {
                            camera?.cameraControl?.enableTorch(!isFlashOn); isFlashOn = !isFlashOn
                        }) {
                            Icon(if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                null, tint = Color.White)
                        }
                    } else Spacer(Modifier.size(48.dp))
                }
            }
        }

        // ── Preview + overlay ─────────────────────────────────────────────
        capturedBitmap?.let { bmp ->
            val displayBmp = scanResult?.overlayBitmap ?: bmp

            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f))
            ) {
                Image(
                    bitmap             = displayBmp.asImageBitmap(),
                    contentDescription = "Preview",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                )

                // Spinner
                if (isInferring) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 28.dp, vertical = 20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFB7F23A))
                            Spacer(Modifier.height(10.dp))
                            Text("Mendeteksi biji...", color = Color.White,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Info bar hasil
                scanResult?.let { res ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total terdeteksi : ${res.aggregated.totalBeans}",
                                    color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text("Nilai cacat : ${"%.2f".format(res.aggregated.totalScore)}",
                                    color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(res.gradeText, color = Color(0xFFB7F23A),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Tombol
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick  = { reset() },
                        enabled  = !isInferring && !isSaving,
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White, contentColor = Color.Black),
                        border = BorderStroke(2.dp, Color(0xFFB7F23A)),
                        shape  = RoundedCornerShape(12.dp),
                    ) { Text("Kembali", fontWeight = FontWeight.SemiBold) }

                    scanResult?.let { res ->
                        Button(
                            onClick = {
                                isSaving = true
                                scanVm.finishScanFromDetections(
                                    batchName      = batchName.ifBlank { "Sampel" },
                                    aggregated     = res.aggregated,
                                    gradeText      = res.gradeText,
                                    scanDurationMs = res.durationMs,
                                    sampleInfoId   = sampleInfoId,
                                    imagePath      = res.imagePath,
                                    thumbnailPath  = res.thumbnailPath,
                                    onDone         = { historyId -> onSaveAndShowDetail(historyId) }
                                )
                            },
                            enabled = !isSaving,
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB7F23A), contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (isSaving) CircularProgressIndicator(
                                Modifier.size(18.dp), Color.Black, strokeWidth = 2.dp)
                            else Text("Simpan & Lihat Hasil", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Camera bind — Preview & ImageCapture share ViewPort (WYSIWYG)
// =============================================================================

private suspend fun bindPreviewAndCapture(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    onCameraBound: (androidx.camera.core.Camera?) -> Unit,
) {
    val provider = suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context),
        )
    }

    withContext(Dispatchers.Main) {
        suspendCoroutine { cont ->
            fun doBind() {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                imageCapture.targetRotation =
                    previewView.display?.rotation ?: Surface.ROTATION_0

                try {
                    provider.unbindAll()
                    val viewport = previewView.viewPort
                    onCameraBound(
                        if (viewport != null) {
                            val group = UseCaseGroup.Builder()
                                .setViewPort(viewport)
                                .addUseCase(preview)
                                .addUseCase(imageCapture)
                                .build()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                group,
                            )
                        } else {
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                            )
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraScreen", "bind failed", e)
                    onCameraBound(null)
                }
                cont.resume(Unit)
            }

            if (previewView.width > 0 && previewView.height > 0) {
                doBind()
            } else {
                previewView.post { doBind() }
            }
        }
    }
}

// =============================================================================
// Helpers
// =============================================================================

private suspend fun saveBitmapAndThumbnail(ctx: Context, bmp: Bitmap): Pair<String?,String?> =
    withContext(Dispatchers.IO) {
        val ts   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir  = File(ctx.filesDir, "scans").apply { if (!exists()) mkdirs() }
        val img  = File(dir, "img_$ts.jpg")
        val thmb = File(dir, "thumb_$ts.jpg")
        try {
            FileOutputStream(img).use  { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val t = BitmapUtils.scaleToMaxSide(bmp, 400)
            FileOutputStream(thmb).use { t.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (t != bmp) t.recycle()
            Pair(img.absolutePath, thmb.absolutePath)
        } catch (e: Exception) { Pair(null, null) }
    }

private fun loadBitmapFromUri(ctx: Context, uri: Uri): Bitmap? = try {
    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: return null
    val ori = ctx.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ?: ExifInterface.ORIENTATION_NORMAL
    rotateBitmap(bmp, ori)
} catch (e: Exception) { null }

private fun rotateBitmapByExif(bmp: Bitmap, path: String): Bitmap =
    rotateBitmap(bmp, ExifInterface(path)
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))

private fun rotateBitmap(src: Bitmap, ori: Int): Bitmap {
    val deg = when (ori) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return src
    }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height,
        Matrix().apply { postRotate(deg) }, true).also { if (it != src) src.recycle() }
}

private fun capturePhotoToBitmap(
    ctx: Context,
    cap: ImageCapture,
    previewView: PreviewView?,
    cb: (Bitmap) -> Unit,
) {
    previewView?.display?.rotation?.let { cap.targetRotation = it }
    val f = File(ctx.cacheDir, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg")
    cap.takePicture(
        ImageCapture.OutputFileOptions.Builder(f).build(),
        ContextCompat.getMainExecutor(ctx),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                BitmapFactory.decodeFile(f.absolutePath)?.let { cb(rotateBitmapByExif(it, f.absolutePath)) }
                    ?: Toast.makeText(ctx, "Failed to decode bitmap", Toast.LENGTH_SHORT).show()
            }
            override fun onError(e: ImageCaptureException) {
                Toast.makeText(ctx, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
