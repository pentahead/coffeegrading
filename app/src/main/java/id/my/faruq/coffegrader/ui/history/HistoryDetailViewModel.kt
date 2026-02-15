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

    fun load(historyId: Long) {
        viewModelScope.launch {
            repository.getDetail(historyId).catch { _ -> _detail.value = null }.collect { result ->
                _detail.value = result
            }
        }
        viewModelScope.launch {
            repository.getDefects(historyId).catch { _ -> _defects.value = emptyList() }.collect { list ->
                _defects.value = list
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
