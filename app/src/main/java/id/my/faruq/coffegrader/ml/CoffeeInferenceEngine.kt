package id.my.faruq.coffegrader.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazy singleton TFLite inference engine untuk model YOLO11n-seg.
 *
 * - Interpreter dibuat sekali (lazy) dan di-serialize via Mutex agar thread-safe.
 * - Preprocess: resize → [1,640,640,3] FLOAT32 (÷255f) tanpa stretch
 *   (letterbox dengan padding hitam jika rasio gambar tidak 1:1).
 * - Output 0: [1,57,8400] untuk deteksi.
 * - Output 1: [1,160,160,32] proto mask (dialokasikan tapi tidak dipakai grading).
 */
@Singleton
class CoffeeInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "CoffeeInferenceEngine"
    private val mutex = Mutex()

    private var interpreter: Interpreter? = null

    // -------------------------------------------------------------------------
    // Inisialisasi interpreter (lazy, dipanggil pertama kali saat inferensi)
    // -------------------------------------------------------------------------

    private fun loadInterpreter(): Interpreter {
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        val assetFd = context.assets.openFd(ModelConfig.MODEL_FILE_NAME)
        val inputStream = FileInputStream(assetFd.fileDescriptor)
        val fileChannel = inputStream.channel
        val mappedBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFd.startOffset,
            assetFd.declaredLength,
        )
        fileChannel.close()
        inputStream.close()
        return Interpreter(mappedBuffer, options)
    }

    private fun getOrCreateInterpreter(): Interpreter {
        if (interpreter == null) {
            interpreter = loadInterpreter()
            Log.d(TAG, "Interpreter loaded: ${ModelConfig.MODEL_FILE_NAME}")
        }
        return interpreter!!
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Jalankan inferensi di coroutine yang memanggil fungsi ini
     * (caller WAJIB pakai Dispatchers.Default atau IO — lihat CameraScreen).
     *
     * @return Result<List<DetectionDto>> — failure jika model gagal load / OOM.
     */
    suspend fun runInference(bitmap: Bitmap): Result<List<DetectionDto>> = mutex.withLock {
        return try {
            val interp = getOrCreateInterpreter()

            // --- Preprocess: letterbox → ByteBuffer FLOAT32 ---
            val inputBuffer = preprocessBitmap(bitmap)

            // --- Alokasi output ---
            // Output 0: [1, 57, 8400]
            val out0 = Array(1) { Array(ModelConfig.BBOX_DIMS + CoffeeLabelTaxonomy.NUM_CLASSES + ModelConfig.MASK_COEFF_DIM) {
                FloatArray(ModelConfig.NUM_PREDICTIONS)
            }}
            // Output 1: [1, 160, 160, 32] — proto masks
            val out1 = Array(1) { Array(ModelConfig.PROTO_SIZE) { Array(ModelConfig.PROTO_SIZE) {
                FloatArray(ModelConfig.MASK_COEFF_DIM)
            }}}

            val outputs = mapOf(0 to out0, 1 to out1)

            interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

            // --- Ratakan out0 untuk Decoder ---
            val numChannels = ModelConfig.BBOX_DIMS + CoffeeLabelTaxonomy.NUM_CLASSES + ModelConfig.MASK_COEFF_DIM
            val flat = FloatArray(numChannels * ModelConfig.NUM_PREDICTIONS)
            for (c in 0 until numChannels) {
                for (p in 0 until ModelConfig.NUM_PREDICTIONS) {
                    flat[c * ModelConfig.NUM_PREDICTIONS + p] = out0[0][c][p]
                }
            }

            val detections = Yolo11SegDecoder.decode(flat)
            Result.success(detections)

        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM saat inferensi — coba kurangi resolusi input", oom)
            interpreter?.close(); interpreter = null
            Result.failure(oom)
        } catch (e: Exception) {
            Log.e(TAG, "Inferensi gagal", e)
            Result.failure(e)
        }
    }

    /** Tutup interpreter jika tidak dibutuhkan lagi (mis. saat Application destroy). */
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    // -------------------------------------------------------------------------
    // Preprocess: letterbox + normalize
    // -------------------------------------------------------------------------

    /**
     * Letterbox resize: pertahankan rasio aspek, padding hitam di sisi pendek.
     * Output ByteBuffer: FLOAT32, shape [1, INPUT_SIZE, INPUT_SIZE, 3], pixel/255f.
     */
    private fun preprocessBitmap(src: Bitmap): ByteBuffer {
        val size = ModelConfig.INPUT_SIZE

        // Hitung skala letterbox
        val scale = minOf(size.toFloat() / src.width, size.toFloat() / src.height)
        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()
        val padX = (size - newW) / 2
        val padY = (size - newH) / 2

        // Canvas hitam 640×640
        val letterboxed = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(letterboxed)
        canvas.drawColor(android.graphics.Color.BLACK)

        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        canvas.drawBitmap(scaled, padX.toFloat(), padY.toFloat(), null)
        if (scaled != src) scaled.recycle()

        // Tulis ke ByteBuffer FLOAT32
        val buf = ByteBuffer.allocateDirect(1 * size * size * 3 * 4)
            .order(ByteOrder.nativeOrder())
        buf.rewind()

        val pixels = IntArray(size * size)
        letterboxed.getPixels(pixels, 0, size, 0, 0, size, size)
        letterboxed.recycle()

        for (px in pixels) {
            buf.putFloat(((px shr 16) and 0xFF) / ModelConfig.PIXEL_NORM) // R
            buf.putFloat(((px shr 8)  and 0xFF) / ModelConfig.PIXEL_NORM) // G
            buf.putFloat((px          and 0xFF) / ModelConfig.PIXEL_NORM) // B
        }
        buf.rewind()
        return buf
    }
}
