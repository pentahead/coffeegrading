package id.my.faruq.coffegrader.ml

import android.util.Log

/**
 * Decoder output YOLO11n-seg.
 * Output0: [1, 57, 8400] — bbox + class scores + mask coefficients
 *
 * FIX:
 *  - Model sudah sigmoid → tidak perlu sigmoid lagi
 *  - Koordinat normalized 0–1 → kalikan INPUT_SIZE
 *  - maskCoeffs[32] disimpan per deteksi untuk segmentasi
 */
object Yolo11SegDecoder {

    private const val TAG       = "Yolo11SegDecoder"
    private const val MIN_BOX_PX = 2f

    fun decode(
        rawOutput    : FloatArray,
        confThreshold: Float = ModelConfig.CONFIDENCE_THRESHOLD,
        iouThreshold : Float = ModelConfig.IOU_THRESHOLD,
    ): List<DetectionDto> {
        val numPred     = ModelConfig.NUM_PREDICTIONS
        val bboxDims    = ModelConfig.BBOX_DIMS
        val numClasses  = CoffeeLabelTaxonomy.NUM_CLASSES
        val maskDim     = ModelConfig.MASK_COEFF_DIM
        val numChannels = bboxDims + numClasses + maskDim  // 57
        val size        = ModelConfig.INPUT_SIZE.toFloat()

        if (rawOutput.size < numChannels * numPred) {
            Log.e(TAG, "rawOutput terlalu kecil — abort")
            return emptyList()
        }

        fun get(channel: Int, pred: Int) = rawOutput[channel * numPred + pred]

        val candidates = mutableListOf<DetectionDto>()

        for (j in 0 until numPred) {
            // Class score sudah sigmoid di model
            var bestClass = -1
            var bestScore = -Float.MAX_VALUE
            for (c in 0 until numClasses) {
                val s = get(bboxDims + c, j)
                if (s > bestScore) { bestScore = s; bestClass = c }
            }
            if (bestScore < confThreshold) continue

            val cxPx = get(0, j) * size
            val cyPx = get(1, j) * size
            val wPx  = get(2, j) * size
            val hPx  = get(3, j) * size
            if (wPx < MIN_BOX_PX || hPx < MIN_BOX_PX) continue

            // Simpan 32 mask coefficients
            val coeffs = FloatArray(maskDim) { k ->
                get(bboxDims + numClasses + k, j)
            }

            candidates.add(DetectionDto(
                classIndex = bestClass,
                className  = CoffeeLabelTaxonomy.NAMES.getOrElse(bestClass) { "unknown" },
                score      = bestScore,
                cx = cxPx, cy = cyPx, w = wPx, h = hPx,
                maskCoeffs = coeffs,
            ))
        }

        Log.d(TAG, "candidates before NMS = ${candidates.size}")
        if (candidates.isEmpty()) return emptyList()

        val result = nonMaxSuppression(candidates, iouThreshold)
        Log.d(TAG, "detections after NMS  = ${result.size}")
        result.take(10).forEachIndexed { i, d ->
            Log.d(TAG, "  det[$i] ${d.className} score=${"%.3f".format(d.score)} " +
                    "cx=${"%.1f".format(d.cx)} cy=${"%.1f".format(d.cy)} " +
                    "w=${"%.1f".format(d.w)} h=${"%.1f".format(d.h)}")
        }
        return result
    }

    private fun nonMaxSuppression(list: List<DetectionDto>, iouThresh: Float): List<DetectionDto> {
        val result  = mutableListOf<DetectionDto>()
        val byClass = list.groupBy { it.classIndex }
        for ((_, group) in byClass) {
            val sorted = group.sortedByDescending { it.score }.toMutableList()
            while (sorted.isNotEmpty()) {
                val best = sorted.removeAt(0)
                result.add(best)
                sorted.removeAll { iou(best, it) >= iouThresh }
            }
        }
        return result
    }

    private fun iou(a: DetectionDto, b: DetectionDto): Float {
        val ax1 = a.cx - a.w/2f; val ay1 = a.cy - a.h/2f
        val ax2 = a.cx + a.w/2f; val ay2 = a.cy + a.h/2f
        val bx1 = b.cx - b.w/2f; val by1 = b.cy - b.h/2f
        val bx2 = b.cx + b.w/2f; val by2 = b.cy + b.h/2f
        val iw = maxOf(0f, minOf(ax2,bx2) - maxOf(ax1,bx1))
        val ih = maxOf(0f, minOf(ay2,by2) - maxOf(ay1,by1))
        val inter = iw * ih
        val union = a.w*a.h + b.w*b.h - inter
        return if (union <= 0f) 0f else inter / union
    }
}
