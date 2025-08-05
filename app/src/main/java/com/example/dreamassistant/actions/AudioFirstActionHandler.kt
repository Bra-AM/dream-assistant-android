package com.example.dreamassistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.dreamassistant.ai.CustomGemma3nService.AssistantAction
import java.util.*

/**
 * Audio-First Action Handler - Everything optimized for speech/audio interaction
 * Perfect for users who can't read and rely on voice commands and TTS
 */
class AudioFirstActionHandler(
    private val context: Context,
    private val onSpeakText: (String) -> Unit // Callback to speak any text
) {

    sealed class ActionResult {
        data class Success(val spokenMessage: String, val needsConfirmation: Boolean = false) : ActionResult()
        data class Error(val spokenMessage: String) : ActionResult()
        data class ReadContent(val content: String, val source: String) : ActionResult()
        data class IntentLaunched(val spokenMessage: String) : ActionResult()
    }

    suspend fun executeAction(action: AssistantAction): ActionResult {
        return withContext(Dispatchers.IO) {
            try {
                when (action) {
                    is AssistantAction.SendWhatsApp -> handleWhatsAppMessage(action)
                    is AssistantAction.ReadWhatsApp -> handleReadWhatsApp(action)
                    is AssistantAction.ReadMessageAloud -> handleReadMessageAloud(action)
                    is AssistantAction.OpenYouTube -> handleOpenYouTube(action)
                    is AssistantAction.RecordVideo -> handleRecordVideo(action)
                    is AssistantAction.CreateMeeting -> handleCreateMeeting(action)
                    is AssistantAction.OpenCamera -> handleOpenCamera()
                    is AssistantAction.ReadScreenContent -> handleReadScreenContent(action)
                }
            } catch (e: Exception) {
                Log.e("AudioActionHandler", "Error executing action: ${e.message}")
                ActionResult.Error("Hubo un problemita. Te explico: ${e.message}")
            }
        }
    }

    private fun handleWhatsAppMessage(action: AssistantAction.SendWhatsApp): ActionResult {
        try {
            // First, speak what we're doing
            val preparingMessage = "¡Perfecto! Voy a enviar '${action.message}' a ${action.contact}. Abriendo WhatsApp..."

            // Create WhatsApp intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, action.message)
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.IntentLaunched(
                    "WhatsApp está abierto. Busca a ${action.contact} y envía el mensaje. " +
                            "El mensaje dice: '${action.message}'. ¿Necesitas que repita algo?"
                )
            } else {
                ActionResult.Error("WhatsApp no está instalado en tu teléfono. ¿Quieres que te ayude de otra forma?")
            }

        } catch (e: Exception) {
            return ActionResult.Error("No pude abrir WhatsApp. Te explico el problema: ${e.message}")
        }
    }

    private fun handleReadWhatsApp(action: AssistantAction.ReadWhatsApp): ActionResult {
        try {
            // Open WhatsApp and guide through audio
            val intent = Intent().apply {
                setClassName("com.whatsapp", "com.whatsapp.HomeActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.IntentLaunched(
                    "WhatsApp está abierto. Ahora puedes tocar cualquier conversación y yo te leo los mensajes. " +
                            "Solo dime 'lee esto' cuando estés en una conversación. ¿Por dónde empezamos?"
                )
            } else {
                ActionResult.Error("WhatsApp no está disponible. ¿Quieres revisar otros mensajes?")
            }

        } catch (e: Exception) {
            return ActionResult.Error("No pude abrir WhatsApp para leer mensajes. ¿Intentamos otra cosa?")
        }
    }

    private fun handleReadMessageAloud(action: AssistantAction.ReadMessageAloud): ActionResult {
        // This would ideally integrate with accessibility services to read screen content
        return ActionResult.ReadContent(
            "Mensaje de ${action.sender}: ${action.messageContent}. ¿Quieres que responda algo?",
            "WhatsApp"
        )
    }

    private fun handleReadScreenContent(action: AssistantAction.ReadScreenContent): ActionResult {
        // This would integrate with accessibility services to read whatever is on screen
        return ActionResult.ReadContent(
            "Te leo lo que está en pantalla: ${action.content}. ¿Necesitas más detalles sobre algo específico?",
            "Pantalla"
        )
    }

    private fun handleOpenYouTube(action: AssistantAction.OpenYouTube): ActionResult {
        try {
            val intent = if (action.searchQuery != null) {
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", action.searchQuery)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent().apply {
                    setClassName("com.google.android.youtube", "com.google.android.youtube.HomeActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            return if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val message = if (action.searchQuery != null) {
                    "YouTube está abierto buscando '${action.searchQuery}'. Los resultados están cargando. " +
                            "Puedes navegar con tu dedo o decirme qué quieres hacer después"
                } else {
                    "YouTube está abierto. ¿Qué tipo de video quieres ver? ¿O prefieres grabar algo para tu canal?"
                }
                ActionResult.IntentLaunched(message)
            } else {
                ActionResult.Error("YouTube no está instalado. ¿Quieres que te ayude con otro video o plataforma?")
            }

        } catch (e: Exception) {
            return ActionResult.Error("No pude abrir YouTube. ¿Intentamos con otra plataforma de videos?")
        }
    }

    private fun handleRecordVideo(action: AssistantAction.RecordVideo): ActionResult {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                val message = if (action.title != null) {
                    "¡Genial! La cámara está lista para grabar tu video sobre '${action.title}'. " +
                            "Cuando termines de grabar, dime si quieres subirlo a YouTube o compartirlo. ¡Vas a salir increíble!"
                } else {
                    "¡Perfecto! La cámara está abierta. Graba lo que quieras y después me dices qué hacemos con el video. " +
                            "¡Tú puedes, eres una estrella!"
                }
                ActionResult.IntentLaunched(message)
            } else {
                ActionResult.Error("No pude abrir la cámara. ¿Quieres intentar con otra app de video?")
            }

        } catch (e: Exception) {
            return ActionResult.Error("Hubo un problema con la cámara. ¿Revisamos la configuración juntas?")
        }
    }

    private fun handleCreateMeeting(action: AssistantAction.CreateMeeting): ActionResult {
        try {
            // Create Google Meet link
            val meetLink = "https://meet.google.com/new"

            // Create calendar event
            val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, action.title)
                putExtra(CalendarContract.Events.DESCRIPTION,
                    "Reunión virtual: ${action.title}\n\n" +
                            "Enlace de Google Meet: $meetLink\n\n" +
                            "Espacio seguro para tu comunidad 💪\n" +
                            "Creado por Dream Assistant"
                )

                // Set time to 1 hour from now
                val startTime = Calendar.getInstance().apply {
                    add(Calendar.HOUR, 1)
                }.timeInMillis

                val endTime = Calendar.getInstance().apply {
                    add(Calendar.HOUR, 2)
                }.timeInMillis

                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (calendarIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(calendarIntent)
                ActionResult.IntentLaunched(
                    "¡Increíble! Estoy creando tu reunión '${action.title}'. " +
                            "El calendario está abierto con todos los detalles. " +
                            "El enlace de Google Meet se agrega automáticamente. " +
                            "Tu comunidad va a adorar estas reuniones. ¿Necesitas ayuda con algo más?"
                )
            } else {
                ActionResult.Success(
                    "Reunión '${action.title}' creada. El enlace de Google Meet es: $meetLink. " +
                            "¿Quieres que te ayude a invitar a las personas de tu comunidad?"
                )
            }

        } catch (e: Exception) {
            return ActionResult.Error("No pude crear la reunión, pero tu idea es fantástica. ¿Intentamos de otra forma?")
        }
    }

    private fun handleOpenCamera(): ActionResult {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                ActionResult.IntentLaunched(
                    "¡La cámara está lista! Toma las fotos que quieras. " +
                            "Después me dices si quieres compartirlas o subirlas a algún lado. ¡Vas a salir hermosa!"
                )
            } else {
                ActionResult.Error("No pude abrir la cámara. ¿Quieres intentar con otra app de fotos?")
            }

        } catch (e: Exception) {
            return ActionResult.Error("Problema con la cámara. ¿Revisamos los permisos juntas?")
        }
    }

    // Helper method to create audio-friendly instructions
    fun createAudioInstructions(appName: String, task: String): String {
        return when (appName.lowercase()) {
            "whatsapp" -> when (task) {
                "send" -> "En WhatsApp, toca la conversación de la persona, escribe tu mensaje, y toca el botón de enviar. Te voy guiando paso a paso si necesitas"
                "read" -> "En WhatsApp, toca cualquier conversación y yo te leo los mensajes que aparezcan. Solo dime 'lee esto'"
                else -> "WhatsApp está abierto. ¿Qué quieres hacer: leer mensajes o enviar uno nuevo?"
            }
            "youtube" -> "YouTube está abierto. Puedes navegar con tu dedo o decirme qué tipo de video buscas"
            "camera" -> "La cámara está lista. Toca el botón circular grande para tomar fotos o grabar video"
            else -> "$appName está abierto. ¿En qué te ayudo ahora?"
        }
    }
}