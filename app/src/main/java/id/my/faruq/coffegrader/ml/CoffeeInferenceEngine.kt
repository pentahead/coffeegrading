package id.my.faruq.coffegrader.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menyimpan parameter letterbox agar koordinat bbox bisa di-unpad
 * dengan tepat saat drawing ke bitmap asli.
 *
 * @param scale   skala resize (min(640/w, 640/h))
 * @param padX    padding horizontal dalam piksel input-space (kiri & kanan)
 * @param padY    padding vertikal dalam piksel input-space (atas & bawah)
 */
data class LetterboxParams(val scale: Float, val padX: Int, val padY: Int)

@Singleton
class CoffeeInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG   = "CoffeeInferenceEngine"
    private val mutex = Mutex()
    private var interpreter: Interpreter? = null

    private fun loadInterpreter(): Interpreter {
        val opts = Interpreter.Options().apply { numThreads = 4 }
        val fd   = context.assets.openFd(ModelConfig.MODEL_FILE_NAME)
        val buf  = FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
        return Interpreter(buf, opts).also {
            Log.d(TAG, "Interpreter loaded: ${ModelConfig.MODEL_FILE_NAME}")
        }
    }

    private fun getOrCreate(): Interpreter =
        interpreter ?: loadInterpreter().also { interpreter = it }

    /**
     * Jalankan inferensi.
     * @return InferenceOutput berisi deteksi (koordinat sudah di-unpad ke bitmap asli)
     *         + proto flat untuk segmentasi.
     */
    suspend fun runInference(bitmap: Bitmap): Result<InferenceOutput> = mutex.withLock {
        return try {
            val interp = getOrCreate()

            // Preprocess + simpan params letterbox
            val (input, lbParams) = preprocessBitmap(bitmap)

            val numCh = ModelConfig.BBOX_DIMS + CoffeeLabelTaxonomy.NUM_CLASSES + ModelConfig.MASK_COEFF_DIM
            val out0  = Array(1) { Array(numCh) { FloatArray(ModelConfig.NUM_PREDICTIONS) } }
            val out1  = Array(1) { Array(ModelConfig.PROTO_SIZE) {
                Array(ModelConfig.PROTO_SIZE) { FloatArray(ModelConfig.MASK_COEFF_DIM) }
            }}

            interp.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to out0, 1 to out1))

            // Ratakan out0
            val flat0 = FloatArray(numCh * ModelConfig.NUM_PREDICTIONS)
            for (c in 0 until numCh)
                for (p in 0 until ModelConfig.NUM_PREDICTIONS)
                    flat0[c * ModelConfig.NUM_PREDICTIONS + p] = out0[0][c][p]

            // Decode — koordinat masih dalam input-space (0..640 termasuk padding)
            val rawDetections = Yolo11SegDecoder.decode(flat0)

            // Unpad + rescale koordinat ke bitmap asli
            val detections = rawDetections.map { det ->
                unpadDetection(det, bitmap, lbParams)
            }

            // Ratakan out1 → [160*160*32]
            val ps    = ModelConfig.PROTO_SIZE
            val md    = ModelConfig.MASK_COEFF_DIM
            val flat1 = FloatArray(ps * ps * md)
            for (h in 0 until ps)
                for (w in 0 until ps)
                    for (c in 0 until md)
                        flat1[(h * ps + w) * md + c] = out1[0][h][w][c]

            Result.success(InferenceOutput(detections = detections, protoFlat = flat1,
                letterbox = lbParams, srcWidth = bitmap.width, srcHeight = bitmap.height))

        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM", oom); interpreter?.close(); interpreter = null
            Result.failure(oom)
        } catch (e: Exception) {
            Log.e(TAG, "Inferensi gagal", e); Result.failure(e)
        }
    }

    fun close() { interpreter?.close(); interpreter = null }

    // ── Letterbox preprocess ──────────────────────────────────────────────────

    /**
     * Resize dengan letterbox (aspect-ratio preserved, padding hitam).
     * Return ByteBuffer FLOAT32 + LetterboxParams untuk unpad nanti.
     */
    private fun preprocessBitmap(src: Bitmap): Pair<ByteBuffer, LetterboxParams> {
        val size  = ModelConfig.INPUT_SIZE  // 640
        val scale = minOf(size.toFloat() / src.width, size.toFloat() / src.height)
        val newW  = (src.width  * scale).toInt()
        val newH  = (src.height * scale).toInt()
        // Padding: bagi dua sisi agar gambar di tengah
        val padX  = (size - newW) / 2
        val padY  = (size - newH) / 2

        val lb     = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(lb)
        canvas.drawColor(android.graphics.Color.BLACK)
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        canvas.drawBitmap(scaled, padX.toFloat(), padY.toFloat(), null)
        if (scaled != src) scaled.recycle()

        val buf    = ByteBuffer.allocateDirect(size * size * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        lb.getPixels(pixels, 0, size, 0, 0, size, size)
        lb.recycle()

        for (px in pixels) {
            buf.putFloat(((px shr 16) and 0xFF) / ModelConfig.PIXEL_NORM)
            buf.putFloat(((px shr  8) and 0xFF) / ModelConfig.PIXEL_NORM)
            buf.putFloat((px          and 0xFF) / ModelConfig.PIXEL_NORM)
        }
        buf.rewind()

        return Pair(buf, LetterboxParams(scale = scale, padX = padX, padY = padY))
    }

    // ── Unpad koordinat dari input-space → bitmap asli ────────────────────────

    /**
     * Konversi koordinat DetectionDto dari input-space (0..640, termasuk padding)
     * ke koordinat bitmap asli (0..src.width / 0..src.height).
     *
     * Rumus:
     *   xOrig = (xInput - padX) / scale
     *   yOrig = (yInput - padY) / scale
     */
    private fun unpadDetection(det: id.my.faruq.coffegrader.ml.DetectionDto,
                                src: Bitmap, lb: LetterboxParams
    ): id.my.faruq.coffegrader.ml.DetectionDto {
        fun unX(v: Float) = ((v - lb.padX) / lb.scale).coerceIn(0f, src.width.toFloat())
        fun unY(v: Float) = ((v - lb.padY) / lb.scale).coerceIn(0f, src.height.toFloat())

        val x1 = unX(det.cx - det.w / 2f)
        val y1 = unY(det.cy - det.h / 2f)
        val x2 = unX(det.cx + det.w / 2f)
        val y2 = unY(det.cy + det.h / 2f)

        return det.copy(
            cx = (x1 + x2) / 2f,
            cy = (y1 + y2) / 2f,
            w  = (x2 - x1).coerceAtLeast(1f),
            h  = (y2 - y1).coerceAtLeast(1f),
        )
    }
}
