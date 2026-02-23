package com.snowy.pet.bridge

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Captures a single JPEG frame from the specified camera.
 * Uses Camera2 API directly — no preview surface needed.
 */
class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "SnowyCameraManager"
        private const val CAPTURE_TIMEOUT_SECS = 10L
    }

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    fun start() {
        handlerThread = HandlerThread("snowy-camera").also { it.start() }
        handler = Handler(handlerThread!!.looper)
    }

    fun stop() {
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    /**
     * Capture a single JPEG image and return as base64 string.
     * @param useFront true for front camera, false for rear
     * @return base64-encoded JPEG, or null on failure
     */
    fun captureBase64(useFront: Boolean = true): String? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val facing = cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING)
            if (useFront) facing == CameraCharacteristics.LENS_FACING_FRONT
            else facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: run {
            Log.e(TAG, "No ${if (useFront) "front" else "rear"} camera found")
            return null
        }

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val jpegSizes = streamMap.getOutputSizes(ImageFormat.JPEG)
        // Pick a reasonable size (not too large — save bandwidth)
        val targetSize = jpegSizes.firstOrNull { it.width <= 1280 } ?: jpegSizes.last()

        val imageReader = ImageReader.newInstance(targetSize.width, targetSize.height, ImageFormat.JPEG, 1)
        val latch = CountDownLatch(1)
        var resultBase64: String? = null

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                resultBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                latch.countDown()
            }
        }, handler)

        try {
            val deviceLatch = CountDownLatch(1)
            var cameraDevice: CameraDevice? = null

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    deviceLatch.countDown()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    deviceLatch.countDown()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    camera.close()
                    deviceLatch.countDown()
                }
            }, handler)

            if (!deviceLatch.await(CAPTURE_TIMEOUT_SECS, TimeUnit.SECONDS)) {
                Log.e(TAG, "Camera open timeout")
                return null
            }

            val camera = cameraDevice ?: return null

            val sessionLatch = CountDownLatch(1)
            var captureSession: CameraCaptureSession? = null

            camera.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        sessionLatch.countDown()
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Session config failed")
                        sessionLatch.countDown()
                    }
                },
                handler
            )

            if (!sessionLatch.await(CAPTURE_TIMEOUT_SECS, TimeUnit.SECONDS)) {
                Log.e(TAG, "Session config timeout")
                camera.close()
                return null
            }

            val session = captureSession ?: run {
                camera.close()
                return null
            }

            val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }.build()

            session.capture(captureRequest, null, handler)

            if (!latch.await(CAPTURE_TIMEOUT_SECS, TimeUnit.SECONDS)) {
                Log.e(TAG, "Capture timeout")
            }

            session.close()
            camera.close()
        } finally {
            imageReader.close()
        }

        return resultBase64
    }
}
