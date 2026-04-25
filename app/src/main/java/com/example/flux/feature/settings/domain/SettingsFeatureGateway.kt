package com.example.flux.feature.settings.domain

import android.content.Context
import android.net.Uri
import com.example.flux.core.domain.trash.TrashSummary
import kotlinx.coroutines.flow.Flow

interface SettingsFeatureGateway {
    fun observeTrashSummary(): Flow<TrashSummary>
    suspend fun exportBackup(context: Context, uri: Uri)
    suspend fun importBackup(context: Context, uri: Uri)
}
