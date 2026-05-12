package id.my.faruq.coffegrader.ml

/**
 * Satu objek terdeteksi setelah NMS.
 *
 * @param classIndex  Index kelas dengan skor tertinggi (0..20).
 * @param className   Nama kelas (dari CoffeeLabelTaxonomy.NAMES).
 * @param score       Confidence score kelas tertinggi (0..1).
 * @param cx          Center-x dalam koordinat piksel INPUT (0..640).
 * @param cy          Center-y.
 * @param w           Lebar bbox dalam piksel input.
 * @param h           Tinggi bbox dalam piksel input.
 */
data class DetectionDto(
    val classIndex: Int,
    val className: String,
    val score: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
)

/** Hasil agrasi per jenis cacat, untuk disimpan ke scan_defect dan ditampilkan UI. */
data class AggregatedDefects(
    val totalScore: Double,
    val totalBeans: Int,
    val rows: List<DefectRow>,
)

data class DefectRow(
    val defectName: String,   // nama tampilan (sama dengan NAMES)
    val count: Int,
    val defectValue: Double,  // count × bobot
)
