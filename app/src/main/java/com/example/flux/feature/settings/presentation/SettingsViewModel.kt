package com.example.flux.feature.settings.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.feature.settings.domain.SettingsFeatureGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExportingBackup: Boolean = false,
    val isImportingBackup: Boolean = false,
    val trashSummary: TrashSummary = TrashSummary()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsFeatureGateway: SettingsFeatureGateway
) : ViewModel() {

    private val _isExportingBackup = MutableStateFlow(false)
    val isExportingBackup = _isExportingBackup.asStateFlow()

    private val _isImportingBackup = MutableStateFlow(false)
    val isImportingBackup = _isImportingBackup.asStateFlow()

    val trashSummary: StateFlow<TrashSummary> = settingsFeatureGateway.observeTrashSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSummary()
        )

    val uiState: StateFlow<SettingsUiState> = combine(
        _isExportingBackup,
        _isImportingBackup,
        trashSummary
    ) { isExporting, isImporting, trashSummary ->
        SettingsUiState(
            isExportingBackup = isExporting,
            isImportingBackup = isImporting,
            trashSummary = trashSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun exportBackup(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isExportingBackup.value = true
            val success = runCatching { settingsFeatureGateway.exportBackup(context, uri) }.isSuccess
            _isExportingBackup.value = false
            onDone(success)
        }
    }

    fun importBackup(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isImportingBackup.value = true
            val success = runCatching { settingsFeatureGateway.importBackup(context, uri) }.isSuccess
            _isImportingBackup.value = false
            onDone(success)
        }
    }
}
