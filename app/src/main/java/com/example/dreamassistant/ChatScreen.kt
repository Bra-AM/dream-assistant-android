package com.example.dreamassistant

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dreamassistant.ai.LlamaEngine
import kotlinx.coroutines.launch

/**
 * ChatScreen - Displays messages and handles text & voice input
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    llamaEngine: LlamaEngine?,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onVoiceInput: () -> Unit      // callback from MainActivity
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Set engine when ready
    LaunchedEffect(llamaEngine) {
        llamaEngine?.let { viewModel.setLlamaEngine(it) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (llamaEngine?.isModelReady() == true)
                    Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (llamaEngine?.isModelReady() == true)
                        "🌟 Dream Assistant Ready!"
                    else
                        "⏳ Cargando tu modelo personalizado...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                if (llamaEngine?.isModelReady() == true) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Entrenado con tus patrones de voz únicos 💕",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages) { message ->
                ChatMessageItem(message = message)
            }
            if (uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Tu modelo personalizado está pensando...")
                        }
                    }
                }
            }
        }

        // Auto-scroll
        LaunchedEffect(uiState.messages.size) {
            if (uiState.messages.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(uiState.messages.size - 1)
                }
            }
        }

        // Input row: text field, mic, send
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.currentInput,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (llamaEngine?.isModelReady() == true)
                                "Escríbeme o háblame..."
                            else
                                "Esperando tu modelo entrenado..."
                        )
                    },
                    enabled = llamaEngine?.isModelReady() == true && !uiState.isLoading
                )

                Spacer(Modifier.width(8.dp))

                // Mic button triggers voice input
                IconButton(
                    onClick = {
                        Log.d("ChatScreen", "Mic tapped")
                        onVoiceInput()
                    },
                    enabled = true  // always enabled
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (llamaEngine?.isModelReady() == true)
                            MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Send button
                IconButton(
                    onClick = {
                        Log.d("ChatScreen", "✉️ Send tapped (text='${uiState.currentInput}')")
                        viewModel.sendMessage()
                    },
                    enabled = true    // temporarily always enabled for testing
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (llamaEngine?.isModelReady() == true && uiState.currentInput.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Renders a single chat message bubble.
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isFromSister()
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUser -> MaterialTheme.colorScheme.primary
                    message is ChatMessage.Assistant && message.isFromRealModel -> Color(0xFF4CAF50)
                    message is ChatMessage.Assistant && message.supportLevel == ChatMessage.SupportLevel.CELEBRATORY -> Color(0xFFFF9800)
                    message is ChatMessage.Assistant && message.isEmotionalSupport -> Color(0xFF9C27B0)
                    message is ChatMessage.Assistant && message.isBusinessAdvice -> Color(0xFF2196F3)
                    else -> Color(0xFFE0E0E0)
                }
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Assistant metadata
                if (message is ChatMessage.Assistant) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (message.isFromRealModel) Text("🧠 Modelo Real", style = MaterialTheme.typography.labelSmall)
                        when (message.supportLevel) {
                            ChatMessage.SupportLevel.ENCOURAGING -> Text("💪", style = MaterialTheme.typography.labelSmall)
                            ChatMessage.SupportLevel.MOTIVATIONAL -> Text("🚀", style = MaterialTheme.typography.labelSmall)
                            ChatMessage.SupportLevel.COMFORTING -> Text("💕", style = MaterialTheme.typography.labelSmall)
                            ChatMessage.SupportLevel.CELEBRATORY -> Text("🎉", style = MaterialTheme.typography.labelSmall)
                            ChatMessage.SupportLevel.PRACTICAL -> Text("🔧", style = MaterialTheme.typography.labelSmall)
                        }
                        if (message.isBusinessAdvice) Text("💼", style = MaterialTheme.typography.labelSmall)
                        if (message.isEmotionalSupport) Text("🤗", style = MaterialTheme.typography.labelSmall)
                        if (message.inferenceTime > 0) Text("⚡${message.inferenceTime/1000f}s", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // User metadata
                if (message is ChatMessage.User) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (message.isVoiceInput) {
                            Text("🎤 Voz", style = MaterialTheme.typography.labelSmall)
                            if (message.speechConfidence < 1f) Text("${(message.speechConfidence*100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(message.getDisplayTime(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
