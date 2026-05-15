package id.my.faruq.coffegrader.ui.camera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.my.faruq.coffegrader.data.repository.SampleRepository
import id.my.faruq.coffegrader.ml.CoffeeInferenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sampleRepository: SampleRepository,
    val inferenceEngine: CoffeeInferenceEngine,
) : ViewModel() {

    private val sampleInfoId: Long? =
        savedStateHandle.get<String>("sampleInfoId")?.toLongOrNull()?.takeIf { it > 0 }

    private val _batchName = MutableStateFlow("")
    val batchName = _batchName.asStateFlow()

    private val _coffeeType = MutableStateFlow<String?>(null)
    val coffeeType = _coffeeType.asStateFlow()

    init {
        sampleInfoId?.let { id ->
            viewModelScope.launch {
                sampleRepository.getById(id)?.let { sample ->
                    _batchName.value = sample.batchId
                    _coffeeType.value = sample.coffeeType
                }
            }
        }
    }

    fun getSampleInfoId(): Long? = sampleInfoId
}
