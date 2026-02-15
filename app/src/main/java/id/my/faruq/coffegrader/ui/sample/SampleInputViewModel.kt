package id.my.faruq.coffegrader.ui.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.my.faruq.coffegrader.data.local.entity.SampleInfoEntity
import id.my.faruq.coffegrader.data.repository.SampleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SampleInputViewModel @Inject constructor(
    private val repository: SampleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SampleInputState())
    val uiState = _uiState.asStateFlow()

    // ================= UPDATE FIELD =================

    fun updateBatchId(v: String) {
        _uiState.value = _uiState.value.copy(batchId = v)
    }

    fun updateCoffeeType(v: String) {
        _uiState.value = _uiState.value.copy(coffeeType = v)
    }

    fun updateProcessing(v: String) {
        // ukuran reset otomatis
        val defaultSize = if (v == "Wet") "Sedang" else "Besar"
        _uiState.value = _uiState.value.copy(
            processingMethod = v,
            beanSize = defaultSize
        )
    }

    fun updateBeanSize(v: String) {
        _uiState.value = _uiState.value.copy(beanSize = v)
    }

    fun updateBeanShape(v: String) {
        _uiState.value = _uiState.value.copy(beanShape = v)
    }

    fun updateSortation(v: String) {
        _uiState.value = _uiState.value.copy(sortationType = v)
    }

    fun toggleInsect(v: Boolean) {
        _uiState.value = _uiState.value.copy(hasInsect = v)
    }

    fun toggleMold(v: Boolean) {
        _uiState.value = _uiState.value.copy(hasMoldSmell = v)
    }

    fun updateMoisture(v: String) {
        _uiState.value = _uiState.value.copy(moisture = v)
    }

    fun updateDirt(v: String) {
        _uiState.value = _uiState.value.copy(dirt = v)
    }

    // ================= VALIDASI SNI =================

    fun isFormValid(): Boolean {
        val s = _uiState.value

        val moistureVal = s.moisture.toFloatOrNull()
        val dirtVal = s.dirt.toFloatOrNull()

        return s.batchId.isNotBlank()
                && moistureVal != null
                && dirtVal != null
                && moistureVal <= 12.5f   // SNI max
                && dirtVal <= 0.5f        // SNI max
    }

    // ================= SAVE =================

    fun saveSample(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {

            val s = _uiState.value
            val moistureVal = s.moisture.toFloatOrNull() ?: 0f
            val dirtVal = s.dirt.toFloatOrNull() ?: 0f

            val sample = SampleInfoEntity(
                batchId = s.batchId,
                coffeeType = s.coffeeType,
                processingMethod = s.processingMethod,
                beanSize = s.beanSize,
                beanShape = s.beanShape,

                hasInsect = s.hasInsect,
                hasMoldSmell = s.hasMoldSmell,

                moistureContent = moistureVal,
                dirtContent = dirtVal,

                sortationType = s.sortationType
            )

            val id = repository.saveSample(sample)
            onSuccess(id)
        }
    }
}
