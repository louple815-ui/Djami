package com.djami.ia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private val REQUEST_MIC_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        val title = TextView(this).apply {
            text = "Assistant DJAMI"
            textSize = 28f
            setPadding(0, 0, 0, 50)
        }

        statusText = TextView(this).apply {
            text = "Appuie sur le bouton pour parler."
            textSize = 20f
            setPadding(0, 0, 0, 50)
        }

        val micButton = Button(this).apply {
            text = "🎤 Activer le Microphone"
            textSize = 20f
            setOnClickListener {
                checkPermissionAndListen()
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(micButton)

        setContentView(layout)
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Micro activé ! (La reconnaissance vocale arrive bientôt)."
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.text = "Merci ! Micro autorisé."
            } else {
                statusText.text = "Erreur : Tu dois autoriser le micro pour parler à DJAMI."
            }
        }
    }
}
