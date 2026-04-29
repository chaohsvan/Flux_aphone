package com.example.flux.feature.settings.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.core.database.entity.CalendarSubscriptionEntity
import com.example.flux.core.domain.settings.ImportBackupMode
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.core.settings.WeatherAppBinding
import com.example.flux.feature.settings.domain.SettingsFeatureGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
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
    val isSyncingCalendar: Boolean = false,
    val weekStartDay: Int = Calendar.SUNDAY,
    val reminderSoundEnabled: Boolean = true,
    val weatherAppBinding: WeatherAppBinding? = null,
    val calendarSubscriptions: List<CalendarSubscriptionEntity> = emptyList(),
    val trashSummary: TrashSummary = TrashSummary()
)

private data class SettingsOperationState(
    val isExportingBackup: Boolean = false,
    val isImportingBackup: Boolean = false,
    val isSyncingCalendar: Boolean = false
)

private data class SettingsContentState(
    val weekStartDay: Int = Calendar.SUNDAY,
    val reminderSoundEnabled: Boolean = true,
    val weatherAppBinding: WeatherAppBinding? = null,
    val calendarSubscriptions: List<CalendarSubscriptionEntity> = emptyList(),
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

    private val _isSyncingCalendar = MutableStateFlow(false)
    val isSyncingCalendar = _isSyncingCalendar.asStateFlow()

    val calendarSubscriptions: StateFlow<List<CalendarSubscriptionEntity>> =
        settingsFeatureGateway.observeCalendarSubscriptions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val weekStartDay: StateFlow<Int> = settingsFeatureGateway.observeWeekStartDay()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Calendar.SUNDAY
        )

    val weatherAppBinding: StateFlow<WeatherAppBinding?> =
        settingsFeatureGateway.observeWeatherAppBinding()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    val reminderSoundEnabled: StateFlow<Boolean> = settingsFeatureGateway.observeReminderSoundEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val trashSummary: StateFlow<TrashSummary> = settingsFeatureGateway.observeTrashSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSummary()
        )

    private val operationState: StateFlow<SettingsOperationState> = combine(
        _isExportingBackup,
        _isImportingBackup,
        _isSyncingCalendar
    ) { isExporting, isImporting, isSyncingCalendar ->
        SettingsOperationState(
            isExportingBackup = isExporting,
            isImportingBackup = isImporting,
            isSyncingCalendar = isSyncingCalendar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsOperationState()
    )

    private val contentState: StateFlow<SettingsContentState> = combine(
        weekStartDay,
        reminderSoundEnabled,
        weatherAppBinding,
        calendarSubscriptions,
        trashSummary
    ) { weekStartDay, reminderSoundEnabled, weatherAppBinding, calendarSubscriptions, trashSummary ->
        SettingsContentState(
            weekStartDay = weekStartDay,
            reminderSoundEnabled = reminderSoundEnabled,
            weatherAppBinding = weatherAppBinding,
            calendarSubscriptions = calendarSubscriptions,
            trashSummary = trashSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsContentState()
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        operationState,
        contentState
    ) { operation, content ->
        SettingsUiState(
            isExportingBackup = operation.isExportingBackup,
            isImportingBackup = operation.isImportingBackup,
            isSyncingCalendar = operation.isSyncingCalendar,
            weekStartDay = content.weekStartDay,
            reminderSoundEnabled = content.reminderSoundEnabled,
            weatherAppBinding = content.weatherAppBinding,
            calendarSubscriptions = content.calendarSubscriptions,
            trashSummary = content.trashSummary
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

    fun importBackup(context: Context, uri: Uri, incremental: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isImportingBackup.value = true
            val mode = if (incremental) ImportBackupMode.Merge else ImportBackupMode.Replace
            val success = runCatching { settingsFeatureGateway.importBackup(context, uri, mode) }.isSuccess
            _isImportingBackup.value = false
            onDone(success)
        }
    }

    fun setWeekStartDay(value: Int) {
        viewModelScope.launch {
            settingsFeatureGateway.setWeekStartDay(value)
        }
    }

    fun setReminderSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsFeatureGateway.setReminderSoundEnabled(enabled)
        }
    }

    fun setWeatherAppBinding(binding: WeatherAppBinding) {
        viewModelScope.launch {
            settingsFeatureGateway.setWeatherAppBinding(binding)
        }
    }

    fun clearWeatherAppBinding() {
        viewModelScope.launch {
            settingsFeatureGateway.clearWeatherAppBinding()
        }
    }

    fun addCalendarSubscription(name: String, icsUrl: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncingCalendar.value = true
            val result = runCatching {
                val subscription = settingsFeatureGateway.addCalendarSubscription(name, icsUrl)
                settingsFeatureGateway.syncCalendarSubscription(subscription.id)
            }
            _isSyncingCalendar.value = false
            result.fold(
                onSuccess = { syncResult ->
                    onDone(
                        true,
                        "已同步：新增 ${syncResult.inserted}，更新 ${syncResult.updated}，删除 ${syncResult.deleted}"
                    )
                },
                onFailure = { throwable ->
                    onDone(false, throwable.message ?: "ICS 同步失败")
                }
            )
        }
    }

    fun syncCalendarSubscription(id: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncingCalendar.value = true
            val result = runCatching { settingsFeatureGateway.syncCalendarSubscription(id) }
            _isSyncingCalendar.value = false
            result.fold(
                onSuccess = { syncResult ->
                    val message = if (syncResult.downloaded) {
                        "已同步：新增 ${syncResult.inserted}，更新 ${syncResult.updated}，删除 ${syncResult.deleted}"
                    } else {
                        "日历没有变化"
                    }
                    onDone(true, message)
                },
                onFailure = { throwable ->
                    onDone(false, throwable.message ?: "ICS 同步失败")
                }
            )
        }
    }

    fun updateCalendarSubscription(
        id: String,
        name: String,
        icsUrl: String,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching { settingsFeatureGateway.updateCalendarSubscription(id, name, icsUrl) }
            result.fold(
                onSuccess = { onDone(true, "已更新订阅") },
                onFailure = { throwable -> onDone(false, throwable.message ?: "订阅更新失败") }
            )
        }
    }

    fun setCalendarSubscriptionEnabled(
        id: String,
        enabled: Boolean,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching { settingsFeatureGateway.setCalendarSubscriptionEnabled(id, enabled) }
            result.fold(
                onSuccess = { onDone(true, if (enabled) "已启用订阅" else "已停用订阅") },
                onFailure = { throwable -> onDone(false, throwable.message ?: "订阅状态更新失败") }
            )
        }
    }

    fun deleteCalendarSubscription(id: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { settingsFeatureGateway.deleteCalendarSubscription(id) }
            result.fold(
                onSuccess = { onDone(true, "已删除订阅和对应事件") },
                onFailure = { throwable -> onDone(false, throwable.message ?: "订阅删除失败") }
            )
        }
    }
}
