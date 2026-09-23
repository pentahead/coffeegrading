package id.my.faruq.coffegrader.ml

/**
 * Mengagregasi hasil deteksi menjadi nilai cacat total dan baris per jenis cacat.
 *
 * Aturan SNI 01-2907-2008:
 *   - 1 deteksi bbox = 1 objek/biji.
 *   - Jika satu biji memiliki lebih dari satu cacat terdeteksi pada bbox yang
 *     sama (overlap tinggi), hanya diambil nilai cacat TERBESAR — sesuai
 *     catatan SNI: "jika satu biji memiliki lebih dari satu cacat, hanya
 *     diambil nilai cacat terbesar."
 *   - Setelah NMS, setiap bbox sudah mewakili satu objek dengan satu kelas
 *     dominan (class score tertinggi), sehingga aturan ini sudah terpenuhi
 *     secara implisit oleh decoder. Namun jika di masa depan ada pipeline
 *     multi-label per bbox, gunakan maxDefectPerBean() di bawah.
 *   - biji_normal (index 10, bobot = 0) ikut dihitung ke totalBeans
 *     tapi tidak menambah defect score.
 */
object DefectAggregator {

    /**
     * @param detections  List setelah NMS dari [Yolo11SegDecoder].
     * @return            [AggregatedDefects] siap simpan ke Room + tampil UI.
     */
    fun aggregate(detections: List<DetectionDto>): AggregatedDefects {
        // Hitung count per classIndex
        val countMap = mutableMapOf<Int, Int>()
        for (det in detections) {
            countMap[det.classIndex] = (countMap[det.classIndex] ?: 0) + 1
        }

        val rows = mutableListOf<DefectRow>()
        var totalScore = 0.0

        for ((classIndex, count) in countMap) {
            val weight = DefectWeights.weightOf(classIndex)
            val defectValue = count * weight

            rows.add(
                DefectRow(
                    defectName  = CoffeeLabelTaxonomy.NAMES.getOrElse(classIndex) { "unknown" },
                    count       = count,
                    defectValue = defectValue,
                )
            )
            totalScore += defectValue
        }

        // Sort: nilai cacat terbesar di atas untuk tampilan UI
        rows.sortByDescending { it.defectValue }

        return AggregatedDefects(
            totalScore = totalScore,
            totalBeans = detections.size,
            rows       = rows,
        )
    }

    /**
     * [HELPER — opsional untuk pipeline multi-label masa depan]
     *
     * Jika suatu bbox memiliki beberapa label cacat (multi-label detection),
     * ambil hanya bobot terbesar per biji sesuai aturan SNI.
     *
     * @param labelsPerBean  List<List<Int>> — tiap elemen = list classIndex
     *                       untuk satu biji.
     * @return               Total nilai cacat (Double).
     */
    fun aggregateMultiLabel(labelsPerBean: List<List<Int>>): Double {
        return labelsPerBean.sumOf { labels ->
            labels.maxOfOrNull { DefectWeights.weightOf(it) } ?: 0.0
        }
    }
}
