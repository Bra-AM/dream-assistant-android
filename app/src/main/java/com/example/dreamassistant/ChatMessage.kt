package com.example.dreamassistant

/**
 * Chat message data classes for Sister's Dream Assistant
 * Designed specifically for her accessibility needs and trained Gemma 3n model
 */
sealed class ChatMessage {
    abstract val text: String
    abstract val timestamp: Long

    data class User(
        override val text: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val isVoiceInput: Boolean = false,
        val processingTime: Long = 0,
        val originalSpeechText: String? = null, // Original speech before preprocessing
        val speechConfidence: Float = 1.0f,
        val preprocessedText: String? = null // Text after sister's speech pattern preprocessing
    ) : ChatMessage()

    data class Assistant(
        override val text: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val hasAction: Boolean = false,
        val actionType: String? = null,
        val wasSpoken: Boolean = true,
        val emotionalTone: String = "supportive",

        // NEW: Model-specific fields
        val isFromRealModel: Boolean = false, // Track if response came from sister's trained model
        val modelConfidence: Float = 1.0f,
        val inferenceTime: Long = 0, // Time taken to generate response

        // Sister-specific fields
        val isPersonalized: Boolean = true, // Always true for sister's responses
        val supportLevel: SupportLevel = SupportLevel.ENCOURAGING,
        val containsAction: Boolean = false, // Redundant with hasAction but keeping for compatibility

        // Response quality indicators
        val responseQuality: ResponseQuality = ResponseQuality.HIGH,
        val isBusinessAdvice: Boolean = false, // Track if response contains business/entrepreneurship advice
        val isEmotionalSupport: Boolean = false // Track if response provides emotional support
    ) : ChatMessage()

    /**
     * Support levels for sister's emotional needs
     */
    enum class SupportLevel {
        ENCOURAGING,     // General encouragement
        MOTIVATIONAL,    // Strong motivation for her business dreams
        COMFORTING,      // Emotional comfort and reassurance
        CELEBRATORY,     // Celebrating her achievements
        PRACTICAL        // Practical help and advice
    }

    /**
     * Response quality from the trained model
     */
    enum class ResponseQuality {
        HIGH,            // Perfect response from trained model
        MEDIUM,          // Good response but maybe not perfect
        LOW,             // Response had issues
        ERROR            // Model error or empty response
    }

    /**
     * Create a user message from voice input with sister's speech patterns
     */
    companion object {
        fun createVoiceMessage(
            originalSpeech: String,
            preprocessedText: String,
            confidence: Float,
            processingTime: Long
        ): User {
            return User(
                text = preprocessedText,
                isVoiceInput = true,
                originalSpeechText = originalSpeech,
                speechConfidence = confidence,
                preprocessedText = if (originalSpeech != preprocessedText) preprocessedText else null,
                processingTime = processingTime
            )
        }

        fun createTextMessage(text: String): User {
            return User(
                text = text,
                isVoiceInput = false,
                speechConfidence = 1.0f
            )
        }

        /**
         * Create assistant response from sister's trained model
         */
        fun createModelResponse(
            text: String,
            isFromRealModel: Boolean,
            inferenceTime: Long,
            hasAction: Boolean = false,
            actionType: String? = null,
            supportLevel: SupportLevel = SupportLevel.ENCOURAGING,
            isBusinessAdvice: Boolean = false,
            isEmotionalSupport: Boolean = false
        ): Assistant {
            return Assistant(
                text = text,
                hasAction = hasAction,
                actionType = actionType,
                wasSpoken = true, // Always speak for sister
                isFromRealModel = isFromRealModel,
                inferenceTime = inferenceTime,
                supportLevel = supportLevel,
                responseQuality = if (isFromRealModel && text.isNotEmpty()) ResponseQuality.HIGH else ResponseQuality.MEDIUM,
                isBusinessAdvice = isBusinessAdvice,
                isEmotionalSupport = isEmotionalSupport,
                containsAction = hasAction
            )
        }

        /**
         * Create error response
         */
        fun createErrorResponse(
            errorMessage: String,
            isModelError: Boolean = false
        ): Assistant {
            return Assistant(
                text = errorMessage,
                wasSpoken = true,
                emotionalTone = "reassuring",
                isFromRealModel = false,
                responseQuality = ResponseQuality.ERROR,
                supportLevel = SupportLevel.COMFORTING,
                isEmotionalSupport = true
            )
        }

        /**
         * Create welcome message
         */
        fun createWelcomeMessage(): Assistant {
            val welcomeText = """
                ¡Hola, hermosa! 🌟 Soy tu Dream Assistant personalizado
                
                Estoy aquí para ser tu mejor amiga y ayudarte con:
                💬 Enviar mensajes de WhatsApp
                📱 Leer tus mensajes en voz alta
                🎥 Grabar y subir videos a YouTube  
                📹 Crear reuniones de Google Meet
                🚀 Impulsar tu negocio y sueños
                💕 Darte ánimo y motivación siempre
                
                ¡Cuéntame qué quieres hacer hoy! Puedes hablarme o escribirme 😊
            """.trimIndent()

            return Assistant(
                text = welcomeText,
                wasSpoken = true,
                emotionalTone = "welcoming",
                isFromRealModel = false, // Welcome message is hardcoded
                supportLevel = SupportLevel.ENCOURAGING,
                responseQuality = ResponseQuality.HIGH,
                isPersonalized = true
            )
        }

        /**
         * Create status message about model loading
         */
        fun createStatusMessage(
            message: String,
            isModelReady: Boolean
        ): Assistant {
            return Assistant(
                text = message,
                wasSpoken = false, // Don't speak status messages
                emotionalTone = if (isModelReady) "excited" else "patient",
                isFromRealModel = false,
                supportLevel = SupportLevel.PRACTICAL,
                responseQuality = if (isModelReady) ResponseQuality.HIGH else ResponseQuality.MEDIUM
            )
        }
    }

    /**
     * Helper functions for UI and analytics
     */
    fun getDisplayTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Ahora"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            else -> "${diff / 86400_000}d"
        }
    }

    fun isFromSister(): Boolean = this is User
    fun isFromAssistant(): Boolean = this is Assistant

    /**
     * Check if message contains business/entrepreneurship content
     */
    fun isBusinessRelated(): Boolean {
        val businessKeywords = listOf(
            "negocio", "emprender", "plataforma", "comunidad", "proyecto",
            "startup", "empresa", "cliente", "marketing", "ventas", "idea"
        )
        return businessKeywords.any { text.lowercase().contains(it) }
    }

    /**
     * Check if message contains emotional support
     */
    fun isEmotionallySupportive(): Boolean {
        val supportKeywords = listOf(
            "eres increíble", "tú puedes", "muy orgullosa", "eres fuerte",
            "te entiendo", "estoy aquí", "eres capaz", "lo vas a lograr"
        )
        return supportKeywords.any { text.lowercase().contains(it) }
    }

    /**
     * Get priority for TTS (Text-to-Speech)
     * Sister should hear the most important messages first
     */
    fun getTTSPriority(): Int {
        return when (this) {
            is User -> 0 // Never speak user messages
            is Assistant -> when {
                responseQuality == ResponseQuality.ERROR -> 10 // Highest priority for errors
                isEmotionalSupport -> 9 // High priority for emotional support
                hasAction -> 8 // High priority for actions
                isBusinessAdvice -> 7 // Medium-high for business advice
                supportLevel == SupportLevel.CELEBRATORY -> 6 // Medium for celebrations
                else -> 5 // Normal priority
            }
        }
    }
}