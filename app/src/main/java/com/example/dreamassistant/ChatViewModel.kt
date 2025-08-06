package com.example.dreamassistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreamassistant.ai.LlamaEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var llamaEngine: LlamaEngine? = null
        private set

    var isModelReady: Boolean = false
        private set

    init {
        Log.d(TAG, "🚀 ChatViewModel initialized for Sister's Dream Assistant")
        addMessage(ChatMessage.createWelcomeMessage())
    }

    /**
     * Set the real LlamaEngine when it's ready
     */
    fun setLlamaEngine(engine: LlamaEngine) {
        llamaEngine = engine
        isModelReady = engine.isModelReady()
        Log.d(TAG, "✅ Real LlamaEngine set - Model ready: $isModelReady")

        if (isModelReady) {
            val readyMessage = ChatMessage.createModelResponse(
                text = "🎉 ¡Tu modelo personalizado está listo! Ahora entiendo perfectamente tu voz y patrones únicos. Estoy aquí para ayudarte con todo lo que necesites para tu plataforma y tus sueños empresariales! 🚀💕",
                isFromRealModel = true,
                inferenceTime = 0,
                supportLevel = ChatMessage.SupportLevel.CELEBRATORY,
                isBusinessAdvice = true
            )
            addMessage(readyMessage)

            val modelInfo = engine.getModelInfo()
            Log.d(TAG, "📊 Sister's Model Info: $modelInfo")
        } else {
            val loadingMessage = ChatMessage.createStatusMessage(
                message = "⏳ Cargando tu modelo entrenado con tus 202+ muestras de voz...",
                isModelReady = false
            )
            addMessage(loadingMessage)
        }
    }

    /**
     * Update the current input text (unused in voice-only setup)
     */
    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(currentInput = text)
    }

    /**
     * Add a ChatMessage to the UI state
     */
    fun addMessage(message: ChatMessage) {
        Log.d(TAG, "💬 Adding message: ${message.text.take(30)}")
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message
        )
    }

    /**
     * Drive the assistant by raw text (from voice recognition)
     */
    fun sendMessageWithText(text: String) {
        _uiState.value = _uiState.value.copy(currentInput = text)
        sendMessage()
    }

    /**
     * Send message using Sister's REAL trained model
     */
    fun sendMessage() {
        val currentInput = _uiState.value.currentInput.trim()
        if (currentInput.isEmpty()) {
            Log.w(TAG, "⚠️ Empty message, ignoring")
            return
        }

        Log.d(TAG, "👤 Sister's message: '$currentInput'")
        addMessage(ChatMessage.createTextMessage(currentInput))
        _uiState.value = _uiState.value.copy(
            currentInput = "",
            isLoading = true
        )

        viewModelScope.launch {
            try {
                val engine = llamaEngine
                if (engine == null || !isModelReady) {
                    handleError("El motor de IA no está disponible aún. Por favor espera.", true)
                    return@launch
                }

                val result = engine.generateResponseWithTiming(currentInput)
                if (result.isSuccess) {
                    val (response, inferenceTime) = result.getOrThrow()
                    val assistantMsg = ChatMessage.createModelResponse(
                        text = response,
                        isFromRealModel = true,
                        inferenceTime = (inferenceTime * 1000).toLong(),
                        supportLevel = ChatMessage.SupportLevel.ENCOURAGING
                    )
                    addMessage(assistantMsg)
                } else {
                    handleError("Hubo un problema: ${result.exceptionOrNull()?.message}", true)
                }
            } catch (e: Exception) {
                handleError("Error inesperado: ${e.message}", true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Handle errors uniformly
     */
    private fun handleError(message: String, isModelError: Boolean) {
        Log.e(TAG, "🚨 Handling error: $message")
        addMessage(ChatMessage.createErrorResponse(message, isModelError))
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Clear messages (testing)
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
        addMessage(ChatMessage.createWelcomeMessage())
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🔇 ChatViewModel cleared")
    }
}

/**
 * UI State for the chat screen
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false
)
