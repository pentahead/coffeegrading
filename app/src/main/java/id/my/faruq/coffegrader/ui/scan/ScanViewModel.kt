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
                defects = defects,
                sampleInfoId = sampleInfoId,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath
            )

            onDone(historyId)
        }
    }
}
