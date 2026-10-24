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
import android.widget.EditText
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
    private lateinit var btnAnalyze: Button
    private lateinit var etInput: EditText
    
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
        btnAnalyze = findViewById(R.id.btnAnalyze)
        etInput = findViewById(R.id.etInput)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRENCH
            }
        }

        setupSpeechRecognizer()
        
        // Action du bouton Parler
        btnSpeak.setOnClickListener { checkPermissionAndListen() }
        
        // Action du bouton Analyser
        btnAnalyze.setOnClickListener {
            val texteAEcrire = etInput.text.toString()
            if (texteAEcrire.isNotEmpty()) {
                analyserTexteBasique(texteAEcrire)
            } else {
                tvStatus.text = "Écris quelque chose d'abord !"
            }
        }
    }

    // --- LE MOTEUR D'ANALYSE (En attendant l'IA) ---
    private fun analyserTexteBasique(texte: String) {
        tvStatus.text = "Analyse en cours..."
        var correction = texte
        var message = ""

        // Détection de langue basique
        val texteMinuscule = texte.lowercase(Locale.getDefault())
        if (texteMinuscule.contains("the ") || texteMinuscule.contains("is ") || texteMinuscule.contains("hello")) {
            message = "Langue détectée : Anglais.\n"
        } else if (texteMinuscule.contains("der ") || texteMinuscule.contains("das ") || texteMinuscule.contains("hallo")) {
            message = "Langue détectée : Allemand.\n"
        } else {
            message = "Langue détectée : Français.\n"
        }

        // Règle simple : mettre une majuscule au début
        if (correction.isNotEmpty() && correction[0].isLowerCase()) {
            correction = correction.replaceFirstChar { it.uppercase() }
            message += "Correction : Majuscule ajoutée au début.\n"
        }

        // Règle simple : ajouter un point à la fin
        if (correction.isNotEmpty() && !correction.endsWith(".") && !correction.endsWith("!") && !correction.endsWith("?")) {
            correction += "."
            message += "Correction : Point ajouté à la fin.\n"
        }

        val resultatFinal = "$message\nTexte corrigé : $correction"
        tvResult.text = resultatFinal
        tvStatus.text = "Analyse terminée."
        speak(correction) // DJAMI lit la phrase corrigée
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "🎙️ Écoute..."
            }
            override fun onEndOfSpeech() { tvStatus.text = "Traitement..." }
            override fun onError(error: Int) { tvStatus.text = "Erreur d'écoute." }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texteEntendu = matches[0]
                    etInput.setText(texteEntendu) // On remplit le champ de texte
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
            speak("Ouverture de WhatsApp.")
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) startActivity(intent)
            } catch (e: Exception) { Log.e(TAG, "Erreur WhatsApp", e) }
            return
        } 
        else {
            // Si ce n'est pas une commande, on l'analyse !
            analyserTexteBasique(texte)
            return
        }

        tvStatus.text = reponse
        speak(reponse)
    }

    private fun speak(texte: String) {
        if (texte.isNotEmpty()) tts?.speak(texte, TextToSpeech.QUEUE_FLUSH, null, "DJAMI_TTS")
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString()) // À modifier plus tard pour le multilingue
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
