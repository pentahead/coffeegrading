package id.my.faruq.coffegrader.ml

/**
 * Bobot nilai cacat per kelas sesuai SNI 01-2907-2008.
 * Satu-satunya sumber kebenaran — jangan duplikasi di file lain.
 *
 * Tabel SNI resmi (nilai cacat per biji/kotoran, sampel 300 g):
 *   No  Jenis Cacat                      Bobot
 *   1   Biji hitam                       1
 *   2   Biji hitam sebagian              0.5
 *   3   Biji hitam pecah                 0.5
 *   4   Kopi gelondong                   1
 *   5   Biji coklat                      0.25
 *   6   Kulit kopi ukuran besar          1
 *   7   Kulit kopi ukuran sedang         0.5
 *   8   Kulit kopi ukuran kecil          0.2
 *   9   Biji berkulit tanduk             0.5
 *   10  Kulit tanduk ukuran besar        0.5
 *   11  Kulit tanduk ukuran sedang       0.2
 *   12  Kulit tanduk ukuran kecil        0.1
 *   13  Biji pecah                       0.2
 *   14  Biji muda                        0.2
 *   15  Biji berlubang satu              0.1
 *   16  Biji berlubang lebih dari satu   0.2  ← KOREKSI dari 0.5
 *   17  Biji bertutul-tutul              0.1
 *   18  Ranting/tanah/batu besar         5
 *   19  Ranting/tanah/batu sedang        2
 *   20  Ranting/tanah/batu kecil         1
 */
object DefectWeights {

    /**
     * Map classIndex → bobot SNI.
     * Index sesuai CoffeeLabelTaxonomy.NAMES.
     */
    private val WEIGHTS: Map<Int, Double> = mapOf(
        0  to 0.2,   // biji_berlubang_lebih_satu  → SNI no.16: 0.2  ← KOREKSI
        1  to 0.1,   // biji_berlubang_satu         → SNI no.15: 0.1
        2  to 0.1,   // biji_bertutul_tutul          → SNI no.17: 0.1
        3  to 0.25,  // biji_coklat                  → SNI no.5:  0.25
        4  to 1.0,   // biji_gelondong               → SNI no.4:  1
        5  to 1.0,   // biji_hitam                   → SNI no.1:  1
        6  to 0.5,   // biji_hitam_pecah              → SNI no.3:  0.5
        7  to 0.5,   // biji_hitam_sebagian           → SNI no.2:  0.5
        8  to 0.5,   // biji_kulit_tanduk             → SNI no.9:  0.5
        9  to 0.2,   // biji_muda                     → SNI no.14: 0.2
        10 to 0.0,   // biji_normal                   → bukan cacat
        11 to 0.2,   // biji_pecah                    → SNI no.13: 0.2
        12 to 1.0,   // kulit_kopi_ukuran_besar       → SNI no.6:  1
        13 to 0.2,   // kulit_kopi_ukuran_kecil       → SNI no.8:  0.2
        14 to 0.5,   // kulit_kopi_ukuran_sedang      → SNI no.7:  0.5
        15 to 0.5,   // kulit_tanduk_ukuran_besar     → SNI no.10: 0.5
        16 to 0.1,   // kulit_tanduk_ukuran_kecil     → SNI no.12: 0.1
        17 to 0.2,   // kulit_tanduk_ukuran_sedang    → SNI no.11: 0.2
        18 to 5.0,   // tanah_batu_ranting_besar      → SNI no.18: 5
        19 to 1.0,   // tanah_batu_ranting_kecil      → SNI no.20: 1
        20 to 2.0,   // tanah_batu_ranting_sedang     → SNI no.19: 2
    )

    /** Bobot untuk classIndex; 0.0 jika tidak ditemukan (termasuk kelas normal). */
    fun weightOf(classIndex: Int): Double = WEIGHTS.getOrDefault(classIndex, 0.0)
}
