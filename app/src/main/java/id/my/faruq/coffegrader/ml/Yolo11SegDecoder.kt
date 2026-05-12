package id.my.faruq.coffegrader.ml

import android.util.Log

/**
 * Decoder output YOLO11n-seg.
 *
 * Output 0: FloatArray shape [1, 57, 8400]
 *   Layout per prediction (kolom j):
 *     output[0][0..3][j]   = cx, cy, w, h  (koordinat dalam skala INPUT_SIZE)
 *     output[0][4..24][j]  = class scores (21 kelas, raw — bukan sigmoid)
 *     output[0][25..56][j] = mask coefficients (32 nilai)
 *
 * Proses:
 *   1. Transpose [57, 8400] → iterasi per prediksi
 *   2. Cari class score max; skip jika < CONFIDENCE_THRESHOLD
 *   3. NMS (greedy, sort by score)
 *   4. Return List<DetectionDto>
 */
object Yolo11SegDecoder {

    private const val TAG = "Yolo11SegDecoder"

    /**
     * @param rawOutput  FloatArray berukuran 1 × 57 × 8400 (diratakan row-major).
     *                   Index: [batch * 57 * 8400 + channel * 8400 + pred]
     * @param confThreshold  Minimum class score.
     * @param iouThreshold   IoU threshold NMS.
     * @return List deteksi setelah NMS, sudah dipetakan ke nama kelas.
     */
    fun decode(
        rawOutput: FloatArray,
        confThreshold: Float = ModelConfig.CONFIDENCE_THRESHOLD,
        iouThreshold: Float = ModelConfig.IOU_THRESHOLD,
    ): List<DetectionDto> {
        val numPred    = ModelConfig.NUM_PREDICTIONS   // 8400
        val bboxDims   = ModelConfig.BBOX_DIMS         // 4
        val numClasses = CoffeeLabelTaxonomy.NUM_CLASSES // 21
        val numChannels = bboxDims + numClasses + ModelConfig.MASK_COEFF_DIM // 57

        // Validasi panjang array
        val expectedLen = numChannels * numPred
        if (rawOutput.size < expectedLen) {
            Log.e(TAG, "rawOutput.size=${rawOutput.size} < expected=$expectedLen — abort decode")
            return emptyList()
        }

        // Fungsi akses: rawOutput[channel * numPred + predIndex]
        fun get(channel: Int, pred: Int): Float = rawOutput[channel * numPred + pred]

        val candidates = mutableListOf<DetectionDto>()

        for (j in 0 until numPred) {
            // --- Cari kelas dengan score tertinggi ---
            var bestClass = -1
            var bestScore = -Float.MAX_VALUE
            for (c in 0 until numClasses) {
                val score = get(bboxDims + c, j)
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            // Terapkan sigmoid jika perlu (output YOLO11 biasanya raw logit)
            val conf = sigmoid(bestScore)
            if (conf < confThreshold) continue

            val cx = get(0, j)
            val cy = get(1, j)
            val w  = get(2, j)
            val h  = get(3, j)

            if (w <= 0f || h <= 0f) continue

            candidates.add(
                DetectionDto(
                    classIndex = bestClass,
                    className  = CoffeeLabelTaxonomy.NAMES.getOrElse(bestClass) { "unknown" },
                    score      = conf,
                    cx = cx, cy = cy, w = w, h = h,
                )
            )
        }

        if (candidates.isEmpty()) return emptyList()

        return nonMaxSuppression(candidates, iouThreshold)
    }

    // -------------------------------------------------------------------------
    // NMS greedy per-class
    // -------------------------------------------------------------------------

    private fun nonMaxSuppression(
        detections: List<DetectionDto>,
        iouThreshold: Float,
    ): List<DetectionDto> {
        // Kelompokkan per kelas, lalu NMS dalam kelas
        val result = mutableListOf<DetectionDto>()
        val byClass = detections.groupBy { it.classIndex }

        for ((_, group) in byClass) {
            val sorted = group.sortedByDescending { it.score }.toMutableList()
            while (sorted.isNotEmpty()) {
                val best = sorted.removeAt(0)
                result.add(best)
                sorted.removeAll { iou(best, it) >= iouThreshold }
            }
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun sigmoid(x: Float): Float = (1f / (1f + Math.exp(-x.toDouble()))).toFloat()

    /** IoU antara dua bbox (cx, cy, w, h format). */
    private fun iou(a: DetectionDto, b: DetectionDto): Float {
        val ax1 = a.cx - a.w / 2f; val ay1 = a.cy - a.h / 2f
        val ax2 = a.cx + a.w / 2f; val ay2 = a.cy + a.h / 2f
        val bx1 = b.cx - b.w / 2f; val by1 = b.cy - b.h / 2f
        val bx2 = b.cx + b.w / 2f; val by2 = b.cy + b.h / 2f

        val interX1 = maxOf(ax1, bx1); val interY1 = maxOf(ay1, by1)
        val interX2 = minOf(ax2, bx2); val interY2 = minOf(ay2, by2)

        val interW = maxOf(0f, interX2 - interX1)
        val interH = maxOf(0f, interY2 - interY1)
        val interArea = interW * interH

        val aArea = a.w * a.h
        val bArea = b.w * b.h
        val unionArea = aArea + bArea - interArea

        return if (unionArea <= 0f) 0f else interArea / unionArea
    }
}
