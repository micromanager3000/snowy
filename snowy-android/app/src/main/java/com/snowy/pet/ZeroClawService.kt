package com.snowy.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

class ZeroClawService : Service() {

    companion object {
        private const val TAG = "ZeroClawService"
        private const val CHANNEL_ID = "snowy_foreground"
        private const val NOTIFICATION_ID = 1
        private const val MAX_RETRIES = 10
        private const val INITIAL_BACKOFF_MS = 1000L
    }

    @Volatile
    private var running = false
    private var process: Process? = null
    private var workerThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Snowy is starting...")
        startForeground(NOTIFICATION_ID, notification)

        if (!running) {
            running = true
            workerThread = thread(name = "zeroclaw-runner") {
                runWithRetries()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        process?.destroyForcibly()
        workerThread?.interrupt()
        super.onDestroy()
    }

    private fun runWithRetries() {
        var retries = 0
        var backoffMs = INITIAL_BACKOFF_MS

        while (running && retries < MAX_RETRIES) {
            try {
                updateNotification("Snowy is running (attempt ${retries + 1})")
                val exitCode = runZeroClaw()

                if (!running) break // Clean shutdown

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

        // Ensure .zeroclaw directory exists (config is deployed here via ADB push)
        val configDir = File(homeDir, ".zeroclaw")
        configDir.mkdirs()

        Log.i(TAG, "Starting ZeroClaw: $binaryPath")
        Log.i(TAG, "HOME=$homeDir")

        val pb = ProcessBuilder(binaryPath, "daemon")
            .directory(filesDir)
            .redirectErrorStream(true)

        pb.environment()["HOME"] = homeDir

        process = pb.start()

        // Log stdout/stderr in background
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
