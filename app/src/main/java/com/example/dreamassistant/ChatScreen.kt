package com.example.dreamassistant

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val viewModel = remember { ChatViewModel(context) }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val assistantStatus by viewModel.assistantStatus.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Enhanced Header with Model Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dream Assistant 🌟",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            // Model Status Indicator
            Text(
                text = assistantStatus,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Messages with Enhanced Scrolling
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }

            // Enhanced Loading Indicator
            if (isLoading) {
                item {
                    ChatBubble(
                        message = ChatMessage.Assistant(
                            text = "Procesando con tu modelo personalizado... 🧠",
                            isFromRealModel = false
                        ),
                        isLoading = true
                    )
                }
            }
        }

        // Enhanced Status Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic Status Message
            Text(
                text = when {
                    isSpeaking -> "🔊 Hablando contigo con tu voz personalizada..."
                    isListening -> "🎤 Te escucho perfectamente! Mi modelo te entiende ✨"
                    isLoading -> "🧠 Tu modelo entrenado está pensando la mejor respuesta..."
                    else -> "💕 Presiona el micrófono para hablar con tu asistente personalizado"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = when {
                    isListening -> MaterialTheme.colorScheme.primary
                    isSpeaking -> MaterialTheme.colorScheme.secondary
                    isLoading -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        // WhatsApp-Style Enhanced Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced Text Input
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Escríbeme aquí... 💬") },
                placeholder = { Text("Tu asistente personalizado te escucha...") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && !isListening && !isSpeaking,
                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Enhanced Send Button or Microphone
            if (messageText.isNotBlank() && !isListening) {
                // Send Button with Better Animation
                FloatingActionButton(
                    onClick = {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Enviar mensaje a tu asistente personalizado",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Enhanced Microphone with Better UX
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed || isListening) 1.1f else 1f,
                    animationSpec = tween(100),
                    label = "mic_scale"
                )

                FloatingActionButton(
                    onClick = {
                        if (!isListening) {
                            viewModel.startListening()
                        } else {
                            viewModel.stopListening()
                        }
                    },
                    modifier = Modifier
                        .size(if (isListening) 64.dp else 56.dp)
                        .scale(scale)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isPressed = true
                                    if (!isListening) {
                                        viewModel.startListening()
                                    }
                                },
                                onDragEnd = {
                                    isPressed = false
                                    if (isListening) {
                                        viewModel.stopListening()
                                    }
                                }
                            ) { _, _ ->
                                // Handle drag for WhatsApp-style slide to cancel
                            }
                        },
                    containerColor = when {
                        isListening -> MaterialTheme.colorScheme.error
                        isPressed -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.primary
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (isListening) 12.dp else 6.dp,
                        pressedElevation = 16.dp
                    )
                ) {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Default.VolumeUp
                            isListening -> Icons.Default.Stop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = when {
                            isSpeaking -> "Tu asistente está hablando"
                            isListening -> "Grabando tu voz - Toca para parar"
                            else -> "Presiona para hablar con tu asistente personalizado"
                        },
                        tint = Color.White,
                        modifier = Modifier.size(
                            if (isPressed || isListening) 28.dp else 24.dp
                        )
                    )
                }
            }
        }

        // Enhanced Recording Indicator
        if (isListening) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Enhanced Animated Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val scale by animateFloatAsState(
                            targetValue = if ((System.currentTimeMillis() / 400) % 3 == index.toLong()) 1.8f else 1f,
                            animationSpec = tween(200),
                            label = "dot_animation_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Text(
                    text = "🎤 Tu modelo personalizado te está escuchando...\nToca el micrófono cuando termines",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isLoading: Boolean = false
) {
    val isUser = message is ChatMessage.User
    val messageText = when (message) {
        is ChatMessage.User -> message.text
        is ChatMessage.Assistant -> message.text
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Column {
                if (isLoading) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = messageText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Text(
                        text = messageText,
                        modifier = Modifier.padding(16.dp),
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }

                // Enhanced Indicators Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Input Indicator for User Messages
                    if (message is ChatMessage.User && message.isVoiceInput) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Mensaje de voz",
                                tint = if (isUser) Color.White.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Voz",
                                fontSize = 10.sp,
                                color = if (isUser) Color.White.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            // Show speech confidence if available
                            if (message.speechConfidence < 1.0f) {
                                Text(
                                    text = "${(message.speechConfidence * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    color = if (isUser) Color.White.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Assistant Message Indicators
                    if (message is ChatMessage.Assistant) {
                        // Real Model Indicator
                        if (message.isFromRealModel) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = "Respuesta del modelo personalizado",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Modelo Personalizado",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Speaking Indicator
                        if (message.wasSpoken) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Mensaje hablado",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Hablado",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Action Indicator
                        if (message.hasAction && message.actionType != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = "Acción detectada",
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = when (message.actionType) {
                                        "SendWhatsApp" -> "WhatsApp"
                                        "OpenYouTube" -> "YouTube"
                                        "CreateMeeting" -> "Reunión"
                                        "RecordVideo" -> "Video"
                                        "OpenCamera" -> "Cámara"
                                        else -> "Acción"
                                    },
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Support Level Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val (icon, label, color) = when (message.supportLevel) {
                                ChatMessage.SupportLevel.ENCOURAGING -> Triple(
                                    Icons.Default.Favorite, "💕", MaterialTheme.colorScheme.primary
                                )
                                ChatMessage.SupportLevel.MOTIVATIONAL -> Triple(
                                    Icons.Default.Star, "⭐", MaterialTheme.colorScheme.secondary
                                )
                                ChatMessage.SupportLevel.COMFORTING -> Triple(
                                    Icons.Default.Healing, "🤗", MaterialTheme.colorScheme.tertiary
                                )
                                ChatMessage.SupportLevel.CELEBRATORY -> Triple(
                                    Icons.Default.Celebration, "🎉", MaterialTheme.colorScheme.primary
                                )
                                ChatMessage.SupportLevel.PRACTICAL -> Triple(
                                    Icons.Default.Build, "🔧", MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = color.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Timestamp
                    Text(
                        text = message.getDisplayTime(),
                        fontSize = 9.sp,
                        color = if (isUser) Color.White.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}