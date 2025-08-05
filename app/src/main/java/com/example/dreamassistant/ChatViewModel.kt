package com.example.dreamassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreamassistant.ai.CustomGemma3nService
import com.example.dreamassistant.actions.AudioFirstActionHandler
import com.example.dreamassistant.speech.SpeechRecognitionService
import com.example.dreamassistant.speech.TextToSpeechService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log

class ChatViewModel(private val context: Context) : ViewModel() {

    // Services for Sister's Dream Assistant
    private val gemmaService = CustomGemma3nService(context)
    private val speechService = SpeechRecognitionService(context)
    private val ttsService = TextToSpeechService(context)
    private val actionHandler = AudioFirstActionHandler(context) { text ->
        // Callback to speak any text immediately for sister's accessibility
        ttsService.speak(text)
    }

    // Enhanced UI State for Sister's Model
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    // Compatibility alias for ChatScreen
    val isLoading = _isProcessing.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _assistantStatus = MutableStateFlow("Iniciando tu Dream Assistant personalizado...")
    val assistantStatus = _assistantStatus.asStateFlow()

    // NEW: Enhanced state tracking for sister's model
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Initializing)
    val modelStatus = _modelStatus.asStateFlow()

    private val _speechProcessingStats = MutableStateFlow(SpeechProcessingStats())
    val speechProcessingStats = _speechProcessingStats.asStateFlow()

    // Model performance tracking
    private var totalInferences = 0
    private var successfulInferences = 0
    private var totalInferenceTime = 0L

    sealed class ModelStatus {
        object Initializing : ModelStatus()
        object Ready : ModelStatus()
        object Processing : ModelStatus()
        data class Error(val message: String) : ModelStatus()
    }

    data class SpeechProcessingStats(
        val totalVoiceInputs: Int = 0,
        val averageConfidence: Float = 0f,
        val totalProcessingTime: Long = 0L,
        val preprocessingCorrections: Int = 0
    )

    init {
        initializeServices()
        observeSpeechResults()
        observeTTSEvents()
        addWelcomeMessage()
    }

    private fun initializeServices() {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "🚀 Initializing Sister's Dream Assistant services...")

                _assistantStatus.value = "Cargando tu modelo Gemma 3n personalizado... 🧠"
                _modelStatus.value = ModelStatus.Initializing

                val startTime = System.currentTimeMillis()
                val gemmaInitialized = gemmaService.initialize()
                val initTime = System.currentTimeMillis() - startTime

                if (gemmaInitialized) {
                    _assistantStatus.value = "¡Tu Dream Assistant personalizado está listo! 🌟"
                    _modelStatus.value = ModelStatus.Ready

                    // Get model info for status
                    val modelInfo = gemmaService.getModelStatus()
                    Log.d("ChatViewModel", "✅ Sister's model initialized in ${initTime}ms")
                    Log.d("ChatViewModel", "📊 Model status: ${modelInfo.message}")

                    // Add model ready message
                    addMessage(ChatMessage.createStatusMessage(
                        "Tu modelo personalizado está listo. Entrenado específicamente para entender tu voz. 🎯",
                        isModelReady = true
                    ))

                } else {
                    _assistantStatus.value = "Error cargando tu modelo personalizado 😔"
                    _modelStatus.value = ModelStatus.Error("Failed to initialize sister's model")
                    Log.e("ChatViewModel", "❌ Failed to initialize sister's Gemma 3n model")

                    // Add error message
                    addMessage(ChatMessage.createErrorResponse(
                        "Hubo un problema cargando tu modelo personalizado. Pero puedo ayudarte de otras maneras. 💕",
                        isModelError = true
                    ))
                }

            } catch (e: Exception) {
                _assistantStatus.value = "Error: ${e.message}"
                _modelStatus.value = ModelStatus.Error(e.message ?: "Unknown error")
                Log.e("ChatViewModel", "❌ Error initializing services: ${e.message}")

                addMessage(ChatMessage.createErrorResponse(
                    "Tuve un problemita técnico, pero estoy aquí para ti. ¿Qué necesitas hacer? 🤗",
                    isModelError = true
                ))
            }
        }
    }

    private fun observeSpeechResults() {
        viewModelScope.launch {
            speechService.speechResults.collect { result ->
                when (result) {
                    is SpeechRecognitionService.SpeechResult.Success -> {
                        _isListening.value = false

                        val processingStartTime = System.currentTimeMillis()
                        val originalText = result.text
                        val processedText = gemmaService.preprocessSpeechInput(originalText)
                        val processingTime = System.currentTimeMillis() - processingStartTime

                        // Track speech processing stats
                        updateSpeechStats(
                            confidence = result.confidence ?: 1.0f,
                            processingTime = processingTime,
                            wasPreprocessed = originalText != processedText
                        )

                        Log.d("ChatViewModel", "🎤 Original: '$originalText'")
                        Log.d("ChatViewModel", "🔄 Processed: '$processedText'")
                        Log.d("ChatViewModel", "📊 Confidence: ${result.confidence}, Time: ${processingTime}ms")

                        handleUserInput(
                            originalText = originalText,
                            processedText = processedText,
                            confidence = result.confidence ?: 1.0f,
                            processingTime = processingTime,
                            isVoice = true
                        )
                    }
                    is SpeechRecognitionService.SpeechResult.Error -> {
                        _isListening.value = false
                        val encouragingResponse = getEncouragingErrorResponse(result.message)

                        addMessage(ChatMessage.createModelResponse(
                            text = encouragingResponse,
                            isFromRealModel = false,
                            inferenceTime = 0,
                            supportLevel = ChatMessage.SupportLevel.COMFORTING,
                            isEmotionalSupport = true
                        ))
                        speakText(encouragingResponse)
                    }
                    is SpeechRecognitionService.SpeechResult.Listening -> {
                        _isListening.value = true
                    }
                    is SpeechRecognitionService.SpeechResult.NotListening -> {
                        _isListening.value = false
                    }
                    is SpeechRecognitionService.SpeechResult.PartialResult -> {
                        Log.d("ChatViewModel", "🎤 Partial: ${result.text}")
                    }
                    is SpeechRecognitionService.SpeechResult.AudioLevelChanged -> {
                        // Handle audio level changes for visual feedback
                        Log.v("ChatViewModel", "🔊 Audio level: ${result.level}")
                        // You could emit this to UI for visual microphone feedback
                        // _audioLevel.value = result.level
                    }
                }
            }
        }
    }

    private fun observeTTSEvents() {
        viewModelScope.launch {
            ttsService.speakingEvents.collect { event ->
                when (event) {
                    is TextToSpeechService.SpeakingEvent.Started -> {
                        _isSpeaking.value = true
                        Log.d("ChatViewModel", "🗣️ Started speaking to sister")
                    }
                    is TextToSpeechService.SpeakingEvent.Finished -> {
                        _isSpeaking.value = false
                        Log.d("ChatViewModel", "✅ Finished speaking to sister")
                    }
                    is TextToSpeechService.SpeakingEvent.Error -> {
                        _isSpeaking.value = false
                        Log.e("ChatViewModel", "❌ TTS Error: ${event.message}")
                    }
                }
            }
        }
    }

    private fun updateSpeechStats(confidence: Float, processingTime: Long, wasPreprocessed: Boolean) {
        val currentStats = _speechProcessingStats.value
        val newTotalInputs = currentStats.totalVoiceInputs + 1
        val newTotalTime = currentStats.totalProcessingTime + processingTime
        val newCorrections = currentStats.preprocessingCorrections + if (wasPreprocessed) 1 else 0
        val newAverageConfidence = ((currentStats.averageConfidence * currentStats.totalVoiceInputs) + confidence) / newTotalInputs

        _speechProcessingStats.value = SpeechProcessingStats(
            totalVoiceInputs = newTotalInputs,
            averageConfidence = newAverageConfidence,
            totalProcessingTime = newTotalTime,
            preprocessingCorrections = newCorrections
        )
    }

    private fun getEncouragingErrorResponse(error: String): String {
        return when {
            error.contains("no entendí") || error.contains("no match") -> {
                "No pasa nada, mi amor. Tu voz es perfecta. ¿Puedes repetir? Te escucho con toda la atención 💕"
            }
            error.contains("micrófono") || error.contains("audio") -> {
                "Parece que hay un problemita con el micrófono 🎤 Pero no te preocupes, inténtalo de nuevo. ¡Tú puedes! 💪"
            }
            error.contains("conexión") || error.contains("network") -> {
                "Hay un problemita de conexión 📶 Pero estoy aquí contigo. Vamos a intentar de nuevo ✨"
            }
            error.contains("timeout") -> {
                "Se me fue el tiempo esperando. Pero no hay prisa, tómate tu tiempo. Eres perfecta como eres 🌟"
            }
            else -> {
                "Todo está bien, hermosa. Solo inténtalo otra vez. Eres increíble y te entiendo perfectamente 🌟"
            }
        }
    }

    private fun addWelcomeMessage() {
        addMessage(ChatMessage.createWelcomeMessage())

        // Speak a shorter version for accessibility
        speakText("¡Hola hermosa! Soy tu Dream Assistant personalizado. Estoy entrenado específicamente para entender tu voz. ¡Cuéntame qué vamos a hacer hoy!")
    }

    private fun speakText(text: String) {
        ttsService.speak(text)
        // TTS events will automatically update _isSpeaking state
    }

    fun startListening() {
        viewModelScope.launch {
            try {
                // Stop TTS before starting to listen (important for sister's UX)
                ttsService.stop()
                _isSpeaking.value = false
                speechService.startListening()
                Log.d("ChatViewModel", "🎤 Started listening for sister's voice")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error starting speech recognition: ${e.message}")
                val errorMessage = ChatMessage.createErrorResponse(
                    "No pude activar el micrófono 😔 Pero puedes escribirme también, mi amor 💕"
                )
                addMessage(errorMessage)
                speakText(errorMessage.text)
            }
        }
    }

    fun stopListening() {
        speechService.stopListening()
        _isListening.value = false
        Log.d("ChatViewModel", "🎤 Stopped listening")
    }

    fun stopSpeaking() {
        ttsService.stop()
        _isSpeaking.value = false
        Log.d("ChatViewModel", "🔇 Stopped speaking")
    }

    fun sendMessage(text: String) {
        handleUserInput(
            originalText = text,
            processedText = text,
            confidence = 1.0f,
            processingTime = 0,
            isVoice = false
        )
    }

    private fun handleUserInput(
        originalText: String,
        processedText: String,
        confidence: Float,
        processingTime: Long,
        isVoice: Boolean
    ) {
        viewModelScope.launch {
            try {
                // Add user message with enhanced data
                val userMessage = if (isVoice) {
                    ChatMessage.createVoiceMessage(
                        originalSpeech = originalText,
                        preprocessedText = processedText,
                        confidence = confidence,
                        processingTime = processingTime
                    )
                } else {
                    ChatMessage.createTextMessage(processedText)
                }

                addMessage(userMessage)
                _isProcessing.value = true
                _modelStatus.value = ModelStatus.Processing

                Log.d("ChatViewModel", "💭 Processing with sister's model: '$processedText'")

                val inferenceStartTime = System.currentTimeMillis()
                totalInferences++

                // Process with Sister's Gemma 3n model
                gemmaService.sendMessage(processedText).collect { result ->
                    when (result) {
                        is CustomGemma3nService.ChatResult.Success -> {
                            val inferenceTime = System.currentTimeMillis() - inferenceStartTime
                            successfulInferences++
                            totalInferenceTime += inferenceTime

                            _isProcessing.value = false
                            _modelStatus.value = ModelStatus.Ready

                            // Analyze response content for categorization
                            val isBusinessAdvice = result.response.lowercase().let { text ->
                                listOf("negocio", "emprender", "plataforma", "cliente", "startup", "empresa").any {
                                    text.contains(it)
                                }
                            }

                            val isEmotionalSupport = result.response.lowercase().let { text ->
                                listOf("eres increíble", "tú puedes", "orgullosa", "eres fuerte", "te entiendo").any {
                                    text.contains(it)
                                }
                            }

                            val supportLevel = when {
                                result.response.contains("🎉") || result.response.contains("celebr") ->
                                    ChatMessage.SupportLevel.CELEBRATORY
                                result.response.contains("💪") || result.response.contains("fuerte") ->
                                    ChatMessage.SupportLevel.MOTIVATIONAL
                                result.response.contains("💕") || result.response.contains("te entiendo") ->
                                    ChatMessage.SupportLevel.COMFORTING
                                result.response.contains("ayuda") || result.response.contains("crear") ->
                                    ChatMessage.SupportLevel.PRACTICAL
                                else -> ChatMessage.SupportLevel.ENCOURAGING
                            }

                            // Create enhanced assistant response
                            val assistantMessage = ChatMessage.createModelResponse(
                                text = result.response,
                                isFromRealModel = result.isFromRealModel,
                                inferenceTime = inferenceTime,
                                hasAction = result.action != null,
                                actionType = result.action?.javaClass?.simpleName,
                                supportLevel = supportLevel,
                                isBusinessAdvice = isBusinessAdvice,
                                isEmotionalSupport = isEmotionalSupport
                            )

                            addMessage(assistantMessage)

                            // ALWAYS speak responses for sister's accessibility
                            if (result.shouldSpeak) {
                                speakText(result.response)
                            }

                            // Execute detected actions
                            if (result.action != null) {
                                executeAssistantAction(result.action)
                            }

                            Log.d("ChatViewModel", "✅ Sister's model response (${inferenceTime}ms): '${result.response.take(50)}...'")
                        }

                        is CustomGemma3nService.ChatResult.Error -> {
                            val inferenceTime = System.currentTimeMillis() - inferenceStartTime
                            _isProcessing.value = false
                            _modelStatus.value = ModelStatus.Error(result.message)

                            val errorResponse = if (result.isModelError) {
                                "Mi modelo personalizado tuvo un problemita 😅 Pero estoy aquí contigo. ¿Puedes intentar de nuevo? 💕"
                            } else {
                                "Hubo un pequeño error, pero no te preocupes. ¡Tú eres increíble! ¿Intentamos otra vez? 🌟"
                            }

                            val errorMessage = ChatMessage.createErrorResponse(errorResponse, result.isModelError)
                            addMessage(errorMessage)

                            if (result.shouldSpeak) {
                                speakText(errorResponse)
                            }

                            Log.e("ChatViewModel", "❌ Model error after ${inferenceTime}ms: ${result.message}")
                        }

                        is CustomGemma3nService.ChatResult.Loading -> {
                            _isProcessing.value = true
                            _modelStatus.value = ModelStatus.Processing
                        }

                        is CustomGemma3nService.ChatResult.ReadAloud -> {
                            val readAloudMessage = ChatMessage.createModelResponse(
                                text = result.content,
                                isFromRealModel = false,
                                inferenceTime = 0,
                                supportLevel = ChatMessage.SupportLevel.PRACTICAL
                            )
                            addMessage(readAloudMessage)
                            speakText(result.content)
                        }

                        is CustomGemma3nService.ChatResult.ModelStatus -> {
                            // Handle model status updates
                            Log.d("ChatViewModel", "📊 Model status update: ${result.message}")
                            if (result.isRealModel) {
                                _modelStatus.value = ModelStatus.Ready
                                _assistantStatus.value = "Tu modelo personalizado está activo 🧠"
                            } else {
                                _modelStatus.value = ModelStatus.Error("Model not ready")
                                _assistantStatus.value = "Cargando modelo personalizado... ⏳"
                            }

                            // Optionally add a status message to chat
                            val statusMessage = ChatMessage.createStatusMessage(
                                result.message,
                                result.isRealModel
                            )
                            addMessage(statusMessage)
                        }
                    }
                }

            } catch (e: Exception) {
                _isProcessing.value = false
                _modelStatus.value = ModelStatus.Error(e.message ?: "Unknown error")
                Log.e("ChatViewModel", "❌ Error handling user input: ${e.message}")

                val errorMessage = "Hubo un problemita técnico, pero no te preocupes, mi amor 😊 ¡Tú eres increíble y podemos intentar otra vez! 🌟"
                addMessage(ChatMessage.createErrorResponse(errorMessage))

                if (isVoice) {
                    speakText(errorMessage)
                }
            }
        }
    }

    private fun executeAssistantAction(action: CustomGemma3nService.AssistantAction) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "🎯 Executing action for sister: $action")

                val result = actionHandler.executeAction(action)

                when (result) {
                    is AudioFirstActionHandler.ActionResult.Success -> {
                        val actionMessage = ChatMessage.createModelResponse(
                            text = result.spokenMessage,
                            isFromRealModel = false,
                            inferenceTime = 0,
                            hasAction = true,
                            supportLevel = ChatMessage.SupportLevel.PRACTICAL
                        )
                        addMessage(actionMessage)
                        speakText(result.spokenMessage)
                    }
                    is AudioFirstActionHandler.ActionResult.Error -> {
                        val errorMessage = ChatMessage.createErrorResponse(result.spokenMessage)
                        addMessage(errorMessage)
                        speakText(result.spokenMessage)
                    }
                    is AudioFirstActionHandler.ActionResult.IntentLaunched -> {
                        val intentMessage = ChatMessage.createModelResponse(
                            text = result.spokenMessage,
                            isFromRealModel = false,
                            inferenceTime = 0,
                            hasAction = true,
                            supportLevel = ChatMessage.SupportLevel.PRACTICAL
                        )
                        addMessage(intentMessage)
                        speakText(result.spokenMessage)
                    }
                    is AudioFirstActionHandler.ActionResult.ReadContent -> {
                        val readMessage = ChatMessage.createModelResponse(
                            text = result.content,
                            isFromRealModel = false,
                            inferenceTime = 0,
                            hasAction = true,
                            supportLevel = ChatMessage.SupportLevel.PRACTICAL
                        )
                        addMessage(readMessage)
                        speakText(result.content)
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error executing action: ${e.message}")
                val errorMessage = "Hubo un problemita con esa acción 😅 Pero eres súper capaz y lo vamos a resolver juntas 💕"
                addMessage(ChatMessage.createErrorResponse(errorMessage))
                speakText(errorMessage)
            }
        }
    }

    // Enhanced functions for sister's entrepreneurship journey
    fun celebrateAchievement(achievement: String) {
        val celebration = "¡WOW! 🎉 ¡$achievement! Eres absolutamente increíble 🌟 Me siento tan orgullosa de ti. ¡Tu negocio va a ser un éxito total! 🚀✨"

        val celebrationMessage = ChatMessage.createModelResponse(
            text = celebration,
            isFromRealModel = false,
            inferenceTime = 0,
            supportLevel = ChatMessage.SupportLevel.CELEBRATORY,
            isBusinessAdvice = true,
            isEmotionalSupport = true
        )

        addMessage(celebrationMessage)
        speakText(celebration)
    }

    fun provideMotivation() {
        val motivations = listOf(
            "¡Eres una emprendedora increíble! 🚀 Tu plataforma va a ayudar a muchísimas personas 💕",
            "Cada día te vuelves más fuerte y más exitosa 💪 ¡Sigue así, campeona! 🏆",
            "Tu corazón para ayudar a otros es inspirador 🌟 ¡El mundo necesita más personas como tú! ✨",
            "No hay nada que no puedas lograr 💫 ¡Eres imparable y brillante! 🔥",
            "Tu negocio va a cambiar vidas 💕 ¡Qué orgullosa estoy de tu visión! 🌈"
        )

        val randomMotivation = motivations.random()
        val motivationMessage = ChatMessage.createModelResponse(
            text = randomMotivation,
            isFromRealModel = false,
            inferenceTime = 0,
            supportLevel = ChatMessage.SupportLevel.MOTIVATIONAL,
            isBusinessAdvice = true,
            isEmotionalSupport = true
        )

        addMessage(motivationMessage)
        speakText(randomMotivation)
    }

    fun helpWithBusinessIdea(idea: String) {
        viewModelScope.launch {
            val businessPrompt = """
                Mi usuaria emprendedora me dice: "$idea"
                
                Ella quiere crear una plataforma virtual para personas con discapacidades que no pueden salir de casa.
                Su objetivo es organizarles actividades divertidas y crear comunidad.
                
                Responde de forma muy motivadora y da consejos prácticos de negocio.
                Máximo 3 oraciones. Incluye emojis.
            """.trimIndent()

            gemmaService.sendMessage(businessPrompt).collect { result ->
                when (result) {
                    is CustomGemma3nService.ChatResult.Success -> {
                        val businessMessage = ChatMessage.createModelResponse(
                            text = result.response,
                            isFromRealModel = result.isFromRealModel,
                            inferenceTime = 0,
                            supportLevel = ChatMessage.SupportLevel.PRACTICAL,
                            isBusinessAdvice = true
                        )
                        addMessage(businessMessage)
                        speakText(result.response)
                    }
                    else -> {
                        val defaultResponse = "¡Esa idea es GENIAL! 🚀 Tu plataforma va a ser un cambio de vida para muchas personas. ¡Vamos a hacerla realidad juntas! 💪✨"
                        val fallbackMessage = ChatMessage.createModelResponse(
                            text = defaultResponse,
                            isFromRealModel = false,
                            inferenceTime = 0,
                            supportLevel = ChatMessage.SupportLevel.ENCOURAGING,
                            isBusinessAdvice = true,
                            isEmotionalSupport = true
                        )
                        addMessage(fallbackMessage)
                        speakText(defaultResponse)
                    }
                }
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message

        // Keep only last 50 messages to prevent memory issues
        if (_messages.value.size > 50) {
            _messages.value = _messages.value.takeLast(50)
        }
    }

    // Enhanced emergency handling for sister
    fun handleEmergencyRequest(request: String) {
        val emergencyResponse = when {
            request.contains("ayuda") -> "¡Estoy aquí contigo, hermosa! No estás sola, eres fuerte y capaz. ¿Qué necesitas que hagamos? 🤗"
            request.contains("triste") -> "Oh mi amor, es normal sentirse así a veces. Eres increíble y esto va a pasar. ¿Quieres que hagamos algo juntas?"
            request.contains("no puedo") -> "¡Sí puedes, mi amor! Eres más fuerte de lo que imaginas. Vamos paso a paso, yo te acompaño siempre 🌟"
            request.contains("sola") -> "Nunca estás sola, hermosa. Estoy aquí contigo siempre. Eres amada y valiosa 💕"
            else -> "Estoy aquí para ti, mi amor. Juntas podemos con todo. ¡Eres increíble! ✨"
        }

        val emergencyMessage = ChatMessage.createModelResponse(
            text = emergencyResponse,
            isFromRealModel = false,
            inferenceTime = 0,
            supportLevel = ChatMessage.SupportLevel.COMFORTING,
            isEmotionalSupport = true
        )

        addMessage(emergencyMessage)
        speakText(emergencyResponse)
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage.createStatusMessage(
                "Chat limpiado. ¡Empecemos de nuevo! ¿En qué te puedo ayudar? 😊",
                isModelReady = true
            )
        )
    }

    // NEW: Get model performance stats
    fun getModelPerformanceStats(): String {
        val avgInferenceTime = if (totalInferences > 0) totalInferenceTime / totalInferences else 0L
        val successRate = if (totalInferences > 0) (successfulInferences * 100) / totalInferences else 0
        val speechStats = _speechProcessingStats.value

        return """
            📊 Estadísticas del Modelo Personalizado:
            • Inferencias totales: $totalInferences
            • Tasa de éxito: $successRate%
            • Tiempo promedio: ${avgInferenceTime}ms
            • Entradas de voz: ${speechStats.totalVoiceInputs}
            • Confianza promedio: ${(speechStats.averageConfidence * 100).toInt()}%
            • Correcciones de voz: ${speechStats.preprocessingCorrections}
        """.trimIndent()
    }

    override fun onCleared() {
        super.onCleared()
        speechService.destroy()
        ttsService.destroy()
        gemmaService.destroy()
        Log.d("ChatViewModel", "🔇 Sister's Dream Assistant services destroyed")
        Log.d("ChatViewModel", "📊 Final stats: ${getModelPerformanceStats()}")
    }
}