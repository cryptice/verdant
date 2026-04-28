package app.verdant.android.ui.supplies
import app.verdant.android.data.repository.SupplyRepository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSupplyInventoryRequest
import app.verdant.android.data.model.CreateSupplyTypeRequest
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.model.SupplyTypeResponse
import app.verdant.android.data.model.UpdateSupplyTypeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SupplyInventoryViewModel"

/**
 * Three-state UI: cold load shows a spinner, errors that block first-load
 * show a retry, and once we have data we stay in [Loaded] — even while
 * subsequent refreshes are in flight, so the list doesn't flicker.
 */
sealed interface SupplyInventoryUiState {
    data object Loading : SupplyInventoryUiState
    data class Error(val message: String) : SupplyInventoryUiState
    data class Loaded(
        val items: List<SupplyInventoryResponse>,
        val types: List<SupplyTypeResponse>,
        val isRefreshing: Boolean = false,
        val decrementingId: Long? = null,
        val saving: Boolean = false,
    ) : SupplyInventoryUiState
}

@HiltViewModel
class SupplyInventoryViewModel @Inject constructor(
    private val supplyRepository: SupplyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SupplyInventoryUiState>(SupplyInventoryUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is SupplyInventoryUiState.Loaded) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            try {
                val items = supplyRepository.listInventory()
                val types = runCatching { supplyRepository.listTypes() }.getOrDefault(emptyList())
                _uiState.value = SupplyInventoryUiState.Loaded(items = items, types = types)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load supply inventory", e)
                _uiState.value = if (current is SupplyInventoryUiState.Loaded) {
                    current.copy(isRefreshing = false)
                } else {
                    SupplyInventoryUiState.Error(e.message ?: "Kunde inte ladda material")
                }
            }
        }
    }

    fun createInventory(
        supplyTypeId: Long,
        quantity: Double,
        costCents: Int?,
        notes: String?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val current = _uiState.value as? SupplyInventoryUiState.Loaded ?: return@launch
            _uiState.value = current.copy(saving = true)
            try {
                supplyRepository.createInventory(
                    CreateSupplyInventoryRequest(
                        supplyTypeId = supplyTypeId,
                        quantity = quantity,
                        costCents = costCents,
                        notes = notes,
                    ),
                )
                refresh()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create supply inventory", e)
                _uiState.value = current.copy(saving = false)
            }
        }
    }

    fun createSupplyType(
        name: String,
        category: String,
        unit: String,
        inexhaustible: Boolean,
        onCreated: (SupplyTypeResponse) -> Unit,
    ) {
        viewModelScope.launch {
            val current = _uiState.value as? SupplyInventoryUiState.Loaded ?: return@launch
            try {
                val created = supplyRepository.createType(
                    CreateSupplyTypeRequest(
                        name = name, category = category, unit = unit, inexhaustible = inexhaustible,
                    ),
                )
                _uiState.value = current.copy(types = (current.types + created).sortedBy { it.name })
                onCreated(created)
                if (inexhaustible) refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create supply type", e)
            }
        }
    }

    fun updateSupplyType(
        id: Long,
        name: String,
        category: String,
        unit: String,
        inexhaustible: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                supplyRepository.updateType(
                    id,
                    UpdateSupplyTypeRequest(
                        name = name, category = category, unit = unit, inexhaustible = inexhaustible,
                    ),
                )
                refresh()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update supply type", e)
            }
        }
    }

    fun deleteSupplyType(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                supplyRepository.deleteType(id)
                refresh()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete supply type", e)
            }
        }
    }

    fun decrement(id: Long, quantity: Double) {
        viewModelScope.launch {
            val current = _uiState.value as? SupplyInventoryUiState.Loaded ?: return@launch
            _uiState.value = current.copy(decrementingId = id)
            try {
                supplyRepository.decrement(id, quantity)
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrement supply", e)
                _uiState.value = current.copy(decrementingId = null)
            }
        }
    }
}
