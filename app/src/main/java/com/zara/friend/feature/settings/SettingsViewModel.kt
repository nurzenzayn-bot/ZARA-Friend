package com.zara.friend.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zara.friend.data.sync.SyncManager
import com.zara.friend.domain.model.SyncStatus
import com.zara.friend.domain.model.SyncTrigger
import com.zara.friend.domain.model.UserSettings
import com.zara.friend.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel layar Pengaturan (potongan Fase 3): pengaturan sync + status sync.
 * Pengaturan lainnya lengkap di Fase 5.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings(),
        )

    /** Status sinkronisasi untuk indikator UI. */
    val syncStatus: StateFlow<SyncStatus> = syncManager.status

    fun onSyncEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(syncEnabled = enabled))
        }
    }

    fun onSyncNowClick() {
        viewModelScope.launch {
            syncManager.syncNow(SyncTrigger.MANUAL)
        }
    }
}
