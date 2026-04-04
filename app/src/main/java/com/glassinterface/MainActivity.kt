package com.glassinterface

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.glassinterface.core.tts.TTSManager
import com.glassinterface.core.voice.VoiceInputManager
import com.glassinterface.ui.GlassNavHost
import com.glassinterface.ui.theme.GlassInterfaceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ttsManager: TTSManager

    @Inject
    lateinit var voiceInputManager: VoiceInputManager

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            setupUI()
        } else {
            finish()
        }
    }

    @Inject
    lateinit var settingsRepository: com.glassinterface.feature.settings.SettingsRepository

    private var headsetEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize TTS early
        ttsManager.initialize()

        // Observe headset toggle
        lifecycleScope.launch {
            settingsRepository.alertConfig.collect { config ->
                headsetEnabled = config.headsetOnClick
            }
        }

        // Check permissions
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            setupUI()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun setupUI() {
        setContent {
            GlassInterfaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GlassNavHost()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (headsetEnabled && (keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK || 
            keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
            
            if (event?.repeatCount == 0) {
                if (voiceInputManager.isListening.value) {
                    voiceInputManager.stopListening()
                } else {
                    ttsManager.stop()
                    voiceInputManager.startListening()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        voiceInputManager.destroy()
    }
}
