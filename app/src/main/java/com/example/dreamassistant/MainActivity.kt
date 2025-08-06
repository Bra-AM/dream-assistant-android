package com.example.dreamassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.dreamassistant.ai.LlamaEngine
import com.example.dreamassistant.speech.SpeechRecognitionService
import com.example.dreamassistant.speech.TextToSpeechService
import com.example.dreamassistant.ui.theme.DreamAssistantTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.io.File

/**
 * MainActivity for Sister's Dream Assistant
 * Combines real llama.cpp model initialization with continuous voice loop
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: ChatViewModel by viewModels()

    // Speech & TTS services
    private lateinit var speechService: SpeechRecognitionService
    private lateinit var ttsService: TextToSpeechService

    // Required permissions
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        handlePermissionResults(perms)
    }

    // LlamaEngine & ready flag
    private lateinit var llamaEngine: LlamaEngine
    var isModelReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "🚀 Starting Sister's Dream Assistant")

        // Initialize services
        speechService = SpeechRecognitionService(this)
        ttsService    = TextToSpeechService(this)

        // Kick off model initialization
        initializeLlamaEngine()

        // Ask for permissions
        checkAndRequestPermissions()

        // Compose UI
        setContent {
            DreamAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold { innerPadding ->
                        ChatScreen(
                            llamaEngine  = if (viewModel.isModelReady) viewModel.llamaEngine else null,
                            modifier     = Modifier.padding(innerPadding),
                            onVoiceInput = { speechService.startListening() }
                        )
                    }
                }
            }
        }

        // 1) Listen for sister’s speech
        lifecycleScope.launch {
            speechService.speechResults.collect { result ->
                if (result is SpeechRecognitionService.SpeechResult.Success) {
                    val userMsg = ChatMessage.createVoiceMessage(
                        originalSpeech   = result.text,
                        preprocessedText = result.text,
                        confidence       = result.confidence ?: 1f,
                        processingTime   = result.processingTime
                    )
                    viewModel.addMessage(userMsg)
                    viewModel.sendMessageWithText(result.text)
                }
            }
        }

        // 2) Speak assistant replies
        lifecycleScope.launch {
            snapshotFlow { viewModel.uiState.value.messages.lastOrNull() }
                .filterIsInstance<ChatMessage.Assistant>()
                .collect { assistant ->
                    ttsService.speak(assistant.text)
                }
        }

        // 3) Loop back to listening when done
        lifecycleScope.launch {
            ttsService.events
                .filter { it is TextToSpeechService.TtsEvent.UtteranceDone }
                .collect {
                    speechService.startListening()
                }
        }

        initializeAppLifecycle()
    }

    private fun initializeLlamaEngine() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🧠 Initializing LlamaEngine...")
                llamaEngine = LlamaEngine.getInstance()
                viewModel.setLlamaEngine(llamaEngine)

                if (!llamaEngine.isModelReady()) {
                    Log.d(TAG, "🔄 Loading sister's model asynchronously...")
                    initializeModelAsync()
                } else {
                    isModelReady = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing LlamaEngine: ${e.message}")
                showToast("Error inicializando IA: ${e.message}")
            }
        }
    }

    //change here
    private suspend fun initializeModelAsync() {
        // Common helper to copy an asset to internal storage if missing
        suspend fun copyAssetIfNeeded(assetPath: String, outFile: File) {
            if (!outFile.exists()) {
                assets.open(assetPath).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "✅ Copied asset $assetPath → ${outFile.absolutePath}")
            }
        }

        try {
            showToast("Cargando modelo personalizado… ⏳")
            // 1) Attempt to load the personalized model
            val personalFile = File(filesDir, "sister_dream_assistant.gguf")
            copyAssetIfNeeded("models/sister_dream_assistant.gguf", personalFile)

            var result = llamaEngine.initializeModel(this@MainActivity, personalFile)
            if (!result.isSuccess) {
                // 2) Fallback: load the base Gemma 3n model
                Log.w(TAG, "⚠️ Personal model load failed: ${result.exceptionOrNull()?.message}")
                showToast("Modelo personalizado no disponible, cargando modelo base… ⏳")

                val baseFile = File(filesDir, "gemma3nlu_base.gguf")
                copyAssetIfNeeded("models/gemma3nlu_base.gguf", baseFile)

                result = llamaEngine.initializeModel(this@MainActivity, baseFile)
            }

            if (result.isSuccess) {
                isModelReady = true
                viewModel.setLlamaEngine(llamaEngine)
                showToast("¡Modelo listo! 🌟")
                testModel()
            } else {
                Log.e(TAG, "❌ Both model loads failed: ${result.exceptionOrNull()?.message}")
                showToast("Error cargando cualquier modelo 😔")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during model init: ${e.message}")
            showToast("Error al cargar modelo: ${e.message}")
        }
    }


    private suspend fun testModel() {
        try {
            val testRes = llamaEngine.generateResponse("Hola, ¿cómo estás?")
            if (testRes.isSuccess) {
                showToast("Test OK: ${testRes.getOrNull()?.take(20)}...")
            }
        } catch (_: Exception) {}
    }

    private fun checkAndRequestPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missing.isNotEmpty()) permissionLauncher.launch(missing)
    }

    private fun handlePermissionResults(perms: Map<String, Boolean>) {
        if (perms[Manifest.permission.RECORD_AUDIO] == true) {
            speechService.startListening()
        } else {
            showToast("Permiso de audio requerido")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun initializeAppLifecycle() {
        Log.d(TAG, "📱 App lifecycle initialized")
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsService.shutdown()
        if (::llamaEngine.isInitialized) llamaEngine.cleanup()
    }
}
