package com.snowy.pet.bridge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Continuous speech recognition using Android's built-in SpeechRecognizer.
 * Listens for the wake word "Snowy" and routes speech to ZeroClaw.
 *
 * When "Snowy" is detected:
 *   1. onWakeWord fires (tail wag)
 *   2. The rest of the utterance is passed to onSpeechToChat
 */
class SpeechManager(
    private val context: Context,
    private val onWakeWord: () -> Unit,
    private val onSpeechToChat: (String) -> Unit,
) {
    companion object {
        private const val TAG = "SpeechManager"
        private const val WAKE_WORD = "snowy"
        private const val RESTART_DELAY_MS = 300L
        private const val ERROR_RESTART_DELAY_MS = 1500L
    }

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var listening = false

    fun start() {
        listening = true
        mainHandler.post { initAndListen() }
    }

    fun stop() {
        listening = false
        mainHandler.post {
            try {
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (_: Exception) {}
            recognizer = null
        }
    }

    private fun initAndListen() {
        if (!listening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on this device")
            return
        }

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(recognitionListener)
            Log.i(TAG, "SpeechRecognizer created, starting continuous listening")
        }

        startListening()
    }

    private fun startListening() {
        if (!listening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            scheduleRestart(ERROR_RESTART_DELAY_MS)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (listening) {
            mainHandler.postDelayed({ startListening() }, delayMs)
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""

            if (text.isNotEmpty()) {
                Log.i(TAG, "Heard: \"$text\"")
            }

            if (text.contains(WAKE_WORD, ignoreCase = true)) {
                Log.i(TAG, "Wake word detected!")
                onWakeWord()
                // Strip wake word and send the rest as a chat message
                val message = text
                    .replace(Regex("(?i)\\b${WAKE_WORD}\\b[,.]?\\s*"), "")
                    .trim()
                if (message.isNotEmpty()) {
                    onSpeechToChat(message)
                }
            }

            scheduleRestart(RESTART_DELAY_MS)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Could react to wake word faster here, but final results are more reliable
        }

        override fun onError(error: Int) {
            // NO_MATCH and SPEECH_TIMEOUT are normal during continuous listening
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Silent restart — these happen every few seconds of silence
                }
                else -> {
                    val name = errorName(error)
                    Log.w(TAG, "Recognition error: $name")
                }
            }
            scheduleRestart(
                if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                ) RESTART_DELAY_MS else ERROR_RESTART_DELAY_MS
            )
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun errorName(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "BUSY"
        SpeechRecognizer.ERROR_SERVER -> "SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
        else -> "UNKNOWN($error)"
    }
}
