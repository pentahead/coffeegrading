package id.my.faruq.coffegrader.ui.history

// ===== AndroidX Lifecycle =====
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel

import id.my.faruq.coffegrader.data.repository.DefectRowUi
import id.my.faruq.coffegrader.data.repository.HistoryDetailUi
import id.my.faruq.coffegrader.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<HistoryDetailUi?>(null)
    val detail = _detail.asStateFlow()

    private val _defects = MutableStateFlow<List<DefectRowUi>>(emptyList())
    val defects = _defects.asStateFlow()

    private fun computeDefectiveBeans(defects: List<DefectRowUi>): Int {
        return defects.sumOf { it.count }
    }

    private fun computeDominantDefect(defects: List<DefectRowUi>): String {
        if (defects.isEmpty()) return "-"
        return defects.maxByOrNull { it.totalScore }?.defectName ?: "-"
    }

    fun load(historyId: Long) {
        viewModelScope.launch {
            repository.getDetail(historyId).catch { _ -> _detail.value = null }.collect { result ->
                val derivedDefectiveBeans = computeDefectiveBeans(_defects.value)
                val derivedDominantDefect = computeDominantDefect(_defects.value)
                _detail.value = result?.copy(
                    defectiveBeans = derivedDefectiveBeans,
                    dominantDefect = derivedDominantDefect
                )
            }
        }
        viewModelScope.launch {
            repository.getDefects(historyId).catch { _ -> _defects.value = emptyList() }.collect { list ->
                _defects.value = list
                val currentDetail = _detail.value ?: return@collect
                val derivedDefectiveBeans = computeDefectiveBeans(list)
                val derivedDominantDefect = computeDominantDefect(list)
                _detail.value = currentDetail.copy(
                    defectiveBeans = derivedDefectiveBeans,
                    dominantDefect = derivedDominantDefect
                )
            }
        }
    }

    fun updateBatchName(historyId: Long, batchName: String, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBatchName(historyId, batchName)
            _detail.value = _detail.value?.copy(batchName = batchName)
            onUpdated()
        }
    }
}
