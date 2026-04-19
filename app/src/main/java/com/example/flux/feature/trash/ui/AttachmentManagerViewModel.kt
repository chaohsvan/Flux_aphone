package com.example.flux.feature.trash.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.domain.diary.AttachmentManagerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AttachmentManagerViewModel @Inject constructor(
    private val attachmentManagerUseCase: AttachmentManagerUseCase
) : ViewModel() {

    private val _orphanFiles = MutableStateFlow<List<File>>(emptyList())
    val orphanFiles = _orphanFiles.asStateFlow()

    private val _freedSpaceBytes = MutableStateFlow(0L)
    val freedSpaceBytes = _freedSpaceBytes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun scan(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            _orphanFiles.value = attachmentManagerUseCase.getOrphanFiles(context)
            _isScanning.value = false
        }
    }

    fun clean(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            val freed = attachmentManagerUseCase.cleanOrphans(context)
            _freedSpaceBytes.value = freed
            _orphanFiles.value = emptyList()
            _isScanning.value = false
        }
    }
}
