package id.my.faruq.coffegrader.ml

/**
 * Satu-satunya sumber kebenaran urutan kelas model YOLO11n-seg.
 * Urutan HARUS sama persis dengan channel kelas di output tensor model.
 *
 * Model baru memakai label numerik (0, 1, 10, 11, …) — urutan di bawah
 * mengikuti ID output model, bukan urutan angka 0..20.
 */
object CoffeeLabelTaxonomy {

    /** Nama kelas sesuai index output model (index 0..20). */
    val NAMES: List<String> = listOf(
        "biji_berlubang_lebih_satu",  // ID 0  (label model: 0)
        "biji_berlubang_satu",        // ID 1  (label model: 1)
        "biji_normal",                // ID 2  (label model: 10)
        "biji_pecah",                 // ID 3  (label model: 11)
        "kulit_kopi_ukuran_besar",    // ID 4  (label model: 12)
        "kulit_kopi_ukuran_kecil",    // ID 5  (label model: 13)
        "kulit_kopi_ukuran_sedang",   // ID 6  (label model: 14)
        "kulit_tanduk_ukuran_besar",  // ID 7  (label model: 15)
        "kulit_tanduk_ukuran_kecil",  // ID 8  (label model: 16)
        "kulit_tanduk_ukuran_sedang", // ID 9  (label model: 17)
        "tanah_batu_ranting_besar",   // ID 10 (label model: 18)
        "tanah_batu_ranting_kecil",   // ID 11 (label model: 19)
        "biji_bertutul_tutul",        // ID 12 (label model: 2)
        "tanah_batu_ranting_sedang",  // ID 13 (label model: 20)
        "biji_coklat",                // ID 14 (label model: 3)
        "biji_gelondong",             // ID 15 (label model: 4)
        "biji_hitam",                 // ID 16 (label model: 5)
        "biji_hitam_pecah",           // ID 17 (label model: 6)
        "biji_hitam_sebagian",        // ID 18 (label model: 7)
        "biji_kulit_tanduk",          // ID 19 (label model: 8)
        "biji_muda",                  // ID 20 (label model: 9)
    )

    /** Index kelas yang BUKAN cacat — tidak dihitung ke defect score. */
    const val NORMAL_CLASS_INDEX: Int = 2

    val NUM_CLASSES: Int get() = NAMES.size
}
