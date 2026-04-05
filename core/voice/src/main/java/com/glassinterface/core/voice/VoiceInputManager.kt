package com.glassinterface.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's [SpeechRecognizer] into push-to-talk voice input.
 *
 * Call [startListening] when the user taps the mic button.
 * Results are emitted via [lastResult] (raw text) and [lastCommand] (parsed intent).
 * Observe [isListening] for UI state.
 */
@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceInputManager"
        private val WAKE_WORDS_PATTERN = Regex("\\b(hey gi|hi gi|hey g|hi g|hey gee|hi gee|hi|hey|hello|ok)\\b", RegexOption.IGNORE_CASE)
    }

    private var recognizer: SpeechRecognizer? = null
    var isContinuousMode = false
    var isAwaitingCommand = false


    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastResult = MutableStateFlow<String?>(null)
    /** Raw speech-to-text result. */
    val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    /** Parsed [VoiceCommand] from the last speech result. */
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Start listening for a voice command.
     * Must be called from the main thread.
     */
    fun startListening() {
        if (isContinuousMode && recognizer != null) {
            // Already initialized and might be listening, or we just restart it
        } else {
            if (_isListening.value) {
                Log.w(TAG, "Already listening, ignoring duplicate request")
                return
            }
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device")
            _error.value = "Speech recognition not available"
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        _isListening.value = true
        _error.value = null
        recognizer?.startListening(intent)
        Log.i(TAG, "Started listening...")
    }

    fun stopListening() {
        recognizer?.stopListening()
        isContinuousMode = false
        _isListening.value = false
        Log.i(TAG, "Stopped listening")
    }

    fun startContinuousListening() {
        isContinuousMode = true
        startListening()
    }

    fun stopContinuousListening() {
        isContinuousMode = false
        stopListening()
    }

    fun destroy() {
        isContinuousMode = false
        recognizer?.destroy()
        recognizer = null
        _isListening.value = false
    }

    /** Clear the last command so it isn't re-processed. */
    fun consumeCommand() {
        _lastCommand.value = null
        _lastResult.value = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
            if (!isContinuousMode) {
                _isListening.value = false
            }
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that, try again"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                else -> "Recognition error: $error"
            }
            Log.e(TAG, errorMsg)
            _error.value = errorMsg
            
            if (isContinuousMode) {
                // Ignore silent timeouts in continuous mode and just restart
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_CLIENT) {
                    startListening()
                } else {
                    _isListening.value = false
                }
            } else {
                _isListening.value = false
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            Log.i(TAG, "Recognized: \"$text\"")

            var commandText = text
            var wakeWordDetected = false

            if (isContinuousMode) {
                val lowerText = text.lowercase()
                val match = WAKE_WORDS_PATTERN.find(lowerText)
                if (match != null) {
                    wakeWordDetected = true
                    commandText = text.substring(match.range.last + 1).trim()
                }

                if (wakeWordDetected) {
                    Log.i(TAG, "Wake word detected! Command: $commandText")
                    _lastResult.value = commandText
                    
                    if (commandText.isNotEmpty()) {
                        _lastCommand.value = VoiceCommandParser.parse(commandText)
                        isAwaitingCommand = false
                    } else {
                        // Just the wake word, send a special command to acknowledge
                        _lastCommand.value = VoiceCommand(com.glassinterface.core.voice.CommandType.UNKNOWN, "WAKE_WORD_ACK")
                        isAwaitingCommand = true
                    }
                } else if (isAwaitingCommand && text.isNotEmpty()) {
                    Log.i(TAG, "Command received after wake word: $text")
                    _lastResult.value = text
                    _lastCommand.value = VoiceCommandParser.parse(text)
                    isAwaitingCommand = false
                }
                
                // Restart listening for continuous mode
                startListening()
            } else {
                _lastResult.value = text
                _lastCommand.value = VoiceCommandParser.parse(text)
                _isListening.value = false
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (isContinuousMode) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase() ?: ""
                
                if (WAKE_WORDS_PATTERN.containsMatchIn(text)) {
                    Log.i(TAG, "Partial Wake word detected")
                    _lastCommand.value = VoiceCommand(com.glassinterface.core.voice.CommandType.UNKNOWN, "WAKE_WORD_ACK")
                }
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
