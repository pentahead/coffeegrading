package id.my.faruq.coffegrader.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.my.faruq.coffegrader.data.local.entity.ScanDefectEntity
import id.my.faruq.coffegrader.data.repository.ScanRepository
import id.my.faruq.coffegrader.ml.AggregatedDefects
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: ScanRepository,
) : ViewModel() {

    /**
     * [LAMA — TIDAK DIUBAH]
     * Simpan entri riwayat tanpa perhitungan cacat / mutu (placeholder).
     * Masih dipertahankan agar tidak ada kompilasi error jika ada pemanggil lain.
     */
    fun finishScan(
        batchName: String,
        totalBeans: Int,
        sampleInfoId: Long? = null,
        imagePath: String? = null,
        thumbnailPath: String? = null,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val dateTime = nowFormatted()
            val historyId = repository.saveScanResult(
                batchName     = batchName,
                dateTime      = dateTime,
                totalBeans    = totalBeans,
                defectScore   = 0.0,
                gradeText     = "Belum dinilai",
                scanDurationMs = 0L,
                defects       = emptyList(),
                sampleInfoId  = sampleInfoId,
                imagePath     = imagePath,
                thumbnailPath = thumbnailPath,
            )
            onDone(historyId)
        }
    }

    /**
     * [BARU] Simpan hasil lengkap setelah inferensi ML.
     *
     * @param batchName      Nama batch dari CameraViewModel.
     * @param aggregated     Output [DefectAggregator.aggregate].
     * @param gradeText      Output [GradePolicy.gradeFromScore].
     * @param scanDurationMs Durasi dari mulai capture hingga selesai inferensi.
     * @param sampleInfoId   ID sampel dari navigasi (nullable).
     * @param imagePath      Path gambar asli tersimpan.
     * @param thumbnailPath  Path thumbnail tersimpan.
     * @param onDone         Callback dengan historyId untuk navigasi ke detail.
     */
    fun finishScanFromDetections(
        batchName: String,
        aggregated: AggregatedDefects,
        gradeText: String,
        scanDurationMs: Long,
        sampleInfoId: Long? = null,
        imagePath: String? = null,
        thumbnailPath: String? = null,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val dateTime = nowFormatted()

            // Konversi DefectRow → ScanDefectEntity (historyId diisi oleh repository)
            val defectEntities = aggregated.rows
                .filter { it.defectValue > 0.0 } // skip biji_normal (bobot = 0)
                .map { row ->
                    ScanDefectEntity(
                        historyId   = 0L, // akan di-overwrite di ScanRepository.saveScanResult
                        defectName  = row.defectName,
                        defectCount = row.count,
                        defectValue = row.defectValue,
                    )
                }

            val historyId = repository.saveScanResult(
                batchName      = batchName,
                dateTime       = dateTime,
                totalBeans     = aggregated.totalBeans,
                defectScore    = aggregated.totalScore,
                gradeText      = gradeText,
                scanDurationMs = scanDurationMs,
                defects        = defectEntities,
                sampleInfoId   = sampleInfoId,
                imagePath      = imagePath,
                thumbnailPath  = thumbnailPath,
            )
            onDone(historyId)
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private fun nowFormatted(): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
}
