package com.djami.ia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnSpeak: Button
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val REQUEST_MIC_CODE = 100
    private val TAG = "DJAMI_AI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvResult = findViewById(R.id.tvResult)
        btnSpeak = findViewById(R.id.btnSpeak)

        // Initialisation de la Voix (TTS)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRENCH
                Log.d(TAG, "Moteur TTS initialisé.")
            } else {
                Log.e(TAG, "Erreur d'initialisation du TTS.")
            }
        }

        setupSpeechRecognizer()
        
        btnSpeak.setOnClickListener {
            checkPermissionAndListen()
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvStatus.text = "Erreur : Reconnaissance vocale indisponible"
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "🎙️ Écoute..."
                tvResult.text = "..."
            }
            override fun onEndOfSpeech() {
                tvStatus.text = "Traitement..."
            }
            override fun onError(error: Int) {
                tvStatus.text = "Erreur d'écoute. Réessaie."
                Log.e(TAG, "Erreur SpeechRecognizer: $error")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texteEntendu = matches[0]
                    tvResult.text = texteEntendu
                    handleCommand(texteEntendu)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvResult.text = matches[0]
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleCommand(texte: String) {
        val t = texte.lowercase(Locale.getDefault())
        var reponse = ""

        if (t.contains("heure")) {
            val heureActuelle = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            reponse = "Il est $heureActuelle."
        } 
        else if (t.contains("whatsapp")) {
            reponse = "Ouverture de WhatsApp."
            speak(reponse)
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    reponse = "WhatsApp n'est pas installé."
                    tvStatus.text = reponse
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur ouverture WhatsApp", e)
            }
            return
        } 
        else {
            reponse = "Je n'ai pas encore l'IA locale, mais j'ai bien entendu : $texte"
        }

        tvStatus.text = reponse
        speak(reponse)
    }

    private fun speak(texte: String) {
        if (texte.isNotEmpty()) {
            tts?.speak(texte, TextToSpeech.QUEUE_FLUSH, null, "DJAMI_TTS")
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
        speechRecognizer?.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            tvStatus.text = "Permission micro refusée."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
