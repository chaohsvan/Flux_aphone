package com.example.flux.feature.settings.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.domain.settings.ExportBackupUseCase
import com.example.flux.core.domain.settings.ImportBackupUseCase
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    observeTrashSummaryUseCase: ObserveTrashSummaryUseCase
) : ViewModel() {

    private val _isExportingBackup = MutableStateFlow(false)
    val isExportingBackup = _isExportingBackup.asStateFlow()

    private val _isImportingBackup = MutableStateFlow(false)
    val isImportingBackup = _isImportingBackup.asStateFlow()

    val trashSummary: StateFlow<TrashSummary> = observeTrashSummaryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSummary()
        )

    fun exportBackup(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isExportingBackup.value = true
            val success = runCatching {
                exportBackupUseCase(context, uri)
            }.isSuccess
            _isExportingBackup.value = false
            onDone(success)
        }
    }

    fun importBackup(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isImportingBackup.value = true
            val success = runCatching {
                importBackupUseCase(context, uri)
            }.isSuccess
            _isImportingBackup.value = false
            onDone(success)
        }
    }
}
