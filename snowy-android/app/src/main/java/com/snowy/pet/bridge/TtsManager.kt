package com.snowy.pet.bridge

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Wraps Android's built-in TextToSpeech for Snowy's voice.
 * Default pitch is higher (1.5) for a puppy-like voice.
 */
class TtsManager(context: Context) {

    companion object {
        private const val TAG = "TtsManager"
    }

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ready = true
                Log.i(TAG, "TTS initialized")
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    fun speak(text: String, pitch: Float = 1.5f, speed: Float = 1.0f) {
        if (!ready) {
            Log.w(TAG, "TTS not ready, dropping: $text")
            return
        }
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "snowy-${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
