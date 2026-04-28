package app.verdant.android.ui.supplies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSupplyInventoryRequest
import app.verdant.android.data.model.CreateSupplyTypeRequest
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.model.SupplyTypeResponse
import app.verdant.android.data.model.UpdateSupplyTypeRequest
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SupplyInventoryViewModel"

data class SupplyInventoryState(
    val isLoading: Boolean = true,
    val items: List<SupplyInventoryResponse> = emptyList(),
    val types: List<SupplyTypeResponse> = emptyList(),
    val error: String? = null,
    val decrementingId: Long? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class SupplyInventoryViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplyInventoryState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = repo.getSupplyInventory()
                val types = runCatching { repo.getSupplyTypes() }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(isLoading = false, items = items, types = types)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load supply inventory", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createSupplyInventory(
                    CreateSupplyInventoryRequest(
                        supplyTypeId = supplyTypeId,
                        quantity = quantity,
                        costCents = costCents,
                        notes = notes,
                    ),
                )
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create supply inventory", e)
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
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
            try {
                val created = repo.createSupplyType(
                    CreateSupplyTypeRequest(
                        name = name, category = category, unit = unit, inexhaustible = inexhaustible,
                    ),
                )
                _uiState.value = _uiState.value.copy(types = (_uiState.value.types + created).sortedBy { it.name })
                onCreated(created)
                if (inexhaustible) refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create supply type", e)
                _uiState.value = _uiState.value.copy(error = e.message)
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
                repo.updateSupplyType(
                    id,
                    UpdateSupplyTypeRequest(
                        name = name, category = category, unit = unit, inexhaustible = inexhaustible,
                    ),
                )
                refresh()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update supply type", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteSupplyType(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteSupplyType(id)
                refresh()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete supply type", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun decrement(id: Long, quantity: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(decrementingId = id)
            try {
                repo.decrementSupply(id, quantity)
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrement supply", e)
                _uiState.value = _uiState.value.copy(decrementingId = null, error = e.message)
            }
        }
    }
}
