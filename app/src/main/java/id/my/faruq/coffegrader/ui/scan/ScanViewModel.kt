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
        onDone: () -> Unit
    ) {
        viewModelScope.launch {

            // 🕒 waktu scan
            val dateTime = SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            // 🔧 Dummy hasil scan (nanti diganti YOLO)
            val defects = listOf(
                ScanDefectEntity(
                    historyId = 0,
                    defectName = "Biji Hitam",
                    defectCount = 2,
                    defectValue = 2.0f
                ),
                ScanDefectEntity(
                    historyId = 0,
                    defectName = "Biji Pecah",
                    defectCount = 5,
                    defectValue = 1.0f
                )
            )

            // 🔢 hitung skor cacat
            val defectScore = defects.sumOf {
                (it.defectCount * it.defectValue).toInt()
            }

            // 🏷️ contoh penentuan mutu (sementara)
            val gradeText = when {
                defectScore <= 3 -> "1"
                defectScore <= 6 -> "2"
                defectScore <= 10 -> "3"
                else -> "4a"
            }

            // 💾 simpan ke database
            repository.saveScanResult(
                batchName = batchName,
                dateTime = dateTime,
                totalBeans = totalBeans,
                defectScore = defectScore,
                gradeText = gradeText,
                defects = defects
            )

            onDone()
        }
    }
}
