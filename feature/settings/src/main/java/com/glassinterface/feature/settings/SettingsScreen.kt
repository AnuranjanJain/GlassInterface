package com.glassinterface.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glassinterface.core.common.SceneMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val config by viewModel.alertConfig.collectAsStateWithLifecycle()
    var serverUrlInput by remember(config.serverUrl) { mutableStateOf(config.serverUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- External Camera Toggle ---
            SectionHeader("Camera Source")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use External Wi-Fi Camera",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = config.useExternalCamera,
                    onCheckedChange = { viewModel.onExternalCameraChanged(it) }
                )
            }

            // --- Stream URL ---
            SectionHeader("ESP32-CAM Stream URL")
            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = {
                    serverUrlInput = it
                    viewModel.onServerUrlChanged(it)
                },
                label = { Text("Stream URL") },
                placeholder = { Text("http://192.168.4.1:81/stream") },
                singleLine = true,
                enabled = config.useExternalCamera,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Enter the MJPEG stream URL of your ESP32-CAM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- Alert Sensitivity ---
            SectionHeader("Alert Sensitivity")
            Text(
                text = "Threshold: ${(config.sensitivity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = config.sensitivity,
                onValueChange = { viewModel.onSensitivityChanged(it) },
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Lower = more alerts, Higher = only high-confidence detections",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- Scene Mode ---
            SectionHeader("Scene Mode")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SceneMode.entries.forEach { mode ->
                    FilterChip(
                        selected = config.mode == mode,
                        onClick = { viewModel.onSceneModeChanged(mode) },
                        label = {
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() }
                            )
                        }
                    )
                }
            }

            // --- Alert Cooldown ---
            SectionHeader("Alert Cooldown")
            val cooldownSeconds = config.cooldownMs / 1000f
            Text(
                text = "Minimum gap: ${"%.1f".format(cooldownSeconds)}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = cooldownSeconds,
                onValueChange = { viewModel.onCooldownChanged((it * 1000).toLong()) },
                valueRange = 0.5f..10f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Hands-Free Accessibility ---
            SectionHeader("Hands-Free Accessibility")

            // Tap Anywhere
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap Anywhere to Talk",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = config.tapAnywhere,
                    onCheckedChange = { viewModel.onTapAnywhereChanged(it) }
                )
            }
            Text("Tap anywhere on the screen to trigger the assistant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Bluetooth Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bluetooth Button Trigger",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = config.headsetOnClick,
                    onCheckedChange = { viewModel.onHeadsetOnClickChanged(it) }
                )
            }
            Text("Press Play/Pause on Bluetooth headset to talk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Shake to Wake
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shake to Wake",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = config.shakeToWake,
                    onCheckedChange = { viewModel.onShakeToWakeChanged(it) }
                )
            }
            Text("Shake the phone twice quickly to talk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Proximity Wake
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proximity Wave",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = config.proximityWake,
                    onCheckedChange = { viewModel.onProximityWakeChanged(it) }
                )
            }
            Text("Wave a hand closely over the top of the phone to talk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // --- Gemini API Key ---
            SectionHeader("🤖 Gemini AI Assistant")

            val geminiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
            var geminiKeyInput by remember(geminiKey) { mutableStateOf(geminiKey) }

            OutlinedTextField(
                value = geminiKeyInput,
                onValueChange = {
                    geminiKeyInput = it
                    viewModel.onGeminiApiKeyChanged(it)
                },
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Free key from aistudio.google.com. Enables Q&A, smart responses, and conversational mode.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
