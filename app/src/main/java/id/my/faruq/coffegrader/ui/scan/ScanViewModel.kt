package id.my.faruq.coffegrader.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.my.faruq.coffegrader.data.local.entity.ScanDefectEntity
import id.my.faruq.coffegrader.data.repository.ScanRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    fun finishScan(
        batchName: String,
        totalBeans: Int,
        sampleInfoId: Long? = null,
        imagePath: String? = null,
        thumbnailPath: String? = null,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {

            //  waktu scan
            val dateTime = SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            //  Dummy hasil scan (nanti diganti YOLO)
            val defects = listOf(
                ScanDefectEntity(
                    historyId = 0,
                    defectName = "Biji Hitam",
                    defectCount = 2,
                    defectValue = 2.0
                ),
                ScanDefectEntity(
                    historyId = 0,
                    defectName = "Biji Pecah",
                    defectCount = 5,
                    defectValue = 1.0
                )
            )

           //  hitung total nilai cacat sesuai SNI
            val defectScore: Double = defects.sumOf {
                it.defectCount * it.defectValue
            }


            //  klasifikasi mutu kopi sesuai SNI 01-2907-2008
            val gradeText = when {
                defectScore <= 11.0 -> "Mutu 1"
                defectScore in 12.0..25.0 -> "Mutu 2"
                defectScore in 26.0..44.0 -> "Mutu 3"
                defectScore in 45.0..60.0 -> "Mutu 4a"
                defectScore in 61.0..80.0 -> "Mutu 4b"
                defectScore in 81.0..150.0 -> "Mutu 5"
                defectScore in 151.0..225.0 -> "Mutu 6"
                else -> "Di luar standar SNI"
            }



            //  simpan ke database (termasuk path gambar & thumbnail)
            val historyId = repository.saveScanResult(
                batchName = batchName,
                dateTime = dateTime,
                totalBeans = totalBeans,
                defectScore = defectScore,
                gradeText = gradeText,
                scanDurationMs = 0L,
                defects = defects,
                sampleInfoId = sampleInfoId,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath
            )

            onDone(historyId)
        }
    }

    /**
     * Finish scan berbasis hasil ML (YOLO-seg).
     * - `detectedClassIndices` dipakai untuk menghitung breakdown cacat + skor SNI
     * - `scanDurationMs` disimpan ke database agar tampil di `HistoryDetailScreen`
     *
     * Catatan:
     * - mapping `defectValue` masih placeholder dan sebaiknya disesuaikan dengan tabel SNI yang Anda pakai.
     */
    fun finishScanFromMl(
        batchName: String,
        detectedClassIndices: List<Int>,
        totalBeans: Int = detectedClassIndices.size,
        scanDurationMs: Long,
        sampleInfoId: Long? = null,
        imagePath: String? = null,
        thumbnailPath: String? = null,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val dateTime = SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            data class DefectRule(val defectName: String, val defectValue: Double)

            fun ruleForClass(classIndex: Int): DefectRule? {
                // Class index mengikuti metadata.yaml:
                // 0 broken, 1 foreign_matter, 2 full_black, 3 full_sour, 4 fungus, 5 good,
                // 6 immature, 7 insect_severe, 8 insect_slight, 9 partial_black,
                // 10 partial_sour, 11 withered
                return when (classIndex) {
                    0 -> DefectRule("Biji Pecah", 1.0)
                    1 -> DefectRule("Biji Asing", 1.0)
                    2, 9 -> DefectRule("Biji Hitam", 2.0) // full_black + partial_black
                    3, 10 -> DefectRule("Biji Masam", 1.0) // full_sour + partial_sour
                    4 -> DefectRule("Biji Berjamur", 2.0) // fungus
                    6 -> DefectRule("Biji Muda", 1.0) // immature
                    7, 8 -> DefectRule("Biji Terserang Serangga", 2.0) // insect_severe + insect_slight
                    11 -> DefectRule("Biji Keriput", 1.0) // withered
                    5 -> null // good (bukan cacat)
                    else -> null
                }
            }

            val countByDefect = mutableMapOf<String, Int>()
            val valueByDefect = mutableMapOf<String, Double>()
            for (classIndex in detectedClassIndices) {
                val rule = ruleForClass(classIndex) ?: continue
                countByDefect[rule.defectName] = (countByDefect[rule.defectName] ?: 0) + 1
                valueByDefect[rule.defectName] = rule.defectValue
            }

            val defects = countByDefect.map { (defectName, defectCount) ->
                ScanDefectEntity(
                    historyId = 0,
                    defectName = defectName,
                    defectCount = defectCount,
                    defectValue = valueByDefect[defectName] ?: 1.0
                )
            }

            val defectScore: Double = defects.sumOf { it.defectCount * it.defectValue }

            val gradeText = when {
                defectScore <= 11.0 -> "Mutu 1"
                defectScore in 12.0..25.0 -> "Mutu 2"
                defectScore in 26.0..44.0 -> "Mutu 3"
                defectScore in 45.0..60.0 -> "Mutu 4a"
                defectScore in 61.0..80.0 -> "Mutu 4b"
                defectScore in 81.0..150.0 -> "Mutu 5"
                defectScore in 151.0..225.0 -> "Mutu 6"
                else -> "Di luar standar SNI"
            }

            val historyId = repository.saveScanResult(
                batchName = batchName,
                dateTime = dateTime,
                totalBeans = totalBeans,
                defectScore = defectScore,
                gradeText = gradeText,
                scanDurationMs = scanDurationMs,
                defects = defects,
                sampleInfoId = sampleInfoId,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath
            )

            onDone(historyId)
        }
    }
}
