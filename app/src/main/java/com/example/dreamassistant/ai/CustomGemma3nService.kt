package com.example.dreamassistant.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Sister's Personalized Gemma 3n Service - REAL MODEL ONLY
 * Uses your actual trained GGUF model from Colab training
 * NO FALLBACKS - Only your sister's custom model responses
 */
class CustomGemma3nService(private val context: Context) {

    private var modelLoader: RealModelLoader? = null
    private var isInitialized = false
    private val modelPath = "models/sister_dream_assistant.gguf"

    sealed class ChatResult {
        data class Success(
            val response: String,
            val action: AssistantAction? = null,
            val shouldSpeak: Boolean = true,
            val isFromRealModel: Boolean = false
        ) : ChatResult()

        data class Error(
            val message: String,
            val shouldSpeak: Boolean = true,
            val isModelError: Boolean = false
        ) : ChatResult()

        object Loading : ChatResult()

        data class ReadAloud(val content: String, val contentType: String) : ChatResult()

        data class ModelStatus(
            val isRealModel: Boolean,
            val message: String,
            val modelInfo: String? = null
        ) : ChatResult()
    }

    sealed class AssistantAction {
        data class SendWhatsApp(val contact: String, val message: String) : AssistantAction()
        data class ReadWhatsApp(val contact: String? = null) : AssistantAction()
        data class ReadMessageAloud(val messageContent: String, val sender: String) : AssistantAction()
        data class OpenYouTube(val searchQuery: String? = null) : AssistantAction()
        data class RecordVideo(val title: String? = null) : AssistantAction()
        data class CreateMeeting(val title: String, val participants: List<String> = emptyList()) : AssistantAction()
        object OpenCamera : AssistantAction()
        data class ReadScreenContent(val content: String) : AssistantAction()
    }

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("CustomGemma3n", "🚀 Initializing Sister's REAL Gemma 3n model...")
                Log.d("CustomGemma3n", "🎯 Model trained on her 202+ voice samples")

                // Copy your sister's trained model from assets to internal storage
                val modelFile = copyModelFromAssets()
                if (modelFile == null) {
                    Log.e("CustomGemma3n", "❌ CRITICAL: Sister's trained model not found!")
                    Log.e("CustomGemma3n", "Make sure 'sister_dream_assistant.gguf' is in assets/models/")
                    return@withContext false
                }

                // Initialize the REAL model loader
                modelLoader = RealModelLoader(context)
                val success = modelLoader!!.loadModel(modelFile.absolutePath)

                if (success) {
                    isInitialized = true
                    Log.d("CustomGemma3n", "✅ SUCCESS! Sister's REAL model is ready!")
                    Log.d("CustomGemma3n", "🌟 Model understands her unique speech patterns")
                    Log.d("CustomGemma3n", "💕 Trained specifically for her entrepreneurship journey")

                    // Log model info
                    val modelInfo = modelLoader!!.getModelInfo()
                    if (modelInfo != null) {
                        Log.d("CustomGemma3n", "📊 Model Info:\n${modelInfo.getDisplayInfo()}")
                    }

                    return@withContext true
                } else {
                    Log.e("CustomGemma3n", "❌ FAILED to load sister's trained model")
                    Log.e("CustomGemma3n", "Check the model file and implementation")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e("CustomGemma3n", "❌ CRITICAL ERROR initializing model: ${e.message}")
                Log.e("CustomGemma3n", "Stack trace: ${e.stackTrace.joinToString("\n")}")
                return@withContext false
            }
        }
    }

    private suspend fun copyModelFromAssets(): File? {
        return withContext(Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val modelFile = File(context.filesDir, "sister_dream_assistant.gguf")

                if (modelFile.exists()) {
                    Log.d("CustomGemma3n", "📂 Sister's model already exists: ${modelFile.length()} bytes")
                    return@withContext modelFile
                }

                Log.d("CustomGemma3n", "📋 Copying sister's trained model from assets...")

                // Try to copy the model file
                val inputStream = assetManager.open(modelPath)
                val outputStream = FileOutputStream(modelFile)

                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                Log.d("CustomGemma3n", "✅ Sister's model copied successfully: ${modelFile.length()} bytes")
                return@withContext modelFile

            } catch (e: Exception) {
                Log.e("CustomGemma3n", "❌ CRITICAL: Could not copy sister's model: ${e.message}")
                Log.e("CustomGemma3n", "Make sure the GGUF file from Colab is in assets/models/")
                return@withContext null
            }
        }
    }

    suspend fun sendMessage(message: String): Flow<ChatResult> = flow {
        emit(ChatResult.Loading)

        if (!isInitialized) {
            val errorMsg = "❌ Sister's trained model is not ready yet. Please wait for initialization."
            Log.e("CustomGemma3n", errorMsg)
            emit(ChatResult.Error(errorMsg, isModelError = true))
            return@flow
        }

        try {
            Log.d("CustomGemma3n", "🗣️ Sister says: '$message'")

            // Step 1: Preprocess her speech patterns
            val processedMessage = preprocessSpeechInput(message)
            Log.d("CustomGemma3n", "🔄 Processed: '$processedMessage'")

            // Step 2: Detect actions first
            val detectedAction = detectAction(processedMessage)
            if (detectedAction != null) {
                Log.d("CustomGemma3n", "🎯 Action detected: $detectedAction")
            }

            // Step 3: Generate response using ONLY her trained model
            val response = withContext(Dispatchers.IO) {
                generateWithRealModel(processedMessage, detectedAction)
            }

            Log.d("CustomGemma3n", "🤖 Sister's model response: '$response'")

            emit(ChatResult.Success(
                response = response,
                action = detectedAction,
                shouldSpeak = true,
                isFromRealModel = true
            ))

        } catch (e: Exception) {
            Log.e("CustomGemma3n", "❌ ERROR processing message: ${e.message}")
            Log.e("CustomGemma3n", "Stack trace: ${e.stackTrace.joinToString("\n")}")

            val errorResponse = when {
                e.message?.contains("not initialized") == true -> {
                    "Lo siento, mi modelo personalizado no está listo aún. Por favor espera un momento."
                }
                e.message?.contains("empty response") == true -> {
                    "Hmm, tuve un problemita generando una respuesta. ¿Puedes intentar de nuevo?"
                }
                else -> {
                    "Hubo un error técnico con mi modelo entrenado. ¿Puedes repetir lo que dijiste?"
                }
            }

            emit(ChatResult.Error(errorResponse, isModelError = true))
        }
    }

    /**
     * Generate response using ONLY the real trained model - NO FALLBACKS
     */
    private suspend fun generateWithRealModel(message: String, action: AssistantAction?): String {
        return withContext(Dispatchers.IO) {
            val loader = modelLoader ?: throw IllegalStateException("Model loader not initialized")

            if (!loader.isModelLoaded()) {
                throw IllegalStateException("Sister's trained model is not loaded")
            }

            try {
                Log.d("CustomGemma3n", "🧠 Using sister's REAL trained model for: '${message.take(30)}...'")

                // Build the prompt exactly like your Colab training
                val prompt = buildPersonalizedPrompt(message, action)

                // Generate using the real model
                val rawResponse = loader.generateResponse(prompt)

                // Clean and validate response
                val cleanResponse = cleanModelResponse(rawResponse, message)

                if (cleanResponse.isEmpty()) {
                    throw IllegalStateException("Sister's model returned empty response for: '$message'")
                }

                Log.d("CustomGemma3n", "✅ Real model generated: '${cleanResponse.take(50)}...'")
                return@withContext cleanResponse

            } catch (e: Exception) {
                Log.e("CustomGemma3n", "❌ REAL MODEL ERROR: ${e.message}")
                throw IllegalStateException("Sister's trained model failed: ${e.message}")
            }
        }
    }

    private fun buildPersonalizedPrompt(message: String, action: AssistantAction?): String {
        val systemPrompt = """
            Eres el Dream Assistant de una joven emprendedora increíble con dificultades del habla.
            - Eres su mejor amiga, siempre positiva y motivadora
            - Entiendes sus patrones únicos de voz (entrenado con sus 202 muestras de audio)
            - Responde máximo 2 oraciones para Text-to-Speech
            - Ella quiere crear una plataforma para personas con discapacidades
            - Ayúdala con WhatsApp, YouTube, reuniones, y apoyo emocional
            - Usa lenguaje de mejor amiga, cariñoso pero no romántico
            - Siempre sé encouraging y supportive
        """.trimIndent()

        val actionContext = action?.let {
            when (it) {
                is AssistantAction.SendWhatsApp -> "\nAcción detectada: Enviar mensaje de WhatsApp a ${it.contact}"
                is AssistantAction.OpenYouTube -> "\nAcción detectada: Abrir YouTube"
                is AssistantAction.CreateMeeting -> "\nAcción detectada: Crear reunión '${it.title}'"
                is AssistantAction.RecordVideo -> "\nAcción detectada: Grabar video"
                is AssistantAction.OpenCamera -> "\nAcción detectada: Abrir cámara"
                else -> "\nAcción detectada: ${it.javaClass.simpleName}"
            }
        } ?: ""

        return """$systemPrompt$actionContext

<start_of_turn>user
$message<end_of_turn>
<start_of_turn>model
"""
    }

    private fun cleanModelResponse(rawResponse: String, originalMessage: String): String {
        return rawResponse
            .replace("<start_of_turn>model", "")
            .replace("<end_of_turn>", "")
            .replace("<start_of_turn>user", "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(300) // Reasonable limit for TTS
            .let { response ->
                // Validate response quality
                when {
                    response.isEmpty() -> ""
                    response.length < 3 -> ""
                    response.contains("Mock response") -> ""
                    response.contains("ERROR") -> ""
                    response.contains("undefined") -> ""
                    else -> response
                }
            }
    }

    private fun detectAction(message: String): AssistantAction? {
        val lowerMessage = message.lowercase().trim()

        return when {
            // WhatsApp messaging
            (lowerMessage.contains("envía") || lowerMessage.contains("manda")) &&
                    (lowerMessage.contains("mensaje") || lowerMessage.contains("whatsapp")) -> {
                parseWhatsAppCommand(lowerMessage)
            }

            // Read messages
            lowerMessage.contains("lee") && lowerMessage.contains("mensaje") -> {
                AssistantAction.ReadWhatsApp()
            }

            lowerMessage.contains("qué mensajes") || lowerMessage.contains("mensajes nuevos") -> {
                AssistantAction.ReadWhatsApp()
            }

            lowerMessage.contains("lee esto") || lowerMessage.contains("qué dice") -> {
                AssistantAction.ReadScreenContent("Contenido de pantalla")
            }

            // YouTube
            lowerMessage.contains("abre youtube") -> {
                AssistantAction.OpenYouTube()
            }

            lowerMessage.contains("busca en youtube") -> {
                val searchQuery = extractSearchQuery(lowerMessage)
                AssistantAction.OpenYouTube(searchQuery)
            }

            // Video recording
            (lowerMessage.contains("grabar") && lowerMessage.contains("video")) ||
                    lowerMessage.contains("vamos a grabar") -> {
                val title = extractVideoTitle(lowerMessage)
                AssistantAction.RecordVideo(title)
            }

            // Camera
            lowerMessage.contains("abre") && lowerMessage.contains("cámara") -> {
                AssistantAction.OpenCamera
            }

            // Meetings
            lowerMessage.contains("crea") && (lowerMessage.contains("reunión") || lowerMessage.contains("meeting")) -> {
                val title = extractMeetingTitle(lowerMessage)
                AssistantAction.CreateMeeting(title)
            }

            else -> null
        }
    }

    private fun parseWhatsAppCommand(message: String): AssistantAction.SendWhatsApp? {
        val patterns = listOf(
            Regex("(?:envía|manda)\\s+mensaje\\s+a\\s+([\\w\\s]+?)\\s+(?:que\\s+diga|diciendo)?\\s*(.+)", RegexOption.IGNORE_CASE),
            Regex("(?:envía|manda)\\s+a\\s+([\\w\\s]+?)\\s+(.+)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null && match.groupValues.size >= 3) {
                val contact = match.groupValues[1].trim()
                val messageText = match.groupValues[2].trim()
                if (contact.isNotBlank() && messageText.isNotBlank()) {
                    return AssistantAction.SendWhatsApp(contact, messageText)
                }
            }
        }
        return null
    }

    private fun extractSearchQuery(message: String): String? {
        val pattern = Regex("busca\\s+en\\s+youtube\\s+(.+)", RegexOption.IGNORE_CASE)
        return pattern.find(message)?.groupValues?.get(1)?.trim()
    }

    private fun extractVideoTitle(message: String): String? {
        val pattern = Regex("grabar\\s+(?:video\\s+)?(?:sobre\\s+|de\\s+)?(.+)", RegexOption.IGNORE_CASE)
        return pattern.find(message)?.groupValues?.get(1)?.trim()
    }

    private fun extractMeetingTitle(message: String): String {
        val pattern = Regex("(?:reunión|meeting)\\s+(?:sobre\\s+|de\\s+|para\\s+)?(.+)", RegexOption.IGNORE_CASE)
        return pattern.find(message)?.groupValues?.get(1)?.trim() ?: "Reunión de Comunidad Virtual"
    }

    /**
     * Enhanced speech preprocessing specifically for your sister's patterns
     * Based on the 202 voice samples you used for training
     */
    fun preprocessSpeechInput(rawSpeech: String): String {
        return rawSpeech.trim()
            .replace(Regex("\\s+"), " ")
            .let { text ->
                // Speech pattern corrections based on her training data
                text.replace("ola", "hola")
                    .replace("k tal", "qué tal")
                    .replace("q", "que")
                    .replace("x", "por")
                    .replace("tb", "también")
                    .replace("wa", "whatsapp")
                    .replace("watsap", "whatsapp")
                    .replace("utube", "youtube")
                    .replace("camara", "cámara")
                    .replace("reunin", "reunión")
                    .replace("negosio", "negocio")
                    .replace("plataforma", "plataforma")
            }
    }

    /**
     * Get status of the real model
     */
    fun getModelStatus(): ChatResult.ModelStatus {
        val modelInfo = modelLoader?.getModelInfo()
        return ChatResult.ModelStatus(
            isRealModel = isInitialized && modelLoader?.isModelLoaded() == true,
            message = if (isInitialized) {
                "✅ Sister's trained model is active and ready"
            } else {
                "❌ Sister's trained model is not loaded"
            },
            modelInfo = modelInfo?.getDisplayInfo()
        )
    }

    fun destroy() {
        try {
            modelLoader?.cleanup()
            modelLoader = null
            isInitialized = false
            Log.d("CustomGemma3n", "🔇 Sister's Dream Assistant disconnected")
        } catch (e: Exception) {
            Log.e("CustomGemma3n", "Error during cleanup: ${e.message}")
        }
    }
}