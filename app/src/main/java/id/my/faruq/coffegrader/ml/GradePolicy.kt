package id.my.faruq.coffegrader.ml

/**
 * Menentukan mutu kopi berdasarkan total nilai cacat dan jenis kopi,
 * sesuai SNI 01-2907-2008.
 *
 * Output string konsisten dengan GradeColors.colorFor():
 *   "Mutu 1" | "Mutu 2" | "Mutu 3" | "Mutu 4" | "Mutu 4a" | "Mutu 4b"
 *   | "Mutu 5" | "Mutu 6" | "Di luar standar"
 *
 * Tabel SNI (nilai cacat per 300 g):
 *   Mutu 1  : maksimum 11
 *   Mutu 2  : 12 – 25
 *   Mutu 3  : 26 – 44
 *   Mutu 4a : 45 – 60   
 *   Mutu 4b : 61 – 80   
 *   Mutu 5  : 81 – 150
 *   Mutu 6  : 151 – 225
 *   Di luar : > 225
 *
 * Catatan SNI:
 *   - Untuk Arabika, mutu 4 TIDAK dibagi menjadi 4a dan 4b.
 *   - Untuk Robusta, mutu 4 dibagi 4a (45–60) dan 4b (61–80).
 *   - Perhitungan pada sampel 300 gram.
 */
object GradePolicy {

    /**
     * @param totalScore  Total nilai cacat (hasil [DefectAggregator.aggregate]).
     * @param coffeeType  Exact string dari DB: "Arabika" atau "Robusta".
     * @return            String mutu kompatibel dengan GradeColors.
     */
    fun gradeFromScore(totalScore: Double, coffeeType: String?): String {
        val isArabika = coffeeType == "Arabika"
        return when {
            totalScore <= 11.0  -> "Mutu 1"
            totalScore <= 25.0  -> "Mutu 2"
            totalScore <= 44.0  -> "Mutu 3"
            totalScore <= 60.0  -> "Mutu 4a"
            totalScore <= 80.0  -> "Mutu 4b"
            totalScore <= 150.0 -> "Mutu 5"
            totalScore <= 225.0 -> "Mutu 6"
            else                -> "Di luar standar"
        }
    }
}
