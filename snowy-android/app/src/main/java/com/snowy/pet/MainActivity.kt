package com.snowy.pet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.snowy.pet.ui.Emotion
import com.snowy.pet.ui.PetFaceScreen

class MainActivity : ComponentActivity() {

    companion object {
        /** Shared mutable state so the bridge can update the face from any thread. */
        val currentEmotion = mutableStateOf(Emotion.HAPPY)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Log but don't block — service runs regardless
        results.forEach { (perm, granted) ->
            android.util.Log.i("MainActivity", "$perm granted=$granted")
        }
        startZeroClawService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val emotion by remember { currentEmotion }
            PetFaceScreen(emotion = emotion)
        }

        requestPermissionsAndStart()
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.CAMERA)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            startZeroClawService()
        }
    }

    private fun startZeroClawService() {
        val intent = Intent(this, ZeroClawService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
