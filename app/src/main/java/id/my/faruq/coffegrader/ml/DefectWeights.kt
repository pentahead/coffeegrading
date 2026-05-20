package id.my.faruq.coffegrader.ml

/**
 * Bobot nilai cacat per kelas sesuai SNI 01-2907-2008.
 * Satu-satunya sumber kebenaran — jangan duplikasi di file lain.
 *
 * Key = classIndex output model (urutan sama dengan [CoffeeLabelTaxonomy.NAMES]).
 */
object DefectWeights {

    /**
     * Map classIndex → bobot SNI.
     * Index sesuai CoffeeLabelTaxonomy.NAMES / ID output model baru.
     */
    private val WEIGHTS: Map<Int, Double> = mapOf(
        0  to 0.2,   // biji_berlubang_lebih_satu
        1  to 0.1,   // biji_berlubang_satu
        2  to 0.0,   // biji_normal
        3  to 0.2,   // biji_pecah
        4  to 1.0,   // kulit_kopi_ukuran_besar
        5  to 0.2,   // kulit_kopi_ukuran_kecil
        6  to 0.5,   // kulit_kopi_ukuran_sedang
        7  to 0.5,   // kulit_tanduk_ukuran_besar
        8  to 0.1,   // kulit_tanduk_ukuran_kecil
        9  to 0.2,   // kulit_tanduk_ukuran_sedang
        10 to 5.0,   // tanah_batu_ranting_besar
        11 to 1.0,   // tanah_batu_ranting_kecil
        12 to 0.1,   // biji_bertutul_tutul
        13 to 2.0,   // tanah_batu_ranting_sedang
        14 to 0.25,  // biji_coklat
        15 to 1.0,   // biji_gelondong
        16 to 1.0,   // biji_hitam
        17 to 0.5,   // biji_hitam_pecah
        18 to 0.5,   // biji_hitam_sebagian
        19 to 0.5,   // biji_kulit_tanduk
        20 to 0.2,   // biji_muda
    )

    /** Bobot untuk classIndex; 0.0 jika tidak ditemukan (termasuk kelas normal). */
    fun weightOf(classIndex: Int): Double = WEIGHTS.getOrDefault(classIndex, 0.0)
}
