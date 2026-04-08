package id.my.faruq.coffegrader.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import id.my.faruq.coffegrader.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun sigmoid(x: Float): Float {
    return (1f / (1f + kotlin.math.exp(-x)))
}

/** Satu deteksi YOLO-seg (bbox piksel letterbox, koef mask 32-d) */
private data class Detection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val score: Float,
    val classIndex: Int,
    val coeffs: FloatArray,
)

data class YoloSegDetection(
    val classIndex: Int,
    val score: Float,
)

data class YoloSegmentationResult(
    val overlay: Bitmap,
    val detections: List<YoloSegDetection>,
)

/**
 * Loader + decoder untuk YOLOv8/YOLO11-seg TFLite (tanpa NMS di graf):
 * - Output 1: [1, C, N] atau [1, N, C] — C = 4 (cx,cy,w,h) + numClasses + 32 (mask coeff)
 * - Output 2: prototype masks (mis. [1,32,160,160])
 * - Skor kelas: sigmoid(logit); NMS setelah filter conf; bbox center → xyxy piksel letterbox.
 */
class YoloSegmentationTflite(
    context: Context,
    private val modelResId: Int = R.raw.best_v2_float32,
    private val labels: List<String> = emptyList(),
    private val logTag: String = "YoloSegmentationTflite",
) : AutoCloseable {

    private val interpreter: Interpreter
    private val inputW: Int
    private val inputH: Int
    private val inputC: Int
    private val inputDataType: DataType
    private val isNhwc: Boolean

    private val maskDimFallback = 32

    private enum class ProtoLayout { NCHW, NHWC }

    init {
        val modelBuffer = loadModelBuffer(context, modelResId)
        interpreter = Interpreter(modelBuffer)

        val inputShape = interpreter.getInputTensor(0).shape()
        // Umumnya:
        // - NHWC: [1, H, W, C]
        // - NCHW: [1, C, H, W]
        isNhwc = inputShape.size == 4 && inputShape[3] == 3
        inputDataType = interpreter.getInputTensor(0).dataType()

        inputH = if (isNhwc) inputShape[1] else inputShape[2]
        inputW = if (isNhwc) inputShape[2] else inputShape[3]
        inputC = if (isNhwc) inputShape[3] else inputShape[1]

        Log.d(logTag, "Input tensor shape=${inputShape.contentToString()}, isNhwc=$isNhwc, dataType=$inputDataType")
    }

    override fun close() {
        interpreter.close()
    }

    fun segment(
        bitmap: Bitmap,
        confThreshold: Float = 0.55f,
        iouThreshold: Float = 0.6f,
        maskThreshold: Float = 0.5f,
        maxDetections: Int = 150,
        maskAlpha: Int = 120,
    ): YoloSegmentationResult {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            return YoloSegmentationResult(overlay = bitmap, detections = emptyList())
        }

        val (letterboxed, ratio, dw, dh) = letterbox(bitmap, inputW, inputH)

        val inputBuffer = makeInputBuffer(letterboxed)

        val outDetectIndex = findDetectOutputIndex()
        val outProtoIndex = findProtoOutputIndex()

        val outDetectShape = interpreter.getOutputTensor(outDetectIndex).shape()
        val outProtoShape = interpreter.getOutputTensor(outProtoIndex).shape()

        // detection output: rank 3 (mis. [1,48,8400] atau [1,8400,48])
        require(outDetectShape.size == 3) { "Unexpected detect output rank: ${outDetectShape.contentToString()}" }
        val detectBatch = outDetectShape[0]
        val detectDim1 = outDetectShape[1]
        val detectDim2 = outDetectShape[2]
        require(detectBatch == 1) { "Expected batch=1. Got $detectBatch" }

        // prototype output: rank 4 (mis. [1,32,160,160])
        require(outProtoShape.size == 4) { "Unexpected proto output rank: ${outProtoShape.contentToString()}" }
        val protoBatch = outProtoShape[0]
        // Pada model Ultralytics YOLO-seg export (tanpa NMS / end2end),
        // prototype umumnya format:
        // - NCHW: [1, nm, 160, 160]
        // - NHWC: [1, 160, 160, nm]
        // Kita deteksi layout-nya lewat ukuran dim.
        val protoLayout = when {
            outProtoShape[1] == 32 && outProtoShape[2] == 160 && outProtoShape[3] == 160 -> ProtoLayout.NCHW
            outProtoShape[1] == 160 && outProtoShape[2] == 160 && outProtoShape[3] == 32 -> ProtoLayout.NHWC
            else -> {
                // fallback: asumsi NCHW seperti kode awal
                ProtoLayout.NCHW
            }
        }

        val protoH: Int
        val protoW: Int
        val maskDim: Int
        when (protoLayout) {
            ProtoLayout.NCHW -> {
                maskDim = outProtoShape[1]
                protoH = outProtoShape[2]
                protoW = outProtoShape[3]
            }
            ProtoLayout.NHWC -> {
                protoH = outProtoShape[1]
                protoW = outProtoShape[2]
                maskDim = outProtoShape[3]
            }
        }
        require(protoBatch == 1) { "Expected proto batch=1. Got $protoBatch" }

        val detect = Array(detectBatch) { Array(detectDim1) { FloatArray(detectDim2) } }
        val protoOutput: Any = when (protoLayout) {
            ProtoLayout.NCHW -> {
                Array(protoBatch) { // [1, nm, H, W]
                    Array(maskDim) { // nm
                        Array(protoH) { FloatArray(protoW) } // [H][W]
                    }
                }
            }
            ProtoLayout.NHWC -> {
                Array(protoBatch) { // [1, H, W, nm]
                    Array(protoH) { // H
                        Array(protoW) { FloatArray(maskDim) } // [W][nm]
                    }
                }
            }
        }

        val outputs = hashMapOf<Int, Any>(
            outDetectIndex to detect,
            outProtoIndex to protoOutput
        )
        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        // Ambil layout detect: [1,48,8400] atau [1,8400,48]
        val detectChannels: Int
        val detectPositions: Int
        val detectChannelsDimIs1: Boolean
        if (detectDim1 == 48) {
            detectChannels = detectDim1
            detectPositions = detectDim2
            detectChannelsDimIs1 = true
        } else if (detectDim2 == 48) {
            detectChannels = detectDim2
            detectPositions = detectDim1
            detectChannelsDimIs1 = false
        } else {
            throw IllegalStateException("Unexpected detect dims: ${outDetectShape.contentToString()} (expected channels=48)")
        }

        // YOLOv8/YOLO11 TFLite seg (tanpa NMS): [0..3]=cx,cy,w,h; [4..4+nc-1]=class logits;
        // [4+nc ..]=mask coefficients (32)
        val classStart = 4
        val numClasses = (detectChannels - 4 - maskDim).coerceAtLeast(0)
        val maskCoeffStart = 4 + numClasses
        if (numClasses <= 0 || maskCoeffStart + maskDim > detectChannels) {
            Log.w(
                logTag,
                "Layout: detectChannels=$detectChannels numClasses=$numClasses maskDim=$maskDim maskCoeffStart=$maskCoeffStart"
            )
        }

        fun detectAt(channel: Int, pos: Int): Float {
            return if (detectChannelsDimIs1) detect[0][channel][pos] else detect[0][pos][channel]
        }

        val detections = detect(
            detectAt = ::detectAt,
            numPreds = detectPositions,
            numClasses = numClasses,
            maskDim = maskDim,
            maskCoeffStart = maskCoeffStart,
            inputWidth = inputW,
            inputHeight = inputH,
            confThreshold = confThreshold,
            iouThreshold = iouThreshold,
            maxDetections = maxDetections,
        )

        if (detections.isEmpty()) {
            return YoloSegmentationResult(overlay = bitmap, detections = emptyList())
        }

        val kept = detections
        Log.d(logTag, "Kept detections: ${kept.size}")

        val overlay = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(overlay)

        val detectionsMeta = kept.map { YoloSegDetection(classIndex = it.classIndex, score = it.score) }

        // Default warna berdasarkan classIndex
        fun colorForClass(idx: Int): Int {
            val base = idx * 40
            val r = (120 + base) % 255
            val g = (255 - (base / 2) % 255)
            val b = (base * 2) % 255
            return Color.argb(maskAlpha, r, g, b)
        }
        fun labelForClass(idx: Int): String {
            return labels.getOrNull(idx) ?: "class_$idx"
        }

        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val textBgPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        val textPad = 8f

        val ratioSafe = if (ratio <= 0f) 1f else ratio

        for (det in kept) {
            // Buat mask dari prototype
            val maskOn = when (protoLayout) {
                ProtoLayout.NCHW -> {
                    val protoNchw = protoOutput as Array<Array<Array<FloatArray>>>
                    generateMask160FromNchw(protoNchw, det.coeffs, protoH, protoW, maskThreshold)
                }
                ProtoLayout.NHWC -> {
                    val protoNhwc = protoOutput as Array<Array<Array<FloatArray>>>
                    generateMask160FromNhwc(protoNhwc, det.coeffs, protoH, protoW, maskThreshold)
                }
            }

            val maskBitmap = buildMaskBitmap(maskOn, protoW, protoH, colorForClass(det.classIndex))
            val maskBitmapInput = Bitmap.createScaledBitmap(maskBitmap, inputW, inputH, false)

            // Crop mask ke bbox (di ruang input/letterbox)
            val leftI = det.x1.roundToInt().coerceIn(0, inputW - 1)
            val topI = det.y1.roundToInt().coerceIn(0, inputH - 1)
            val rightI = det.x2.roundToInt().coerceIn(leftI + 1, inputW)
            val bottomI = det.y2.roundToInt().coerceIn(topI + 1, inputH)

            val cropW = rightI - leftI
            val cropH = bottomI - topI
            if (cropW <= 1 || cropH <= 1) continue

            val maskCrop = Bitmap.createBitmap(maskBitmapInput, leftI, topI, cropW, cropH)

            // Map bbox dari letterbox-space -> original image
            val leftOrig = ((leftI - dw) / ratioSafe).roundToInt()
            val topOrig = ((topI - dh) / ratioSafe).roundToInt()
            val wOrig = ((cropW) / ratioSafe).roundToInt().coerceAtLeast(1)
            val hOrig = ((cropH) / ratioSafe).roundToInt().coerceAtLeast(1)

            if (leftOrig >= bitmap.width || topOrig >= bitmap.height) continue

            val maskScaled = Bitmap.createScaledBitmap(maskCrop, wOrig, hOrig, false)
            canvas.drawBitmap(maskScaled, leftOrig.toFloat(), topOrig.toFloat(), null)

            // Draw bbox + label di koordinat gambar asli agar kelas cacat terlihat jelas.
            val rightOrig = (leftOrig + wOrig).coerceAtMost(bitmap.width - 1)
            val bottomOrig = (topOrig + hOrig).coerceAtMost(bitmap.height - 1)
            val classColor = colorForClass(det.classIndex)
            boxPaint.color = classColor
            textBgPaint.color = Color.argb(220, Color.red(classColor), Color.green(classColor), Color.blue(classColor))

            val label = "${labelForClass(det.classIndex)} ${(det.score * 100f).roundToInt()}%"
            val textW = textPaint.measureText(label)
            val textH = textPaint.fontMetrics.run { bottom - top }
            val labelLeft = leftOrig.toFloat().coerceAtLeast(0f)
            val labelTop = (topOrig.toFloat() - textH - (textPad * 2f)).coerceAtLeast(0f)
            val labelRight = (labelLeft + textW + textPad * 2f).coerceAtMost(bitmap.width.toFloat())
            val labelBottom = (labelTop + textH + textPad * 2f).coerceAtMost(bitmap.height.toFloat())

            canvas.drawRect(
                leftOrig.toFloat(),
                topOrig.toFloat(),
                rightOrig.toFloat(),
                bottomOrig.toFloat(),
                boxPaint
            )
            canvas.drawRect(labelLeft, labelTop, labelRight, labelBottom, textBgPaint)
            val textBaseline = labelBottom - textPad - textPaint.fontMetrics.bottom
            canvas.drawText(label, labelLeft + textPad, textBaseline, textPaint)
        }

        return YoloSegmentationResult(overlay = overlay, detections = detectionsMeta)
    }

    fun segmentToOverlay(
        bitmap: Bitmap,
        confThreshold: Float = 0.55f,
        iouThreshold: Float = 0.6f,
        maskThreshold: Float = 0.5f,
        maxDetections: Int = 150,
        maskAlpha: Int = 120,
    ): Bitmap {
        return segment(
            bitmap = bitmap,
            confThreshold = confThreshold,
            iouThreshold = iouThreshold,
            maskThreshold = maskThreshold,
            maxDetections = maxDetections,
            maskAlpha = maskAlpha,
        ).overlay
    }

    private fun letterbox(src: Bitmap, targetW: Int, targetH: Int): Quad<Bitmap, Float, Float, Float> {
        val srcW = src.width
        val srcH = src.height

        val ratio = min(targetW.toFloat() / srcW.toFloat(), targetH.toFloat() / srcH.toFloat())
        val newW = (srcW * ratio).roundToInt()
        val newH = (srcH * ratio).roundToInt()
        val dw = (targetW - newW) / 2f
        val dh = (targetH - newH) / 2f

        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.rgb(114, 114, 114)) // umum untuk YOLO letterbox
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        canvas.drawBitmap(scaled, dw, dh, null)
        return Quad(out, ratio, dw, dh)
    }

    private fun makeInputBuffer(inputBitmap: Bitmap): ByteBuffer {
        // We assume input is float32 or uint8. Untuk sebagian besar export YOLO-seg,
        // input biasanya float32 yang dinormalisasi 0..1.
        val byteBuffer = ByteBuffer.allocateDirect(inputW * inputH * inputC * inputDataType.byteSize())
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputW * inputH)
        inputBitmap.getPixels(pixels, 0, inputW, 0, 0, inputW, inputH)

        // NHWC/NCHW
        if (isNhwc) {
            var idx = 0
            for (y in 0 until inputH) {
                for (x in 0 until inputW) {
                    val p = pixels[idx++]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    putChannel(byteBuffer, r)
                    putChannel(byteBuffer, g)
                    putChannel(byteBuffer, b)
                }
            }
        } else {
            // NCHW: [1, C, H, W]
            val ch = Array(inputC) { FloatArray(inputW * inputH) }
            val px = pixels
            for (y in 0 until inputH) {
                for (x in 0 until inputW) {
                    val i = y * inputW + x
                    val p = px[i]
                    ch[0][i] = ((p shr 16) and 0xFF).toFloat()
                    ch[1][i] = ((p shr 8) and 0xFF).toFloat()
                    ch[2][i] = (p and 0xFF).toFloat()
                }
            }
            for (c in 0 until inputC) {
                for (i in 0 until inputW * inputH) {
                    putChannel(byteBuffer, ch[c][i].toInt())
                }
            }
        }

        return byteBuffer
    }

    private fun ByteBuffer.allocateDirect(size: Int): ByteBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())

    private fun DataType.byteSize(): Int = when (this) {
        DataType.FLOAT32 -> 4
        DataType.UINT8 -> 1
        else -> 4
    }

    private fun putChannel(buffer: ByteBuffer, channel0to255: Int) {
        when (inputDataType) {
            DataType.FLOAT32 -> {
                // Normalisasi 0..1 (umum untuk float32 export)
                buffer.putFloat(channel0to255 / 255f)
            }
            DataType.UINT8 -> buffer.put(channel0to255.toByte())
            else -> buffer.putFloat(channel0to255 / 255f)
        }
    }

    private fun findDetectOutputIndex(): Int {
        for (i in 0 until interpreter.outputTensorCount) {
            val s = interpreter.getOutputTensor(i).shape()
            if (s.size == 3 && (s.contains(8400) || s.contains(8400))) return i
        }
        // fallback: index 0
        return 0
    }

    private fun findProtoOutputIndex(): Int {
        for (i in 0 until interpreter.outputTensorCount) {
            val s = interpreter.getOutputTensor(i).shape()
            if (s.size == 4 && s.contains(160) && s.contains(32)) return i
        }
        // fallback: index 1
        return min(1, interpreter.outputTensorCount - 1)
    }

    private fun iouDet(a: Detection, b: Detection): Float {
        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)

        val interArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)

        return interArea / (areaA + areaB - interArea + 1e-6f)
    }

    private fun nmsDetections(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val d = iterator.next()
                if (iouDet(best, d) > iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return result
    }

    /**
     * Decode raw YOLOv8/YOLO11-seg TFLite: layout [1, C, N] dengan C = 4 + numClasses + 32.
     * Skor kelas pakai sigmoid; bbox center (cx,cy,w,h) → xyxy piksel letterbox.
     */
    private fun detect(
        detectAt: (channel: Int, pos: Int) -> Float,
        numPreds: Int,
        numClasses: Int,
        maskDim: Int,
        maskCoeffStart: Int,
        inputWidth: Int,
        inputHeight: Int,
        confThreshold: Float,
        iouThreshold: Float,
        maxDetections: Int,
    ): List<Detection> {
        if (numClasses <= 0) return emptyList()

        val classStart = 4
        val detections = mutableListOf<Detection>()

        for (i in 0 until numPreds) {
            val cx = detectAt(0, i)
            val cy = detectAt(1, i)
            val w = detectAt(2, i)
            val h = detectAt(3, i)

            var bestScore = -1f
            var bestClass = -1

            for (c in 0 until numClasses) {
                val score = sigmoid(detectAt(classStart + c, i))
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            if (bestScore < confThreshold || bestClass < 0) continue
            if (w <= 0f || h <= 0f) continue

            val looksNormalized = cx <= 10f && cy <= 10f
            if (looksNormalized && (w > 1.5f || h > 1.5f)) continue
            if (!looksNormalized && (w > inputWidth * 2f || h > inputHeight * 2f)) continue

            val x1: Float
            val y1: Float
            val x2: Float
            val y2: Float
            if (looksNormalized) {
                x1 = (cx - w / 2f) * inputWidth
                y1 = (cy - h / 2f) * inputHeight
                x2 = (cx + w / 2f) * inputWidth
                y2 = (cy + h / 2f) * inputHeight
            } else {
                x1 = cx - w / 2f
                y1 = cy - h / 2f
                x2 = cx + w / 2f
                y2 = cy + h / 2f
            }

            val area = (x2 - x1) * (y2 - y1)
            if (area < 200f) continue

            val coeffs = FloatArray(maskDim)
            for (k in 0 until maskDim) {
                coeffs[k] = detectAt(maskCoeffStart + k, i)
            }

            detections.add(
                Detection(
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    score = bestScore,
                    classIndex = bestClass,
                    coeffs = coeffs,
                )
            )
        }

        val nmsOut = nmsDetections(detections, iouThreshold)
        return nmsOut.take(maxDetections)
    }

    private fun generateMask160FromNchw(
        protoNchw: Array<Array<Array<FloatArray>>>, // [1, nm, H, W]
        coeff: FloatArray,
        protoH: Int,
        protoW: Int,
        maskThreshold: Float
    ): BooleanArray {
        val mask = BooleanArray(protoH * protoW)
        for (y in 0 until protoH) {
            for (x in 0 until protoW) {
                var sum = 0f
                // protoNchw[0][m][y][x]
                for (m in coeff.indices) {
                    sum += coeff[m] * protoNchw[0][m][y][x]
                }
                val prob = sigmoid(sum)
                mask[y * protoW + x] = prob > maskThreshold
            }
        }
        return mask
    }

    private fun generateMask160FromNhwc(
        protoNhwc: Array<Array<Array<FloatArray>>>, // [1, H, W, nm]
        coeff: FloatArray,
        protoH: Int,
        protoW: Int,
        maskThreshold: Float
    ): BooleanArray {
        val mask = BooleanArray(protoH * protoW)
        for (y in 0 until protoH) {
            for (x in 0 until protoW) {
                val vec = protoNhwc[0][y][x] // FloatArray(nm)
                var sum = 0f
                for (m in coeff.indices) {
                    sum += coeff[m] * vec[m]
                }
                val prob = sigmoid(sum)
                mask[y * protoW + x] = prob > maskThreshold
            }
        }
        return mask
    }

    private fun buildMaskBitmap(
        maskOn: BooleanArray,
        w: Int,
        h: Int,
        colorOn: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        for (i in 0 until w * h) {
            pixels[i] = if (maskOn[i]) colorOn else 0
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun loadModelBuffer(context: Context, modelResId: Int): ByteBuffer {
        val input: InputStream = context.resources.openRawResource(modelResId)
        val bytes = input.readBytes()
        input.close()
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        return buffer
    }

    // Helper: mini 4-tuple biar tidak perlu data class tambahan banyak.
    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

