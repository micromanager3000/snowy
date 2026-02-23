package com.snowy.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.snowy.pet.bridge.AudioManager
import com.snowy.pet.bridge.CameraManager
import com.snowy.pet.bridge.HardwareBridge
import com.snowy.pet.bridge.SpeechManager
import com.snowy.pet.bridge.TtsManager
import com.snowy.pet.ui.Emotion
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

class ZeroClawService : Service() {

    companion object {
        private const val TAG = "ZeroClawService"
        private const val CHANNEL_ID = "snowy_foreground"
        private const val NOTIFICATION_ID = 1
        private const val MAX_RETRIES = 10
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val ZEROCLAW_GATEWAY = "http://127.0.0.1:42617"
        private const val WAKE_ECSTATIC_DURATION_MS = 3000L
    }

    @Volatile
    private var running = false
    private var process: Process? = null
    private var workerThread: Thread? = null
    private var bridge: HardwareBridge? = null
    private var cameraManager: CameraManager? = null
    private var ttsManager: TtsManager? = null
    private var audioManager: AudioManager? = null
    private var speechManager: SpeechManager? = null
    private var appToken: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startHardwareBridge()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Snowy is starting...")
        startForeground(NOTIFICATION_ID, notification)

        if (!running) {
            running = true
            workerThread = thread(name = "zeroclaw-runner") {
                appToken = ensureAppToken()
                runWithRetries()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        process?.destroyForcibly()
        workerThread?.interrupt()
        stopHardwareBridge()
        super.onDestroy()
    }

    private fun startHardwareBridge() {
        cameraManager = CameraManager(this).also { it.start() }
        ttsManager = TtsManager(this)
        audioManager = AudioManager(this)

        bridge = HardwareBridge(
            onFaceChange = { state ->
                MainActivity.currentEmotion.value = Emotion.fromString(state)
            },
            onCameraCapture = { cameraId ->
                val useFront = cameraId != "rear"
                cameraManager?.captureBase64(useFront)
            },
            onTtsSpeak = { text, pitch, speed ->
                ttsManager?.speak(text, pitch, speed)
            },
            onAudioRecord = { durationSecs ->
                audioManager?.recordBase64(durationSecs)
            }
        )

        try {
            bridge?.start()
            Log.i(TAG, "Hardware bridge started on port ${HardwareBridge.PORT}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start hardware bridge", e)
        }

        // Start continuous speech recognition (wake word: "Snowy")
        speechManager = SpeechManager(
            context = this,
            onWakeWord = { handleWakeWord() },
            onSpeechToChat = { message -> sendToChat(message) }
        )
        speechManager?.start()
        Log.i(TAG, "Speech recognition started (wake word: Snowy)")
    }

    private fun stopHardwareBridge() {
        speechManager?.stop()
        speechManager = null
        bridge?.stop()
        bridge = null
        cameraManager?.stop()
        cameraManager = null
        ttsManager?.shutdown()
        ttsManager = null
        audioManager = null
    }

    /**
     * Wake word "Snowy" detected — wag tail super fast (ECSTATIC) for 3 seconds,
     * then revert to whatever emotion was showing before.
     */
    private fun handleWakeWord() {
        val previousEmotion = MainActivity.currentEmotion.value
        MainActivity.currentEmotion.value = Emotion.ECSTATIC
        Log.i(TAG, "Wake word! Tail wagging (ECSTATIC for ${WAKE_ECSTATIC_DURATION_MS}ms)")

        mainHandler.postDelayed({
            // Only revert if still ECSTATIC (bridge might have changed it)
            if (MainActivity.currentEmotion.value == Emotion.ECSTATIC) {
                MainActivity.currentEmotion.value = previousEmotion
            }
        }, WAKE_ECSTATIC_DURATION_MS)
    }

    /**
     * Send transcribed speech to ZeroClaw's webhook as a chat message.
     * Parses the response for emotional cues to update the face,
     * then speaks the response back via TTS.
     */
    private fun sendToChat(message: String) {
        val token = appToken
        if (token == null) {
            Log.w(TAG, "No app token, can't send to chat: $message")
            return
        }

        thread(name = "webhook-sender") {
            try {
                val url = URL("$ZEROCLAW_GATEWAY/webhook")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 5000
                conn.readTimeout = 30000
                conn.doOutput = true

                val body = JSONObject().put("message", message).toString()
                conn.outputStream.use { it.write(body.toByteArray()) }

                val code = conn.responseCode
                if (code in 200..299) {
                    val responseBody = conn.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(responseBody)
                    val reply = responseJson.optString("response", "")
                    Log.i(TAG, "Speech sent to chat: \"$message\" → \"$reply\"")

                    if (reply.isNotEmpty()) {
                        // Infer emotion from response and update face
                        val emotion = inferEmotion(reply)
                        Log.i(TAG, "Inferred emotion: $emotion")
                        MainActivity.currentEmotion.value = emotion

                        // Strip markdown/emoji for cleaner TTS output
                        val cleanReply = reply
                            .replace(Regex("\\*[^*]+\\*"), "")  // remove *action text*
                            .replace(Regex("[🐾✨🎉💕🐶🦴❤️😊🥺😴🤔😮💤🎵]"), "")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                        if (cleanReply.isNotEmpty()) {
                            ttsManager?.speak(cleanReply)
                        }
                    }
                } else {
                    Log.w(TAG, "Webhook returned $code for: \"$message\"")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send speech to chat", e)
            }
        }
    }

    /**
     * Infer Snowy's emotion from the LLM response text.
     * Checks for action cues (*wags tail*), emoji, and keywords.
     */
    private fun inferEmotion(text: String): Emotion {
        val lower = text.lowercase()

        // Check action descriptions first (highest signal)
        return when {
            // Ecstatic signals
            lower.contains("wags tail") && (lower.contains("excit") || lower.contains("fast")) -> Emotion.ECSTATIC
            lower.contains("jumping") || lower.contains("bouncing") -> Emotion.ECSTATIC
            lower.contains("so happy") || lower.contains("so excited") -> Emotion.ECSTATIC
            lower.contains("spins") || lower.contains("zoomies") -> Emotion.ECSTATIC

            // Playful signals
            lower.contains("play bow") || lower.contains("let's play") -> Emotion.PLAYFUL
            lower.contains("playful") || lower.contains("fetch") -> Emotion.PLAYFUL
            lower.contains("tongue out") || lower.contains("panting") -> Emotion.PLAYFUL

            // Curious signals
            lower.contains("tilts head") || lower.contains("head tilt") -> Emotion.CURIOUS
            lower.contains("perks up ear") || lower.contains("sniffs") -> Emotion.CURIOUS
            lower.contains("curious") || lower.contains("what's that") -> Emotion.CURIOUS
            lower.contains("hmm") || lower.contains("interesting") -> Emotion.CURIOUS

            // Happy signals
            lower.contains("wags tail") -> Emotion.HAPPY
            lower.contains("happy") || lower.contains("glad") -> Emotion.HAPPY
            lower.contains("smile") || lower.contains("woof") -> Emotion.HAPPY
            lower.contains("hi hi") || lower.contains("hello") -> Emotion.HAPPY

            // Alert signals
            lower.contains("ears perk") || lower.contains("alert") -> Emotion.ALERT
            lower.contains("hears something") || lower.contains("what was that") -> Emotion.ALERT

            // Sleepy signals
            lower.contains("yawn") || lower.contains("sleepy") -> Emotion.SLEEPY
            lower.contains("tired") || lower.contains("nap") -> Emotion.SLEEPY
            lower.contains("curls up") || lower.contains("rests") -> Emotion.SLEEPY

            // Lonely signals
            lower.contains("miss") || lower.contains("lonely") -> Emotion.LONELY
            lower.contains("whimper") || lower.contains("sad") -> Emotion.LONELY

            // Confused signals
            lower.contains("confused") || lower.contains("don't understand") -> Emotion.CONFUSED
            lower.contains("puzzled") || lower.contains("huh") -> Emotion.CONFUSED

            // Default to happy for positive responses
            lower.contains("!") || lower.contains("love") -> Emotion.HAPPY

            // Content as fallback
            else -> Emotion.CONTENT
        }
    }

    /**
     * Ensure the Android app has a bearer token for calling ZeroClaw's webhook.
     * Injects a persistent token into config.toml before the daemon starts.
     */
    private fun ensureAppToken(): String {
        val prefs = getSharedPreferences("snowy", MODE_PRIVATE)
        var token = prefs.getString("app_token", null)
        if (token == null) {
            token = UUID.randomUUID().toString()
            prefs.edit().putString("app_token", token).apply()
            Log.i(TAG, "Generated new app token")
        }

        val configFile = File(filesDir, ".zeroclaw/config.toml")
        if (configFile.exists()) {
            var content = configFile.readText()
            if (!content.contains(token)) {
                val regex = Regex("""(paired_tokens\s*=\s*\[)(.*?)(])""")
                content = if (regex.containsMatchIn(content)) {
                    regex.replace(content) { match ->
                        val prefix = match.groupValues[1]
                        val existing = match.groupValues[2].trim()
                        val suffix = match.groupValues[3]
                        if (existing.isEmpty()) {
                            "${prefix}\"${token}\"${suffix}"
                        } else {
                            "${prefix}${existing}, \"${token}\"${suffix}"
                        }
                    }
                } else if (content.contains("[gateway]")) {
                    content.replace(
                        "[gateway]",
                        "[gateway]\npaired_tokens = [\"${token}\"]"
                    )
                } else {
                    content + "\n[gateway]\npaired_tokens = [\"${token}\"]\n"
                }
                configFile.writeText(content)
                Log.i(TAG, "Injected app token into config.toml")
            }
        } else {
            Log.w(TAG, "config.toml not found, can't inject token")
        }

        return token
    }

    private fun runWithRetries() {
        var retries = 0
        var backoffMs = INITIAL_BACKOFF_MS

        while (running && retries < MAX_RETRIES) {
            try {
                updateNotification("Snowy is running")
                val exitCode = runZeroClaw()

                if (!running) break

                Log.w(TAG, "ZeroClaw exited with code $exitCode")
                retries++

                if (retries < MAX_RETRIES) {
                    Log.i(TAG, "Restarting in ${backoffMs}ms (retry $retries/$MAX_RETRIES)")
                    Thread.sleep(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
                }
            } catch (e: InterruptedException) {
                Log.i(TAG, "Runner thread interrupted, stopping")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error running ZeroClaw", e)
                retries++
                if (retries < MAX_RETRIES && running) {
                    try {
                        Thread.sleep(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }

        if (retries >= MAX_RETRIES) {
            Log.e(TAG, "ZeroClaw failed after $MAX_RETRIES retries, giving up")
            updateNotification("Snowy stopped — too many failures")
        }
    }

    private fun runZeroClaw(): Int {
        val binaryPath = File(applicationInfo.nativeLibraryDir, "libzeroclaw.so").absolutePath
        val homeDir = filesDir.absolutePath

        val configDir = File(homeDir, ".zeroclaw")
        configDir.mkdirs()

        Log.i(TAG, "Starting ZeroClaw: $binaryPath")
        Log.i(TAG, "HOME=$homeDir")

        val pb = ProcessBuilder(binaryPath, "daemon")
            .directory(filesDir)
            .redirectErrorStream(true)

        pb.environment()["HOME"] = homeDir

        process = pb.start()

        thread(name = "zeroclaw-logger", isDaemon = true) {
            try {
                process?.inputStream?.bufferedReader()?.forEachLine { line ->
                    Log.i(TAG, line)
                }
            } catch (_: Exception) {}
        }

        updateNotification("Snowy is running")
        return process!!.waitFor()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Snowy Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Snowy running in the background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Snowy")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
