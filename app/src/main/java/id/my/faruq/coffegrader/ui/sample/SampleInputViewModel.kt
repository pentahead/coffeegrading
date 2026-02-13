package id.my.faruq.coffegrader.ui.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.faruq.coffegrader.data.local.entity.SampleInfoEntity
import id.my.faruq.coffegrader.data.repository.SampleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SampleInputViewModel(
    private val repository: SampleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SampleInputState())
    val uiState = _uiState.asStateFlow()

    fun updateBatchId(value: String) {
        _uiState.value = _uiState.value.copy(batchId = value)
    }

    fun saveSample(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val sample = SampleInfoEntity(
                batchId = _uiState.value.batchId,
                coffeeType = "Robusta",
                processingMethod = "Dry",
                beanSize = "Besar",
                beanShape = "Normal",
                hasInsect = false,
                hasMoldSmell = false,
                moistureContent = 0f,
                dirtContent = 0f,
                sortationType = "Primer"
            )

            val id = repository.saveSample(sample)
            onSuccess(id)
        }
    }
}
