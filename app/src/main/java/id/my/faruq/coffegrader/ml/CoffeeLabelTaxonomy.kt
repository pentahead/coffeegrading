package id.my.faruq.coffegrader.ml

/**
 * Satu-satunya sumber kebenaran urutan kelas model YOLO11n-seg.
 * Urutan HARUS sama persis dengan channel kelas di output tensor model.
 */
object CoffeeLabelTaxonomy {

    /** Nama kelas sesuai index output model (index 0..20). */
    val NAMES: List<String> = listOf(
        "biji_berlubang_lebih_satu",  // 0
        "biji_berlubang_satu",        // 1
        "biji_bertutul_tutul",        // 2
        "biji_coklat",                // 3
        "biji_gelondong",             // 4
        "biji_hitam",                 // 5
        "biji_hitam_pecah",           // 6
        "biji_hitam_sebagian",        // 7
        "biji_kulit_tanduk",          // 8
        "biji_muda",                  // 9
        "biji_normal",                // 10  ← bukan cacat
        "biji_pecah",                 // 11
        "kulit_kopi_ukuran_besar",    // 12
        "kulit_kopi_ukuran_kecil",    // 13
        "kulit_kopi_ukuran_sedang",   // 14
        "kulit_tanduk_ukuran_besar",  // 15
        "kulit_tanduk_ukuran_kecil",  // 16
        "kulit_tanduk_ukuran_sedang", // 17
        "tanah_batu_ranting_besar",   // 18
        "tanah_batu_ranting_kecil",   // 19
        "tanah_batu_ranting_sedang",  // 20
    )

    /** Index kelas yang BUKAN cacat — tidak dihitung ke defect score. */
    const val NORMAL_CLASS_INDEX: Int = 10

    val NUM_CLASSES: Int get() = NAMES.size
}
