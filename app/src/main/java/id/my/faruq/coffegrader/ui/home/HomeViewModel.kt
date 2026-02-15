package id.my.faruq.coffegrader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.my.faruq.coffegrader.data.local.entity.ScanHistoryEntity
import id.my.faruq.coffegrader.data.repository.ScanRepository
import id.my.faruq.coffegrader.util.GradeColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanRepository: ScanRepository
) : ViewModel() {

    val recentScans = scanRepository.getAllHistory()
        .map { list ->
            list.take(3).map { entity -> entity.toScanHistoryItem() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

private fun ScanHistoryEntity.toScanHistoryItem(): ScanHistoryItem {
    val (datePart, timePart) = if (dateTime.contains(" ")) {
        val parts = dateTime.split(" ", limit = 2)
        Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { dateTime })
    } else {
        val parts = dateTime.split("/").map { it.trim() }
        Pair(parts.getOrElse(1) { "" }, parts.getOrElse(0) { dateTime })
    }
    val dateTimeText = if (timePart.isNotEmpty() && datePart.isNotEmpty()) "$timePart / $datePart" else dateTime
    val gradeColor = Color(GradeColors.colorFor(gradeText))
    return ScanHistoryItem(
        id = id.toString(),
        batchName = batchName,
        dateTimeText = dateTimeText.ifEmpty { dateTime },
        gradeText = gradeText,
        gradeColor = gradeColor
    )
}
