package com.djami.ia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var btnWakeMode: Button
    private lateinit var etInput: EditText
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val REQUEST_CODE = 100
    private val TAG = "DJAMI_AI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvResult = findViewById(R.id.tvResult)
        btnSpeak = findViewById(R.id.btnSpeak)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnWakeMode = findViewById(R.id.btnWakeMode)
        etInput = findViewById(R.id.etInput)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }

        setupSpeechRecognizer()
        
        btnSpeak.setOnClickListener { checkPermissionsAndListen() }
        
        btnAnalyze.setOnClickListener {
            val texteAEcrire = etInput.text.toString()
            if (texteAEcrire.isNotEmpty()) analyserTexte(texteAEcrire) else tvStatus.text = "Écris quelque chose !"
        }

        // Action du bouton Jarvis
        btnWakeMode.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val serviceIntent = Intent(this, WakeWordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "Mode Jarvis activé ! Tu peux verrouiller ton téléphone.", Toast.LENGTH_LONG).show()
                tvStatus.text = "Mode écoute en fond activé."
            } else {
                // Demander les permissions (Micro et Notifications pour Android 13+)
                val perms = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQUEST_CODE)
            }
        }
    }

    // (Le reste du code reste identique à la version précédente : handleCommand, ouvrirApplication, analyserTexte, etc.)
    private fun handleCommand(texte: String) {
        val t = texte.lowercase(Locale.getDefault())
        when {
            t.contains("heure") -> parler("Il est " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) + ".")
            t.contains("whatsapp") -> ouvrirApplication("com.whatsapp", "WhatsApp")
            t.contains("facebook") -> ouvrirApplication("com.facebook.katana", "Facebook")
            t.contains("sms") || t.contains("message") -> ouvrirSms()
            t.contains("appel") || t.contains("appelle") -> {
                val nom = t.replace("appelle", "").replace("appel", "").replace("à", "").trim()
                if (nom.isNotEmpty()) chercherContactEtAppeler(nom) else ouvrirClavierTelephone("")
            }
            else -> analyserTexte(texte)
        }
    }

    private fun ouvrirApplication(pkg: String, nom: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) { startActivity(intent); parler("J'ouvre $nom.") } else parler("$nom n'est pas installé.")
        } catch (e: Exception) { parler("Erreur.") }
    }

    private fun ouvrirSms() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
            startActivity(intent)
            parler("J'ouvre tes messages.")
        } catch (e: Exception) { parler("Impossible d'ouvrir les SMS.") }
    }

    private fun chercherContactEtAppeler(nom: String) {
        tvStatus.text = "Recherche de $nom..."
        val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(nom))
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val num = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val nomComplet = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                ouvrirClavierTelephone(num)
                parler("J'ai trouvé $nomComplet.")
            } else {
                parler("Contact $nom introuvable.")
            }
        }
    }

    private fun ouvrirClavierTelephone(numero: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")))
    }

    private fun parler(texte: String) {
        tvStatus.text = texte
        tts?.speak(texte, TextToSpeech.QUEUE_FLUSH, null, "DJAMI_TTS")
    }

    private fun checkPermissionsAndListen() {
        val perms = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startListening()
        } else {
            ActivityCompat.requestPermissions(this, perms, REQUEST_CODE)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString())
        }
        speechRecognizer?.startListening(intent)
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { tvStatus.text = "🎙️ Écoute..." }
            override fun onEndOfSpeech() { tvStatus.text = "Traitement..." }
            override fun onError(error: Int) { tvStatus.text = "Erreur d'écoute." }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    etInput.setText(it)
                    handleCommand(it)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { tvResult.text = it }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun analyserTexte(texte: String) {
        var t = texte.trim().replaceFirstChar { it.uppercase() }
        if (!t.endsWith(".") && !t.endsWith("!") && !t.endsWith("?")) t += "."
        tvResult.text = "Texte corrigé :\n$t"
        parler(t)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
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
