package id.my.faruq.coffegrader.util

/**
 * English display labels for values that are stored/compared in Indonesian
 * (grade text, sample options, model class names).
 * Display only — the stored values themselves are never changed.
 */
object UiLabels {

    /** "Mutu 4a" -> "Grade 4a", "Di luar standar" -> "Off-standard", "Belum dinilai" -> "Not graded" */
    @JvmStatic
    fun grade(grade: String): String = when (grade.trim().lowercase()) {
        "di luar standar" -> "Off-standard"
        "belum dinilai" -> "Not graded"
        else -> grade.replace(Regex("(?i)mutu"), "Grade")
    }

    private val OPTIONS = mapOf(
        "Arabika" to "Arabica",
        "Besar" to "Large",
        "Sedang" to "Medium",
        "Kecil" to "Small",
        "Primer" to "Primary",
        "Sekunder" to "Secondary",
        "Asalan" to "Unsorted",
        "Campuran" to "Mixed",
        "Polyembrio" to "Polyembryo",
    )

    /** Sample input option value (e.g. "Besar") -> English label (e.g. "Large"). */
    @JvmStatic
    fun option(value: String): String = OPTIONS[value] ?: value

    private val DEFECTS = mapOf(
        "biji_berlubang_lebih_satu" to "Bean with more than one hole",
        "biji_berlubang_satu" to "Bean with one hole",
        "biji_bertutul_tutul" to "Spotted bean",
        "biji_coklat" to "Brown bean",
        "biji_gelondong" to "Coffee cherry",
        "biji_hitam" to "Black bean",
        "biji_hitam_pecah" to "Broken black bean",
        "biji_hitam_sebagian" to "Partly black bean",
        "biji_kulit_tanduk" to "Bean in parchment",
        "biji_muda" to "Immature bean",
        "biji_normal" to "Normal bean",
        "biji_pecah" to "Broken bean",
        "kulit_kopi_ukuran_besar" to "Large husk",
        "kulit_kopi_ukuran_kecil" to "Small husk",
        "kulit_kopi_ukuran_sedang" to "Medium husk",
        "kulit_tanduk_ukuran_besar" to "Large parchment",
        "kulit_tanduk_ukuran_kecil" to "Small parchment",
        "kulit_tanduk_ukuran_sedang" to "Medium parchment",
        "tanah_batu_ranting_besar" to "Large soil/stone/twig",
        "tanah_batu_ranting_kecil" to "Small soil/stone/twig",
        "tanah_batu_ranting_sedang" to "Medium soil/stone/twig",
    )

    /** Model class name (e.g. "biji_hitam") -> English label (e.g. "Black bean"). */
    @JvmStatic
    fun defect(className: String): String = DEFECTS[className] ?: className
}
