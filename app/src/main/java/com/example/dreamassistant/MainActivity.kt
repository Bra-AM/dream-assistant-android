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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.dreamassistant.ui.theme.DreamAssistantTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainActivity for Sister's Dream Assistant
 * Handles permissions and initialization for her personalized Gemma 3n model
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val MICROPHONE_PERMISSION_CODE = 200
    }

    // Enhanced permission handling for sister's accessibility needs
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,           // Essential for voice input
        Manifest.permission.INTERNET,               // For API integrations
        Manifest.permission.READ_CONTACTS,          // For WhatsApp integration
        Manifest.permission.WRITE_CONTACTS,         // For contact management
        Manifest.permission.SEND_SMS,               // For messaging features
        Manifest.permission.CAMERA,                 // For video recording
        Manifest.permission.READ_CALENDAR,          // For meeting creation
        Manifest.permission.WRITE_CALENDAR          // For calendar management
    )

    // Modern permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "🚀 Starting Sister's Dream Assistant")
        Log.d(TAG, "📱 Initializing app for personalized Gemma 3n model")

        // Check and request all necessary permissions
        checkAndRequestPermissions()

        setContent {
            DreamAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // Enhanced ChatScreen with model integration
                        ChatScreen()
                    }
                }
            }
        }

        // Initialize app lifecycle logging
        initializeAppLifecycle()
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "🔒 Requesting permissions for sister's features: ${missingPermissions.size} missing")

            // Show explanation for critical permissions
            if (missingPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                showPermissionExplanation(
                    "Para que pueda escuchar tu voz perfectamente y entender tus comandos 🎤",
                    missingPermissions.toTypedArray()
                )
            } else {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        } else {
            Log.d(TAG, "✅ All permissions granted - ready for sister's voice")
            onAllPermissionsGranted()
        }
    }

    private fun showPermissionExplanation(message: String, permissions: Array<String>) {
        // For sister's accessibility, we'll show a toast and request anyway
        Toast.makeText(
            this,
            "Necesito algunos permisos para ser tu mejor asistente: $message",
            Toast.LENGTH_LONG
        ).show()

        // Delay slightly to let user read the toast
        lifecycleScope.launch {
            delay(1500)
            permissionLauncher.launch(permissions)
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val grantedPermissions = permissions.filterValues { it }.keys
        val deniedPermissions = permissions.filterValues { !it }.keys

        Log.d(TAG, "✅ Granted permissions: ${grantedPermissions.size}")
        Log.d(TAG, "❌ Denied permissions: ${deniedPermissions.size}")

        // Check critical permissions
        val hasMicrophone = permissions[Manifest.permission.RECORD_AUDIO] == true
        val hasInternet = permissions[Manifest.permission.INTERNET] == true

        when {
            hasMicrophone && hasInternet -> {
                Log.d(TAG, "🎤 Essential permissions granted - sister can use voice features")
                showToast("¡Perfecto! Ya puedo escuchar tu voz y ayudarte con todo 🌟")
                onAllPermissionsGranted()
            }

            hasMicrophone && !hasInternet -> {
                Log.d(TAG, "🎤 Microphone OK, but no internet - limited features")
                showToast("Puedo escucharte pero tendré funciones limitadas sin internet 📶")
                onEssentialPermissionsGranted()
            }

            !hasMicrophone && hasInternet -> {
                Log.d(TAG, "❌ No microphone access - major limitation for sister")
                showToast("Sin micrófono solo puedes escribirme, pero estaré aquí para ti 💕")
                onLimitedPermissionsGranted()
            }

            else -> {
                Log.e(TAG, "❌ Critical permissions denied")
                showToast("Sin permisos básicos tengo funcionalidad muy limitada 😔")
                onMinimalPermissionsGranted()
            }
        }

        // Handle individual permission feedback
        if (deniedPermissions.contains(Manifest.permission.READ_CONTACTS)) {
            Log.d(TAG, "📱 No contact access - WhatsApp features limited")
        }

        if (deniedPermissions.contains(Manifest.permission.CAMERA)) {
            Log.d(TAG, "📹 No camera access - video features disabled")
        }

        if (deniedPermissions.contains(Manifest.permission.READ_CALENDAR)) {
            Log.d(TAG, "📅 No calendar access - meeting features limited")
        }
    }

    private fun onAllPermissionsGranted() {
        Log.d(TAG, "🌟 All permissions granted - full functionality available")
        showToast("¡Listo! Todas las funciones están disponibles para ti 🚀")
    }

    private fun onEssentialPermissionsGranted() {
        Log.d(TAG, "🎤 Essential permissions granted - core functionality available")
        showToast("Las funciones principales están listas. Algunas características necesitarán internet 📶")
    }

    private fun onLimitedPermissionsGranted() {
        Log.d(TAG, "📝 Limited permissions - text-only mode")
        showToast("Modo de solo texto activado. Puedes escribirme y te ayudo igual 💕")
    }

    private fun onMinimalPermissionsGranted() {
        Log.d(TAG, "⚠️ Minimal permissions - very limited functionality")
        showToast("Funcionalidad básica disponible. Para más características, activa los permisos en configuración ⚙️")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun initializeAppLifecycle() {
        Log.d(TAG, "📱 Initializing app lifecycle for sister's model")

        // Monitor app lifecycle for model optimization
        lifecycleScope.launch {
            // Log app start metrics
            Log.d(TAG, "⏱️ App started at: ${System.currentTimeMillis()}")
            Log.d(TAG, "🧠 Ready to load sister's trained Gemma 3n model")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "▶️ App resumed - sister's assistant ready")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ App paused - saving sister's conversation state")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔇 App destroyed - sister's model cleanup initiated")
    }

    // Handle back button for better UX
    override fun onBackPressed() {
        // For sister's accessibility, we might want to ask before closing
        Log.d(TAG, "⬅️ Back button pressed")

        // You could add a confirmation dialog here if needed
        // For now, just use default behavior
        super.onBackPressed()
    }

    // Legacy permission handling (fallback)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MICROPHONE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "🎤 Legacy microphone permission granted")
                showToast("¡Perfecto! Ya puedo escuchar tu voz 🎤")
            } else {
                Log.d(TAG, "❌ Legacy microphone permission denied")
                showToast("Sin micrófono solo puedes escribirme, pero estaré aquí para ti 💕")
            }
        }
    }

    // Helper function to check if essential permissions are available
    private fun hasEssentialPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    // Helper function to check if all permissions are available
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Get permission status for analytics
    private fun getPermissionStatus(): Map<String, Boolean> {
        return requiredPermissions.associateWith { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}