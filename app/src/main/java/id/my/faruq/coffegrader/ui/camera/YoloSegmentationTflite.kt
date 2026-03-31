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
import kotlin.math.exp

data class YoloSegDetection(
    val classIndex: Int,
    val score: Float,
)

data class YoloSegmentationResult(
    val overlay: Bitmap,
    val detections: List<YoloSegDetection>,
)

/**
 * Loader + decoder sederhana untuk YOLO-seg export TFLite:
 * - Output 1: detection + mask coeff (mis. [1,48,8400] atau [1,8400,48])
 * - Output 2: prototype masks (mis. [1,32,160,160])
 *
 * Catatan:
 * - Decode bbox di sini memakai heuristic. Kalau bbox hasilnya salah (mis. tidak berada di area objek),
 *   Anda perlu menyesuaikan format decode sesuai script export model Anda (YOLOv8 raw vs already-decoded).
 */
class YoloSegmentationTflite(
    context: Context,
    private val modelResId: Int = R.raw.best_float32,
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
        confThreshold: Float = 0.25f,
        iouThreshold: Float = 0.5f,
        maskThreshold: Float = 0.5f,
        maxDetections: Int = 5,
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

        // Asumsi layout (umum untuk YOLO-seg):
        // [0..3]=bbox (cx,cy,w,h), [4]=obj, [5..(5+nc-1)]=class scores,
        // [(channels-maskDim)..(channels-1)]=mask coeff
        val objIndex = 4
        val classStart = 5
        val maskCoeffStart = detectChannels - maskDim
        val classCount = (maskCoeffStart - classStart).coerceAtLeast(0)
        if (maskCoeffStart <= objIndex + 1) {
            Log.w(logTag, "Layout inference: maskCoeffStart=$maskCoeffStart classCount=$classCount maskDim=$maskDim")
        }

        fun detectAt(channel: Int, pos: Int): Float {
            // detect[channel][pos] jika detectChannelsDimIs1=true, kalau tidak kebalik
            return if (detectChannelsDimIs1) detect[0][channel][pos] else detect[0][pos][channel]
        }

        val candidates = ArrayList<Candidate>(maxDetections * 3)
        val samplePositions = min(200, detectPositions)

        // Heuristic bbox normalized check (berdasarkan sebagian sampel)
        var bboxLooksNormalized = true
        for (p in 0 until samplePositions) {
            val cx = detectAt(0, p)
            val cy = detectAt(1, p)
            val w = detectAt(2, p)
            val h = detectAt(3, p)
            val maybe = (cx in 0f..1.5f && cy in 0f..1.5f && w in 0f..1.5f && h in 0f..1.5f)
            bboxLooksNormalized = bboxLooksNormalized && maybe
        }

        for (pos in 0 until detectPositions) {
            val cxRaw = detectAt(0, pos)
            val cyRaw = detectAt(1, pos)
            val wRaw = detectAt(2, pos)
            val hRaw = detectAt(3, pos)

            val cx = if (bboxLooksNormalized) cxRaw * inputW else cxRaw
            val cy = if (bboxLooksNormalized) cyRaw * inputH else cyRaw
            val w = if (bboxLooksNormalized) wRaw * inputW else wRaw
            val h = if (bboxLooksNormalized) hRaw * inputH else hRaw

            val obj = sigmoid(detectAt(objIndex, pos))
            var bestCls = -1
            var bestClsProb = 0f
            for (c in 0 until classCount) {
                val prob = sigmoid(detectAt(classStart + c, pos))
                if (prob > bestClsProb) {
                    bestClsProb = prob
                    bestCls = c
                }
            }

            val score = obj * bestClsProb
            if (score < confThreshold || bestCls < 0) continue

            val left = cx - w / 2f
            val top = cy - h / 2f
            val right = cx + w / 2f
            val bottom = cy + h / 2f

            // Simpan mask coeff
            val coeff = FloatArray(maskDim)
            for (m in 0 until maskDim) {
                coeff[m] = detectAt(maskCoeffStart + m, pos)
            }

            candidates.add(
                Candidate(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    classIndex = bestCls,
                    score = score,
                    maskCoeff = coeff
                )
            )
        }

        if (candidates.isEmpty()) {
            return YoloSegmentationResult(overlay = bitmap, detections = emptyList())
        }

        val kept = nms(candidates, iouThreshold, maxDetections)
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

        val ratioSafe = if (ratio <= 0f) 1f else ratio

        for (det in kept) {
            // Buat mask dari prototype
            val maskOn = when (protoLayout) {
                ProtoLayout.NCHW -> {
                    val protoNchw = protoOutput as Array<Array<Array<FloatArray>>>
                    generateMask160FromNchw(protoNchw, det.maskCoeff, protoH, protoW, maskThreshold)
                }
                ProtoLayout.NHWC -> {
                    val protoNhwc = protoOutput as Array<Array<Array<FloatArray>>>
                    generateMask160FromNhwc(protoNhwc, det.maskCoeff, protoH, protoW, maskThreshold)
                }
            }

            val maskBitmap = buildMaskBitmap(maskOn, protoW, protoH, colorForClass(det.classIndex))
            val maskBitmapInput = Bitmap.createScaledBitmap(maskBitmap, inputW, inputH, false)

            // Crop mask ke bbox (di ruang input/letterbox)
            val leftI = det.left.roundToInt().coerceIn(0, inputW - 1)
            val topI = det.top.roundToInt().coerceIn(0, inputH - 1)
            val rightI = det.right.roundToInt().coerceIn(leftI + 1, inputW)
            val bottomI = det.bottom.roundToInt().coerceIn(topI + 1, inputH)

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
        }

        return YoloSegmentationResult(overlay = overlay, detections = detectionsMeta)
    }

    fun segmentToOverlay(
        bitmap: Bitmap,
        confThreshold: Float = 0.25f,
        iouThreshold: Float = 0.5f,
        maskThreshold: Float = 0.5f,
        maxDetections: Int = 5,
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

    private data class Candidate(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val classIndex: Int,
        val score: Float,
        val maskCoeff: FloatArray
    )

    private fun nms(
        candidates: List<Candidate>,
        iouThreshold: Float,
        maxDet: Int
    ): List<Candidate> {
        val sorted = candidates.sortedByDescending { it.score }
        val picked = ArrayList<Candidate>(maxDet)
        for (cand in sorted) {
            var keep = true
            for (p in picked) {
                val iou = iou(cand, p)
                if (iou > iouThreshold) {
                    keep = false
                    break
                }
            }
            if (keep) {
                picked.add(cand)
                if (picked.size >= maxDet) break
            }
        }
        return picked
    }

    private fun iou(a: Candidate, b: Candidate): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interW = max(0f, interRight - interLeft)
        val interH = max(0f, interBottom - interTop)
        val interArea = interW * interH

        val areaA = max(0f, a.right - a.left) * max(0f, a.bottom - a.top)
        val areaB = max(0f, b.right - b.left) * max(0f, b.bottom - b.top)
        val union = areaA + areaB - interArea
        return if (union <= 0f) 0f else interArea / union
    }

    private fun sigmoid(x: Float): Float = (1f / (1f + exp(-x.toDouble()).toFloat()))

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

