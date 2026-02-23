package com.snowy.pet.bridge

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Records audio clips from the microphone.
 * Returns base64-encoded audio (OGG/Opus on API 29+, AMR-NB fallback).
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
        private const val MAX_DURATION_SECS = 30
    }

    /**
     * Record audio for the specified duration and return as base64.
     * @param durationSecs recording length (capped at 30s)
     * @return Pair of (base64 audio data, format string) or null on failure
     */
    fun recordBase64(durationSecs: Int = 5): Pair<String, String>? {
        val duration = durationSecs.coerceIn(1, MAX_DURATION_SECS)
        val outputFile = File(context.cacheDir, "snowy-audio.ogg")

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)

            // OGG/Opus is well-supported and compact
            recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(16000)

            recorder.setOutputFile(outputFile.absolutePath)
            recorder.setMaxDuration(duration * 1000)

            val latch = CountDownLatch(1)
            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    latch.countDown()
                }
            }

            recorder.prepare()
            recorder.start()
            Log.i(TAG, "Recording ${duration}s of audio...")

            // Wait for max duration or timeout
            latch.await((duration + 2).toLong(), TimeUnit.SECONDS)

            recorder.stop()
            recorder.release()
            Log.i(TAG, "Recording complete: ${outputFile.length()} bytes")

            if (!outputFile.exists() || outputFile.length() == 0L) {
                Log.e(TAG, "Recording file empty or missing")
                return null
            }

            val bytes = outputFile.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            return Pair(base64, "ogg")
        } catch (e: Exception) {
            Log.e(TAG, "Recording failed", e)
            try { recorder.release() } catch (_: Exception) {}
            return null
        } finally {
            outputFile.delete()
        }
    }
}
