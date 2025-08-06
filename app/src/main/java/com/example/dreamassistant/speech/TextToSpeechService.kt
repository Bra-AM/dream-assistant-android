/*
 * File: TextToSpeechService.kt
 * Location: app/src/main/java/com/example/dreamassistant/speech/TextToSpeechService.kt
 */

package com.example.dreamassistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TextToSpeechService(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    private val _events = MutableSharedFlow<TtsEvent>()
    val events: SharedFlow<TtsEvent> = _events

    sealed class TtsEvent {
        object InitSuccess : TtsEvent()
        object InitFailure : TtsEvent()
        object UtteranceDone : TtsEvent()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}
                override fun onDone(utteranceId: String) {
                    CoroutineScope(Dispatchers.Main).launch {
                        _events.emit(TtsEvent.UtteranceDone)
                    }
                }
                override fun onError(utteranceId: String) {}
            })
            CoroutineScope(Dispatchers.Main).launch { _events.emit(TtsEvent.InitSuccess) }
        } else {
            CoroutineScope(Dispatchers.Main).launch { _events.emit(TtsEvent.InitFailure) }
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTT_ID")
    }

    fun shutdown() {
        tts.shutdown()
    }
}