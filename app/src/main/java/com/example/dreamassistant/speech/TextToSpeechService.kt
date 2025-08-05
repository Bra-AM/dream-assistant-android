package com.example.dreamassistant.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*

/**
 * TextToSpeechService for Sister's Dream Assistant
 * Optimized for warm, supportive friend communication (not romantic)
 */
class TextToSpeechService(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _speakingEvents = Channel<SpeakingEvent>(Channel.BUFFERED)
    val speakingEvents: Flow<SpeakingEvent> = _speakingEvents.receiveAsFlow()

    sealed class SpeakingEvent {
        object Started : SpeakingEvent()
        object Finished : SpeakingEvent()
        data class Error(val message: String) : SpeakingEvent()
    }

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupVoiceAndLanguage()
                setupEmotionalVoiceParameters()
                setupProgressListener()
                isInitialized = true
                Log.d("TTS", "✅ Text-to-Speech initialized for sister's assistant")
            } else {
                Log.e("TTS", "❌ Text-to-Speech initialization failed")
                _speakingEvents.trySend(SpeakingEvent.Error("No puedo hablar"))
            }
        }
    }

    private fun setupVoiceAndLanguage() {
        // Try to get the best Spanish voice available
        val voices = textToSpeech?.voices
        var bestVoice: Voice? = null

        // Look for high-quality Spanish voices (prefer friendly, non-romantic tone)
        voices?.forEach { voice ->
            if (voice.locale.language == "es") {
                when {
                    // Prefer Google voices (higher quality)
                    voice.name.contains("google", ignoreCase = true) -> {
                        bestVoice = voice
                        Log.d("TTS", "🎯 Found Google Spanish voice: ${voice.name}")
                    }
                    // Prefer neural voices (more natural)
                    voice.name.contains("neural", ignoreCase = true) && (bestVoice?.name?.contains("google") != true) -> {
                        bestVoice = voice
                        Log.d("TTS", "🧠 Found neural Spanish voice: ${voice.name}")
                    }
                    // Prefer friendly voices over romantic ones
                    voice.name.contains("female", ignoreCase = true) && bestVoice == null -> {
                        bestVoice = voice
                        Log.d("TTS", "🎤 Found female Spanish voice: ${voice.name}")
                    }
                    // Any Spanish voice as last resort
                    bestVoice == null -> {
                        bestVoice = voice
                        Log.d("TTS", "🗣️ Found Spanish voice: ${voice.name}")
                    }
                }
            }
        }

        // Set the best voice found
        bestVoice?.let { selectedVoice ->
            val result = textToSpeech?.setVoice(selectedVoice)
            Log.d("TTS", "🎭 Voice set result: $result, using: ${selectedVoice.name}")
        } ?: run {
            // Fallback to locale-based language setting
            val result = textToSpeech?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale("es"))
            }
            Log.d("TTS", "🌍 Using locale-based Spanish")
        }
    }

    private fun setupEmotionalVoiceParameters() {
        // Enhanced voice parameters for supportive friend tone (not romantic)
        textToSpeech?.setSpeechRate(0.85f) // Slightly slower for warmth and clarity
        textToSpeech?.setPitch(1.08f) // Friendly pitch (not too high to avoid romantic tone)

        // Advanced voice settings if available
        try {
            // Try to set audio attributes for better quality
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            textToSpeech?.setAudioAttributes(audioAttributes)
            Log.d("TTS", "🎵 Advanced audio attributes set for sister's assistant")
        } catch (e: Exception) {
            Log.d("TTS", "📱 Using basic audio settings: ${e.message}")
        }
    }

    private fun setupProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "🗣️ Started speaking to sister with supportive voice")
                _speakingEvents.trySend(SpeakingEvent.Started)
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "✅ Finished speaking to sister")
                _speakingEvents.trySend(SpeakingEvent.Finished)
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "❌ TTS Error during speech")
                _speakingEvents.trySend(SpeakingEvent.Error("Error al hablar"))
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // This provides real-time feedback during speech
                super.onRangeStart(utteranceId, start, end, frame)
            }
        })
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("TTS", "TTS not initialized, cannot speak")
            _speakingEvents.trySend(SpeakingEvent.Error("Sistema de voz no listo"))
            return
        }

        if (text.isBlank()) {
            Log.w("TTS", "Empty text, nothing to speak")
            return
        }

        // Add emotional intelligence to the text (friend tone, not romantic)
        val emotionalText = addFriendlyEmotionalIntelligence(text)

        Log.d("TTS", "🗣️ Speaking with supportive friend tone: ${emotionalText.take(50)}...")

        // Determine emotional tone and adjust voice parameters
        adjustVoiceForSupportiveEmotion(text)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "dream_assistant_${System.currentTimeMillis()}")
        }

        val result = textToSpeech?.speak(emotionalText, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))

        if (result == TextToSpeech.ERROR) {
            Log.e("TTS", "❌ Error starting TTS")
            _speakingEvents.trySend(SpeakingEvent.Error("Error al hablar"))
        }
    }

    private fun addFriendlyEmotionalIntelligence(text: String): String {
        var emotionalText = text

        // Convert emojis to supportive friend expressions (not romantic)
        emotionalText = emotionalText
            .replace("💕", " con mucho cariño de amiga ")
            .replace("😊", " con alegría ")
            .replace("🌟", " brillantemente ")
            .replace("🚀", " con emoción ")
            .replace("💪", " con fuerza ")
            .replace("🎉", " con celebración ")
            .replace("❤️", " con cariño de amiga ")
            .replace("😔", " con comprensión ")
            .replace("🤗", " con apoyo ")
            .replace("✨", " mágicamente ")
            .replace("🔥", " apasionadamente ")
            .replace("🏆", " victoriosa ")
            .replace("🌈", " con esperanza ")
            .replace("👏", " con admiración ")
            .replace("🎯", " con precisión ")

        // Add emotional pauses and emphasis (supportive, not romantic)
        emotionalText = emotionalText
            .replace("¡", "¡") // Keep natural emphasis
            .replace("!", "! ") // Pause after exclamation
            .replace(".", ". ") // Natural pause
            .replace("?", "? ") // Question pause
            .replace(",", ", ") // Natural breathing

        // Add supportive expressions for specific contexts
        if (emotionalText.contains("increíble") || emotionalText.contains("genial")) {
            emotionalText = emotionalText.replace("increíble", "absolutamente increíble")
            emotionalText = emotionalText.replace("genial", "súper genial")
        }

        if (emotionalText.contains("lo siento") || emotionalText.contains("problema")) {
            emotionalText = emotionalText.replace("lo siento", "ay, lo siento mucho")
        }

        // Add encouraging friend language
        if (emotionalText.contains("puedes")) {
            emotionalText = emotionalText.replace("puedes", "puedes perfectamente")
        }

        if (emotionalText.contains("eres")) {
            emotionalText = emotionalText.replace("eres increíble", "eres absolutamente increíble")
            emotionalText = emotionalText.replace("eres fuerte", "eres muy fuerte")
            emotionalText = emotionalText.replace("eres capaz", "eres súper capaz")
        }

        // Add breathing for longer texts
        if (emotionalText.length > 100) {
            val words = emotionalText.split(" ")
            val chunked = words.chunked(15) // Every 15 words, add a breath
            emotionalText = chunked.joinToString(" ... ") // Natural pause
        }

        return emotionalText
    }

    private fun adjustVoiceForSupportiveEmotion(text: String) {
        val lowerText = text.lowercase()

        when {
            // Excited/Happy emotions (supportive friend energy)
            lowerText.contains("¡") || lowerText.contains("genial") ||
                    lowerText.contains("increíble") || lowerText.contains("éxito") -> {
                textToSpeech?.setSpeechRate(0.9f) // Slightly faster for excitement
                textToSpeech?.setPitch(1.12f) // Higher pitch for joy (friend level)
            }

            // Supportive/Encouraging emotions (best friend support)
            lowerText.contains("puedes") || lowerText.contains("eres") ||
                    lowerText.contains("fuerte") || lowerText.contains("capaz") -> {
                textToSpeech?.setSpeechRate(0.8f) // Slower for emphasis
                textToSpeech?.setPitch(1.0f) // Steady, confident friend pitch
            }

            // Sympathetic/Understanding emotions (caring friend)
            lowerText.contains("entiendo") || lowerText.contains("lo siento") ||
                    lowerText.contains("problema") || lowerText.contains("difícil") -> {
                textToSpeech?.setSpeechRate(0.75f) // Much slower for empathy
                textToSpeech?.setPitch(0.98f) // Slightly lower, warmer friend pitch
            }

            // Motivational/Business emotions (supportive mentor friend)
            lowerText.contains("negocio") || lowerText.contains("emprender") ||
                    lowerText.contains("plataforma") || lowerText.contains("proyecto") -> {
                textToSpeech?.setSpeechRate(0.85f) // Clear and confident
                textToSpeech?.setPitch(1.06f) // Inspiring friend pitch
            }

            // Celebration emotions (excited best friend)
            lowerText.contains("celebr") || lowerText.contains("logr") ||
                    lowerText.contains("éxito") || lowerText.contains("genial") -> {
                textToSpeech?.setSpeechRate(0.88f) // Excited but clear
                textToSpeech?.setPitch(1.1f) // Happy friend pitch
            }

            // Default warm, supportive friend tone
            else -> {
                textToSpeech?.setSpeechRate(0.85f) // Comfortable friend pace
                textToSpeech?.setPitch(1.05f) // Friendly, supportive pitch
            }
        }
    }

    fun stop() {
        textToSpeech?.stop()
        _speakingEvents.trySend(SpeakingEvent.Finished)
        Log.d("TTS", "🔇 Stopped speaking")
    }

    fun destroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        Log.d("TTS", "🔇 Text-to-Speech destroyed for sister's assistant")
    }
}