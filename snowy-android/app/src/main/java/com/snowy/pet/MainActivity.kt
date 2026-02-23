package com.snowy.pet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = "Snowy is starting..."
            textSize = 20f
            setPadding(48, 48, 48, 48)
        }
        setContentView(textView)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
                return // Wait for permission result before starting service
            }
        }

        startZeroClawService()
        textView.text = "Snowy is running.\n\nYou can close this app — Snowy will keep running in the background."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            // Start service regardless — it works without notification permission,
            // but the notification won't show.
            startZeroClawService()
            val textView = findViewById<TextView>(android.R.id.content)
                ?: (window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content))
                    ?.getChildAt(0) as? TextView
            textView?.text = "Snowy is running.\n\nYou can close this app — Snowy will keep running in the background."
        }
    }

    private fun startZeroClawService() {
        val intent = Intent(this, ZeroClawService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
