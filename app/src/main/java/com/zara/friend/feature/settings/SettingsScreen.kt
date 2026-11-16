package com.zara.friend.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zara.friend.R
import com.zara.friend.domain.model.SyncStatus

/**
 * Layar Pengaturan — potongan Fase 3: status sinkronisasi + tombol sync manual.
 * Pengaturan lengkap (tema, gaya sapaan, dsb.) menyusul di Fase 5.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium)

            // ---------- Sinkronisasi (Fase 3) ----------
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.sync_section_title),
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.sync_enabled_label),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.syncEnabled,
                        onCheckedChange = viewModel::onSyncEnabledChange,
                    )
                }

                Text(
                    text = syncStatusDescription(syncStatus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = viewModel::onSyncNowClick,
                    enabled = settings.syncEnabled && syncStatus !is SyncStatus.Syncing,
                ) {
                    Text(stringResource(R.string.sync_now))
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.placeholder_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun syncStatusDescription(status: SyncStatus): String = when (status) {
    is SyncStatus.Idle -> stringResource(R.string.sync_status_idle)
    is SyncStatus.Syncing -> stringResource(R.string.sync_status_syncing)
    is SyncStatus.Success ->
        if (status.summary.isSkipped) {
            stringResource(R.string.sync_status_skipped)
        } else {
            stringResource(
                R.string.sync_status_success,
                status.summary.pushedConversations,
                status.summary.pulledConversations,
            )
        }
    is SyncStatus.Error -> stringResource(R.string.sync_status_error, status.message)
}
