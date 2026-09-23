package id.my.faruq.coffegrader.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.Log
import id.my.faruq.coffegrader.util.UiLabels

/**
 * Decoder segmentasi mask YOLO11n-seg.
 *
 * Koordinat DetectionDto sudah dalam bitmap-asli space (setelah unpad).
 * Untuk decode mask proto, kita perlu konversi BALIK ke proto-space (0..160).
 */
object SegmentationDecoder {

    private const val TAG        = "SegDecoder"
    private const val PROTO_H    = 160
    private const val PROTO_W    = 160
    private const val MASK_DIM   = 32
    private const val MASK_ALPHA = 120

    fun drawWithMasks(
        src        : Bitmap,
        detections : List<DetectionDto>,
        maskCoeffs : FloatArray,
        protoOutput: FloatArray,
        letterbox  : LetterboxParams,
        boxColors  : List<Int>,
    ): Bitmap {
        if (detections.isEmpty()) return src

        val out    = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val stroke = maxOf(3f, src.width / 250f)

        val boxPaint  = Paint().apply { style = Paint.Style.STROKE; strokeWidth = stroke; isAntiAlias = true }
        val bgPaint   = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
        val maskPaint = Paint().apply { isAntiAlias = true; alpha = MASK_ALPHA }
        val txtPaint  = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = maxOf(24f, src.width / 38f)
            isAntiAlias = true; isFakeBoldText = true
        }

        // Log params sekali untuk debug
        Log.d(TAG, "src=${src.width}x${src.height} scale=${letterbox.scale} " +
                "padX=${letterbox.padX} padY=${letterbox.padY}")

        detections.forEachIndexed { detIdx, det ->
            val color = boxColors[det.classIndex % boxColors.size]

            // ── Mask ──────────────────────────────────────────────────────
            val maskBmp = computeMask(detIdx, maskCoeffs, protoOutput, det, src, letterbox)
            if (maskBmp != null) {
                maskPaint.colorFilter = android.graphics.PorterDuffColorFilter(
                    color, PorterDuff.Mode.SRC_IN
                )
                canvas.drawBitmap(maskBmp, 0f, 0f, maskPaint)
                maskPaint.colorFilter = null
                maskBmp.recycle()
            }

            // ── Bbox ──────────────────────────────────────────────────────
            boxPaint.color = color
            val x1 = det.cx - det.w / 2f
            val y1 = det.cy - det.h / 2f
            val x2 = det.cx + det.w / 2f
            val y2 = det.cy + det.h / 2f
            canvas.drawRect(RectF(x1, y1, x2, y2), boxPaint)

            // ── Label ─────────────────────────────────────────────────────
            bgPaint.color = color
            val label    = "${UiLabels.defect(det.className)} ${"%.2f".format(det.score)}"
            val tw       = txtPaint.measureText(label)
            val th       = txtPaint.textSize
            val labelTop = if (y1 - th - 4f >= 0f) y1 - th - 4f else y2
            canvas.drawRect(RectF(x1, labelTop, x1 + tw + 8f, labelTop + th + 4f), bgPaint)
            canvas.drawText(label, x1 + 4f, labelTop + th, txtPaint)
        }

        return out
    }

    private fun computeMask(
        detIdx     : Int,
        maskCoeffs : FloatArray,
        protoOutput: FloatArray,
        det        : DetectionDto,
        src        : Bitmap,
        lb         : LetterboxParams,
    ): Bitmap? {
        val base = detIdx * MASK_DIM
        if (base + MASK_DIM > maskCoeffs.size) return null

        /**
         * Konversi koordinat src (bitmap asli) → proto space (0..160)
         *
         * Alur letterbox:
         *   src → scale → input640 (dengan padding) → proto160 (÷4)
         *
         * Balik:
         *   srcX → inputX = srcX * lb.scale + lb.padX
         *   inputX → protoX = inputX * (PROTO_W / INPUT_SIZE)
         *                    = inputX / 4   (karena 160/640 = 0.25)
         */
        val inputToProto = PROTO_W.toFloat() / ModelConfig.INPUT_SIZE  // 0.25

        fun srcXToProto(x: Float): Float =
            (x * lb.scale + lb.padX) * inputToProto

        fun srcYToProto(y: Float): Float =
            (y * lb.scale + lb.padY) * inputToProto

        // Bbox dalam proto-space
        val bx1 = srcXToProto(det.cx - det.w / 2f).coerceIn(0f, PROTO_W - 1f)
        val by1 = srcYToProto(det.cy - det.h / 2f).coerceIn(0f, PROTO_H - 1f)
        val bx2 = srcXToProto(det.cx + det.w / 2f).coerceIn(0f, PROTO_W - 1f)
        val by2 = srcYToProto(det.cy + det.h / 2f).coerceIn(0f, PROTO_H - 1f)

        Log.d(TAG, "det[$detIdx] ${det.className} " +
                "src=[${det.cx.toInt()},${det.cy.toInt()} ${det.w.toInt()}x${det.h.toInt()}] " +
                "proto=[${bx1.toInt()},${by1.toInt()} → ${bx2.toInt()},${by2.toInt()}]")

        if (bx2 - bx1 < 1f || by2 - by1 < 1f) return null

        // Hitung mask di seluruh proto 160×160 (dalam bbox saja)
        val protoPixels = IntArray(PROTO_H * PROTO_W)
        val iBy1 = by1.toInt()
        val iBy2 = by2.toInt().coerceAtMost(PROTO_H)
        val iBx1 = bx1.toInt()
        val iBx2 = bx2.toInt().coerceAtMost(PROTO_W)

        for (ph in iBy1 until iBy2) {
            val protoBase = (ph * PROTO_W + iBx1) * MASK_DIM
            for (pw in iBx1 until iBx2) {
                var dot = 0f
                val pb = (ph * PROTO_W + pw) * MASK_DIM
                for (c in 0 until MASK_DIM) {
                    dot += maskCoeffs[base + c] * protoOutput[pb + c]
                }
                if (sigmoid(dot) > 0.5f) {
                    protoPixels[ph * PROTO_W + pw] = android.graphics.Color.WHITE
                }
            }
        }

        // Proto bitmap 160×160
        val protoBmp = Bitmap.createBitmap(PROTO_W, PROTO_H, Bitmap.Config.ARGB_8888)
        protoBmp.setPixels(protoPixels, 0, PROTO_W, 0, 0, PROTO_W, PROTO_H)

        // Scale ke src size — mapping harus memperhitungkan padding letterbox
        // Region proto yang berisi gambar asli (tanpa padding):
        //   protoImgX = padX * inputToProto  ..  (padX + newW) * inputToProto
        //   protoImgY = padY * inputToProto  ..  (padY + newH) * inputToProto
        val newW = (src.width  * lb.scale).toInt()
        val newH = (src.height * lb.scale).toInt()
        val protoImgX = (lb.padX * inputToProto).toInt()
        val protoImgY = (lb.padY * inputToProto).toInt()
        val protoImgW = (newW * inputToProto).toInt().coerceAtLeast(1)
        val protoImgH = (newH * inputToProto).toInt().coerceAtLeast(1)

        // Crop region gambar dari proto (tanpa padding)
        val protoImgW2 = protoImgW.coerceAtMost(PROTO_W - protoImgX)
        val protoImgH2 = protoImgH.coerceAtMost(PROTO_H - protoImgY)
        if (protoImgW2 <= 0 || protoImgH2 <= 0) {
            protoBmp.recycle(); return null
        }

        val croppedProto = Bitmap.createBitmap(protoBmp, protoImgX, protoImgY, protoImgW2, protoImgH2)
        protoBmp.recycle()

        // Scale cropped proto → src size
        val scaledMask = Bitmap.createScaledBitmap(croppedProto, src.width, src.height, true)
        croppedProto.recycle()

        return scaledMask
    }

    private fun sigmoid(x: Float) = (1.0 / (1.0 + Math.exp(-x.toDouble()))).toFloat()
}
