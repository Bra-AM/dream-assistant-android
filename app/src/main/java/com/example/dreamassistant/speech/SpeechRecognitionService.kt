package com.example.dreamassistant.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * SpeechRecognitionService optimized for Sister's Dream Assistant
 * Enhanced for her specific speech patterns and accessibility needs
 */
class SpeechRecognitionService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: Any? = null // Store for proper focus management

    private val _speechResults = Channel<SpeechResult>(Channel.BUFFERED)
    val speechResults: Flow<SpeechResult> = _speechResults.receiveAsFlow()

    // Enhanced results with confidence and timing for sister's model
    sealed class SpeechResult {
        data class Success(
            val text: String,
            val confidence: Float? = null,
            val processingTime: Long = 0L,
            val allResults: List<String> = emptyList()
        ) : SpeechResult()

        data class Error(val message: String, val errorCode: Int = -1) : SpeechResult()
        object Listening : SpeechResult()
        object NotListening : SpeechResult()
        data class PartialResult(val text: String, val confidence: Float? = null) : SpeechResult()
        data class AudioLevelChanged(val level: Float) : SpeechResult()
    }

    private var recognitionStartTime: Long = 0L

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d("SpeechService", "🎤 Speech Recognition Service initialized for sister's voice")
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setWillPauseWhenDucked(false)
                    .build()

                audioFocusRequest = request
                audioManager?.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            val success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            Log.d("SpeechService", if (success) "✅ Audio focus granted" else "❌ Audio focus denied")
            success

        } catch (e: Exception) {
            Log.e("SpeechService", "❌ Error requesting audio focus: ${e.message}")
            false
        }
    }

    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    audioManager?.abandonAudioFocusRequest(request as android.media.AudioFocusRequest)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
            Log.d("SpeechService", "🔇 Audio focus released")
        } catch (e: Exception) {
            Log.e("SpeechService", "❌ Error releasing audio focus: ${e.message}")
        }
    }

    fun startListening() {
        Log.d("SpeechService", "🚀 Starting speech recognition for sister's voice...")
        recognitionStartTime = System.currentTimeMillis()

        // Enhanced permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("SpeechService", "❌ Missing microphone permission")
            _speechResults.trySend(SpeechResult.Error(
                "Necesito permiso para usar el micrófono. Ve a configuración y actívalo 🎤",
                -1
            ))
            return
        }

        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechService", "❌ Speech recognition not available")
            _speechResults.trySend(SpeechResult.Error(
                "El reconocimiento de voz no está disponible en este dispositivo 📱",
                -1
            ))
            return
        }

        // Request audio focus with retry
        if (!requestAudioFocus()) {
            Log.w("SpeechService", "⚠️ Audio focus denied, but continuing...")
            _speechResults.trySend(SpeechResult.Error(
                "Hay otra app usando el micrófono. Ciérrala e intenta de nuevo 🎵",
                -1
            ))
            return
        }

        // Clean up previous instance
        speechRecognizer?.destroy()

        // Create new recognizer with error handling
        speechRecognizer = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            Log.e("SpeechService", "❌ Failed to create speech recognizer: ${e.message}")
            _speechResults.trySend(SpeechResult.Error(
                "No puedo inicializar el reconocimiento de voz. Reinicia la app 🔄",
                -1
            ))
            releaseAudioFocus()
            return
        }

        speechRecognizer?.setRecognitionListener(createEnhancedRecognitionListener())

        // Enhanced intent configuration for sister's speech patterns
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Language settings optimized for sister
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES") // Spanish Spain
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es")

            // Recognition settings
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) // Get multiple results for better accuracy
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            // User-friendly prompts
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escucho perfectamente, habla cuando quieras... 🎤")

            // Enhanced settings for accessibility
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // Use online for better accuracy
            putExtra(RecognizerIntent.EXTRA_SECURE, true)

            // Timeout settings optimized for sister's pace
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L) // 3 seconds
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L) // 2 seconds
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L) // 1 second minimum
        }

        try {
            speechRecognizer?.startListening(intent)
            _speechResults.trySend(SpeechResult.Listening)
            Log.d("SpeechService", "✅ Speech recognition started successfully")
        } catch (e: Exception) {
            Log.e("SpeechService", "❌ Error starting speech recognition: ${e.message}")
            _speechResults.trySend(SpeechResult.Error(
                "Error técnico al iniciar el micrófono. Inténtalo de nuevo 🔧",
                -1
            ))
            releaseAudioFocus()
        }
    }

    fun stopListening() {
        Log.d("SpeechService", "⏹️ Stopping speech recognition...")
        speechRecognizer?.stopListening()
        releaseAudioFocus()
    }

    fun destroy() {
        Log.d("SpeechService", "🗑️ Destroying speech recognizer...")
        speechRecognizer?.destroy()
        speechRecognizer = null
        releaseAudioFocus()
        _speechResults.trySend(SpeechResult.NotListening)
    }

    private fun createEnhancedRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("SpeechService", "✅ Ready for sister's speech - microphone active!")
            _speechResults.trySend(SpeechResult.Listening)
        }

        override fun onBeginningOfSpeech() {
            Log.d("SpeechService", "🎤 Sister started speaking - processing...")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Enhanced audio level monitoring for visual feedback
            val normalizedLevel = (rmsdB + 20f) / 20f // Normalize to 0-1
            if (normalizedLevel > 0.1f) { // Only report significant audio
                _speechResults.trySend(SpeechResult.AudioLevelChanged(normalizedLevel))
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.v("SpeechService", "📊 Audio buffer: ${buffer?.size ?: 0} bytes")
        }

        override fun onEndOfSpeech() {
            Log.d("SpeechService", "🔇 Sister finished speaking - analyzing...")
        }

        override fun onError(error: Int) {
            val processingTime = System.currentTimeMillis() - recognitionStartTime

            val (errorMessage, shouldRetry) = when (error) {
                SpeechRecognizer.ERROR_AUDIO ->
                    "Problema con el micrófono. Revisa que esté funcionando 🎤" to false
                SpeechRecognizer.ERROR_CLIENT ->
                    "Error interno. Inténtalo de nuevo 🔄" to true
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Necesito permisos de micrófono. Ve a configuración y actívalos ⚙️" to false
                SpeechRecognizer.ERROR_NETWORK ->
                    "Sin conexión a internet. Revisa tu WiFi o datos 📶" to true
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Conexión lenta. Inténtalo de nuevo 🐌" to true
                SpeechRecognizer.ERROR_NO_MATCH ->
                    "No entendí lo que dijiste. Habla más fuerte y claro 📢" to true
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "El reconocedor está ocupado. Espera un momento ⏱️" to true
                SpeechRecognizer.ERROR_SERVER ->
                    "Error del servidor de Google. Inténtalo más tarde 🌐" to true
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "No detecté tu voz. Habla más cerca del micrófono 🎤" to true
                else ->
                    "Error desconocido. Inténtalo de nuevo 🤔" to true
            }

            Log.e("SpeechService", "❌ Recognition error: $errorMessage (code: $error, time: ${processingTime}ms)")

            // Release audio focus on error
            releaseAudioFocus()

            // For common timeout issues, provide more encouraging messages
            val friendlyMessage = if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                when ((0..2).random()) {
                    0 -> "No te escuché bien. Inténtalo de nuevo, habla más fuerte 💪"
                    1 -> "Habla más claro y cerca del micrófono. ¡Tú puedes! 🎯"
                    else -> "No pasa nada, inténtalo otra vez. Te entiendo perfectamente 🌟"
                }
            } else {
                errorMessage
            }

            _speechResults.trySend(SpeechResult.Error(friendlyMessage, error))
            _speechResults.trySend(SpeechResult.NotListening)
        }

        override fun onResults(results: Bundle?) {
            val processingTime = System.currentTimeMillis() - recognitionStartTime
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            // Release audio focus when we get results
            releaseAudioFocus()

            if (matches != null && matches.isNotEmpty()) {
                val bestResult = matches[0]
                val confidenceScore = confidence?.get(0) ?: 1.0f
                val allResults = matches.toList()

                Log.d("SpeechService", "✅ Sister's speech recognized: '$bestResult'")
                Log.d("SpeechService", "📊 Confidence: $confidenceScore, Time: ${processingTime}ms")
                Log.d("SpeechService", "🎯 All results: $allResults")

                if (bestResult.isNotBlank()) {
                    _speechResults.trySend(SpeechResult.Success(
                        text = bestResult,
                        confidence = confidenceScore,
                        processingTime = processingTime,
                        allResults = allResults
                    ))
                } else {
                    Log.w("SpeechService", "⚠️ Empty result received")
                    _speechResults.trySend(SpeechResult.Error(
                        "No entendí nada. Habla más fuerte y claro 📢",
                        SpeechRecognizer.ERROR_NO_MATCH
                    ))
                }
            } else {
                Log.w("SpeechService", "⚠️ No speech results received")
                _speechResults.trySend(SpeechResult.Error(
                    "No escuché nada. Asegúrate de que el micrófono funcione 🎤",
                    SpeechRecognizer.ERROR_NO_MATCH
                ))
            }

            _speechResults.trySend(SpeechResult.NotListening)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = partialResults?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            val partialText = matches?.firstOrNull() ?: ""
            val partialConfidence = confidence?.firstOrNull()

            if (partialText.isNotBlank()) {
                Log.d("SpeechService", "📝 Sister's partial speech: '$partialText' (confidence: $partialConfidence)")
                _speechResults.trySend(SpeechResult.PartialResult(partialText, partialConfidence))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d("SpeechService", "📡 Speech event: $eventType")
        }
    }

    // Helper function to get speech recognition quality stats
    fun getRecognitionStats(): String {
        return """
            🎤 Speech Recognition Stats:
            • Service: ${if (speechRecognizer != null) "Active" else "Inactive"}
            • Audio Focus: ${if (audioFocusRequest != null) "Granted" else "Released"}
            • Last Recognition: ${if (recognitionStartTime > 0) "${System.currentTimeMillis() - recognitionStartTime}ms ago" else "Never"}
        """.trimIndent()
    }
}