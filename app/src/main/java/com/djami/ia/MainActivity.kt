package com.djami.ia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
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
            text = "Prêt."
            textSize = 18f
            setPadding(0, 0, 0, 20)
        }

        resultText = TextView(this).apply {
            text = "Ce que tu dis apparaîtra ici..."
            textSize = 22f
            setPadding(0, 0, 0, 50)
        }

        val micButton = Button(this).apply {
            text = "🎤 Appuyer pour parler"
            textSize = 20f
            setOnClickListener {
                checkPermissionAndListen()
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(resultText)
        layout.addView(micButton)

        setContentView(layout)
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "🎙️ Écoute en cours..."
                resultText.text = "..."
            }

            override fun onEndOfSpeech() {
                statusText.text = "Traitement..."
            }

            override fun onError(error: Int) {
                statusText.text = "Erreur d'écoute. Réessaie."
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texteEntendu = matches[0]
                    resultText.text = texteEntendu
                    executerCommande(texteEntendu)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    resultText.text = matches[0]
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // --- LE CERVEAU DE DJAMI ---
    private fun executerCommande(texte: String) {
        val t = texte.lowercase(Locale.getDefault())

        if (t.contains("heure")) {
            val heureActuelle = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            statusText.text = "Il est $heureActuelle."
        } 
        else if (t.contains("whatsapp")) {
            statusText.text = "Ouverture de WhatsApp..."
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    startActivity(intent)
                    statusText.text = "WhatsApp est ouvert !"
                } else {
                    statusText.text = "WhatsApp n'est pas installé sur ce téléphone."
                }
            } catch (e: Exception) {
                statusText.text = "Impossible d'ouvrir WhatsApp."
            }
        } 
        else {
            statusText.text = "Je n'ai pas compris la commande. Réessaie !"
        }
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC_CODE)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            statusText.text = "Erreur : Permission micro refusée."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
