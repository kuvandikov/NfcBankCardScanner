/*
 * Copyright 2026 Kuvandikov and the scan-card-nfc contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kuvandikov.scancardnfc.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kuvandikov.scancardnfc.compose.rememberNfcCardScanner
import io.github.kuvandikov.scancardnfc.compose.rememberNfcEnabledState
import io.github.kuvandikov.scancardnfc.settings.openNfcSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun AddCardScreen(viewModel: AddCardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scanner = rememberNfcCardScanner()
    val nfcEnabled by rememberNfcEnabledState(scanner)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanJob by remember { mutableStateOf<Job?>(null) }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.add_card_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.add_card_subtitle), style = MaterialTheme.typography.bodyMedium)

            // The masked PAN is displayed; the full number stays in memory for the backend call.
            OutlinedTextField(
                value = state.card?.maskedPan.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.card_number)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.card?.expiry?.toString().orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.card_expiry)) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.card?.let { card ->
                Text("${stringResource(R.string.card_scheme)}: ${card.scheme.displayName}")
            }

            state.status?.let { status ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(status))
                        if (state.showNfcSettingsAction || (!nfcEnabled && scanner.isSupported)) {
                            TextButton(onClick = { context.openNfcSettings() }) {
                                Text(stringResource(R.string.action_open_nfc_settings))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (state.isScanning) {
                OutlinedButton(
                    onClick = {
                        scanJob?.cancel()
                        viewModel.onScanCancelled()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.action_cancel)) }
            } else {
                OutlinedButton(
                    onClick = {
                        viewModel.onScanStarted()
                        scanJob = scope.launch { viewModel.onScanResult(scanner.scan()) }
                    },
                    enabled = scanner.isSupported,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.action_scan)) }
            }

            Button(
                onClick = viewModel::onAddCard,
                enabled = state.card != null && !state.isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_add_card)) }
        }
    }
}
