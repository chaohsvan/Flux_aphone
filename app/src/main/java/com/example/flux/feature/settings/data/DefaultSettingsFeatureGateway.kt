package com.example.flux.feature.settings.data

import android.content.Context
import android.net.Uri
import com.example.flux.core.domain.settings.ExportBackupUseCase
import com.example.flux.core.domain.settings.ImportBackupUseCase
import com.example.flux.core.domain.trash.ObserveTrashSummaryUseCase
import com.example.flux.core.domain.trash.TrashSummary
import com.example.flux.feature.settings.domain.SettingsFeatureGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultSettingsFeatureGateway @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val observeTrashSummaryUseCase: ObserveTrashSummaryUseCase
) : SettingsFeatureGateway {

    override fun observeTrashSummary(): Flow<TrashSummary> = observeTrashSummaryUseCase()

    override suspend fun exportBackup(context: Context, uri: Uri) {
        exportBackupUseCase(context, uri)
    }

    override suspend fun importBackup(context: Context, uri: Uri) {
        importBackupUseCase(context, uri)
    }
}
