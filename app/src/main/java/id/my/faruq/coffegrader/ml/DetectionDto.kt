package id.my.faruq.coffegrader.ml

/**
 * Satu objek terdeteksi setelah NMS + unpad.
 * cx, cy, w, h dalam koordinat bitmap ASLI (piksel).
 */
data class DetectionDto(
    val classIndex : Int,
    val className  : String,
    val score      : Float,
    val cx: Float, val cy: Float,
    val w : Float, val h : Float,
    val maskCoeffs : FloatArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectionDto) return false
        return classIndex == other.classIndex && score == other.score &&
               cx == other.cx && cy == other.cy
    }
    override fun hashCode() = 31 * classIndex + score.toBits()
}

/**
 * Hasil full inference.
 * @param detections  Koordinat sudah dalam piksel bitmap asli.
 * @param protoFlat   Proto mask [160*160*32] untuk segmentasi.
 * @param letterbox   Parameter letterbox (untuk segmentasi decoder).
 * @param srcWidth    Lebar bitmap asli.
 * @param srcHeight   Tinggi bitmap asli.
 */
data class InferenceOutput(
    val detections : List<DetectionDto>,
    val protoFlat  : FloatArray,
    val letterbox  : LetterboxParams,
    val srcWidth   : Int,
    val srcHeight  : Int,
)

data class AggregatedDefects(
    val totalScore : Double,
    val totalBeans : Int,
    val rows       : List<DefectRow>,
)

data class DefectRow(
    val defectName  : String,
    val count       : Int,
    /** Bobot nilai cacat SNI per biji (lihat [DefectWeights]), bukan total baris. */
    val defectValue : Double,
)
