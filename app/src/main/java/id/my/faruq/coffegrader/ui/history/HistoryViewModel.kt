package id.my.faruq.coffegrader.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.faruq.coffegrader.data.repository.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import id.my.faruq.coffegrader.data.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: HistoryRepository
) : ViewModel() {

    val historyList = repository.getAllHistory()
    init {
        viewModelScope.launch {
            repository.seedDummyIfEmpty()
        }
    }

//    val historyItems = repository.getHistoryWithSample()
//        .map { list ->
//            list.map { h ->
//
//                val dateText = SimpleDateFormat(
//                    "HH:mm:ss / yyyy-MM-dd",
//                    Locale.getDefault()
//                ).format(Date(h.timestamp))
//
//                HistoryItemUi(
//                    id = h.historyId.toString(),
//                    batchName = h.batchId,
//                    dateTimeText = dateText,
//
//                    totalBeans = 132, // sementara fixed dulu
//                    defectScore = h.totalDefectScore.toInt(),
//                    gradeText = h.gradeResult,
//
//                    cardColor = gradeColor(h.gradeResult)
//                )
//            }
//        }
//        .stateIn(
//            viewModelScope,
//            SharingStarted.WhileSubscribed(5000),
//            emptyList()
//        )

    private fun gradeColor(grade: String) = when (grade.lowercase()) {
        "1" -> androidx.compose.ui.graphics.Color(0xFF12A150)
        "2" -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        "3" -> androidx.compose.ui.graphics.Color(0xFFB45F06)
        "4a" -> androidx.compose.ui.graphics.Color(0xFFF57C00)
        else -> androidx.compose.ui.graphics.Color(0xFF8E1B1B)
    }
}
