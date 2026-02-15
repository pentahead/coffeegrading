package id.my.faruq.coffegrader.util

/**
 * Palet warna per mutu (1, 2, 3, 4a, 4b, 5, 6).
 * Mengembalikan nilai warna ARGB (Long) agar bisa dipakai di layer data/UI.
 * Gunakan dengan: Color(GradeColors.colorFor(gradeText))
 */
object GradeColors {

    const val MUTU_1 = 0xFF16A34AL   // hijau
    const val MUTU_2 = 0xFF65A30DL  // lime
    const val MUTU_3 = 0xFFB45309L   // oranye
    const val MUTU_4A = 0xFFEA580CL // oranye-merah
    const val MUTU_4B = 0xFFDC2626L  // merah
    const val MUTU_5 = 0xFF991B1BL  // merah gelap
    const val MUTU_6 = 0xFF450A0AL  // merah sangat gelap

    /** Angka mutu untuk tampilan lingkaran: "Mutu 1" -> "1", "Mutu 4a" -> "4a" */
    @JvmStatic
    fun gradeDisplayText(grade: String): String =
        grade.replace(Regex("(?i)mutu\\s+"), "").trim().ifBlank { grade }

    /** Normalisasi "Mutu 1" / "1" -> kode untuk warna */
    @JvmStatic
    fun colorFor(grade: String): Long {
        val g = grade.lowercase().trim()
        return when {
            g == "1" || g == "mutu 1" -> MUTU_1
            g == "2" || g == "mutu 2" -> MUTU_2
            g == "3" || g == "mutu 3" -> MUTU_3
            g == "4a" || g == "mutu 4a" -> MUTU_4A
            g == "4b" || g == "mutu 4b" -> MUTU_4B
            g == "5" || g == "mutu 5" -> MUTU_5
            g == "6" || g == "mutu 6" -> MUTU_6
            g.startsWith("1") -> MUTU_1
            g.startsWith("2") -> MUTU_2
            g.startsWith("3") -> MUTU_3
            g.contains("4a") -> MUTU_4A
            g.contains("4b") -> MUTU_4B
            g.startsWith("5") -> MUTU_5
            g.startsWith("6") -> MUTU_6
            else -> MUTU_6
        }
    }
}
