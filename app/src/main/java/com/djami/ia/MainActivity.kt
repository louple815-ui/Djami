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
        
        btnSpeak.setOnClickListener { checkPermissionAndListen() }
        
        // Action du bouton Analyser
        btnAnalyze.setOnClickListener {
            val texteAEcrire = etInput.text.toString()
            if (texteAEcrire.isNotEmpty()) {
                analyserTexte(texteAEcrire)
            } else {
                tvStatus.text = "Écris quelque chose d'abord !"
            }
        }
    }

    // --- LE CERVEAU D'ANALYSE ---
    private fun analyserTexte(texte: String) {
        tvStatus.text = "Analyse en cours..."
        
        // On découpe le texte en mots
        val mots = texte.split(" ", ",", ".", "!", "?")
        var motsCorriges = 0
        val motsSoulignes = StringBuilder()

        // On vérifie chaque mot (Simulation locale, le vrai dictionnaire viendra avec l'IA)
        for (mot in mots) {
            if (mot.isNotEmpty()) {
                // Ici, on pourrait interroger le dictionnaire d'Android.
                // Pour l'instant, on fait une correction basique.
                if (mot.length < 2) {
                    motsSoulignes.append("⚠️ Le mot '$mot' est trop court.\n")
                    motsCorriges++
                }
            }
        }

        // Correction des majuscules et points
        var texteFinal = texte.trim()
        if (texteFinal.isNotEmpty() && texteFinal[0].isLowerCase()) {
            texteFinal = texteFinal.replaceFirstChar { it.uppercase() }
        }
        if (texteFinal.isNotEmpty() && !texteFinal.endsWith(".") && !texteFinal.endsWith("!") && !texteFinal.endsWith("?")) {
            texteFinal += "."
        }

        val resultat = "Analyse terminée.\n" +
                "Mots vérifiés : ${mots.size}\n" +
                if (motsSoulignes.isNotEmpty()) "Problèmes :\n$motsSoulignes" else "Aucune faute grave détectée.\n" +
                "\nTexte corrigé :\n$texteFinal"

        tvResult.text = resultat
        tvStatus.text = "Prêt."
        speak(texteFinal)
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { tvStatus.text = "🎙️ Écoute..." }
            override fun onEndOfSpeech() { tvStatus.text = "Traitement..." }
            override fun onError(error: Int) { tvStatus.text = "Erreur d'écoute." }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texteEntendu = matches[0]
                    etInput.setText(texteEntendu)
                    handleCommand(texteEntendu)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) { tvResult.text = matches[0] }
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
            analyserTexte(texte)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString())
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
