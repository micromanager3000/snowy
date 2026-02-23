package com.snowy.pet.bridge

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

/**
 * Local HTTP server (port 42618) that ZeroClaw's skills call
 * to trigger hardware actions on the Android device.
 * Binds to 127.0.0.1 only — not accessible from the network.
 */
class HardwareBridge(
    private val onFaceChange: (String) -> Unit,
    private val onCameraCapture: (String) -> String?,  // camera id -> base64 jpeg or null
    private val onTtsSpeak: (String, Float, Float) -> Unit,  // text, pitch, speed
) : NanoHTTPD("127.0.0.1", PORT) {

    companion object {
        const val PORT = 42618
        private const val TAG = "HardwareBridge"
        private const val MIME_JSON = "application/json"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                method == Method.POST && uri == "/face/show" -> handleFaceShow(session)
                method == Method.POST && uri == "/camera/capture" -> handleCameraCapture(session)
                method == Method.POST && uri == "/tts/speak" -> handleTtsSpeak(session)
                method == Method.GET && uri == "/status" -> handleStatus()
                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND, MIME_JSON,
                    """{"error":"Not found: $method $uri"}"""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bridge error: $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, MIME_JSON,
                """{"error":"${e.message?.replace("\"", "'")}"}"""
            )
        }
    }

    private fun handleFaceShow(session: IHTTPSession): Response {
        val body = readBody(session)
        val json = JSONObject(body)
        val state = json.getString("state")
        Log.i(TAG, "Face state: $state")
        onFaceChange(state)
        return jsonResponse("""{"ok":true,"state":"$state"}""")
    }

    private fun handleCameraCapture(session: IHTTPSession): Response {
        val body = readBody(session)
        val json = JSONObject(body)
        val camera = json.optString("camera", "front")
        Log.i(TAG, "Camera capture: $camera")
        val base64 = onCameraCapture(camera)
        return if (base64 != null) {
            jsonResponse("""{"image":"$base64"}""")
        } else {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, MIME_JSON,
                """{"error":"Camera capture failed"}"""
            )
        }
    }

    private fun handleTtsSpeak(session: IHTTPSession): Response {
        val body = readBody(session)
        val json = JSONObject(body)
        val text = json.getString("text")
        val pitch = json.optDouble("pitch", 1.5).toFloat()
        val speed = json.optDouble("speed", 1.0).toFloat()
        Log.i(TAG, "TTS: $text")
        onTtsSpeak(text, pitch, speed)
        return jsonResponse("""{"ok":true}""")
    }

    private fun handleStatus(): Response {
        return jsonResponse("""{"status":"ok","port":$PORT}""")
    }

    private fun readBody(session: IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val buf = ByteArray(contentLength)
        session.inputStream.read(buf, 0, contentLength)
        return String(buf)
    }

    private fun jsonResponse(json: String): Response {
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, json)
    }

}
