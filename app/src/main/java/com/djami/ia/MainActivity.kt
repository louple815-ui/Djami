package com.djami.ia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    private val REQUEST_CONTACTS_CODE = 101
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
        
        btnSpeak.setOnClickListener { checkPermissionsAndListen() }
        
        btnAnalyze.setOnClickListener {
            val texteAEcrire = etInput.text.toString()
            if (texteAEcrire.isNotEmpty()) {
                analyserTexte(texteAEcrire)
            } else {
                tvStatus.text = "Écris quelque chose d'abord !"
            }
        }
    }

    // --- LE CERVEAU DES COMMANDES ANDROID ---
    private fun handleCommand(texte: String) {
        val t = texte.lowercase(Locale.getDefault())
        
        when {
            t.contains("heure") -> {
                val heureActuelle = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                parler("Il est $heureActuelle.")
            }
            t.contains("whatsapp") -> ouvrirApplication("com.whatsapp", "WhatsApp")
            t.contains("facebook") -> ouvrirApplication("com.facebook.katana", "Facebook")
            t.contains("sms") || t.contains("message") -> ouvrirSms()
            t.contains("appel") || t.contains("appelle") -> {
                // On cherche si un nom est mentionné après "appelle"
                val nomRecherche = t.replace("appelle", "").replace("appel", "").replace("à", "").trim()
                if (nomRecherche.isNotEmpty()) {
                    chercherContactEtAppeler(nomRecherche)
                } else {
                    ouvrirClavierTelephone("")
                }
            }
            else -> analyserTexte(texte)
        }
    }

    // --- OUTILS D'ACTION ---
    private fun ouvrirApplication(packageName: String, nomApp: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
                parler("J'ouvre $nomApp.")
            } else {
                parler("$nomApp n'est pas installé.")
            }
        } catch (e: Exception) {
            parler("Impossible d'ouvrir $nomApp.")
        }
    }

    private fun ouvrirSms() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            startActivity(intent)
            parler("J'ouvre tes messages.")
        } catch (e: Exception) {
            parler("Je ne trouve pas l'application SMS.")
        }
    }

    private fun chercherContactEtAppeler(nom: String) {
        tvStatus.text = "Recherche du contact $nom..."
        val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(nom))
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                // On a trouvé le contact !
                val numeroIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nomIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                
                val numero = it.getString(numeroIndex)
                val nomComplet = it.getString(nomIndex)

                ouvrirClavierTelephone(numero)
                parler("J'ai trouvé $nomComplet. Je prépare l'appel.")
            } else {
                parler("Je n'ai pas trouvé de contact nommé $nom.")
                tvStatus.text = "Contact introuvable."
            }
        }
    }

    private fun ouvrirClavierTelephone(numero: String) {
        // ACTION_DIAL ouvre le clavier avec le numéro, sans lancer l'appel directement (sécurité)
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$numero")
        }
        startActivity(intent)
    }

    private fun parler(texte: String) {
        tvStatus.text = texte
        if (texte.isNotEmpty()) {
            tts?.speak(texte, TextToSpeech.QUEUE_FLUSH, null, "DJAMI_TTS")
        }
    }

    // --- GESTION PERMISSIONS (Micro et Contacts) ---
    private fun checkPermissionsAndListen() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
        val listToRequest = mutableListOf<String>()

        for (p in permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listToRequest.add(p)
            }
        }

        if (listToRequest.isEmpty()) {
            startListening()
        } else {
            ActivityCompat.requestPermissions(this, listToRequest.toTypedArray(), REQUEST_MIC_CODE)
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

    // ... (Le reste des fonctions setupSpeechRecognizer, analyserTexte, onRequestPermissionsResult, onDestroy restent identiques)
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

    private fun analyserTexte(texte: String) {
        tvStatus.text = "Analyse en cours..."
        var texteFinal = texte.trim()
        if (texteFinal.isNotEmpty() && texteFinal[0].isLowerCase()) texteFinal = texteFinal.replaceFirstChar { it.uppercase() }
        if (texteFinal.isNotEmpty() && !texteFinal.endsWith(".") && !texteFinal.endsWith("!") && !texteFinal.endsWith("?")) texteFinal += "."

        tvResult.text = "Texte corrigé :\n$texteFinal"
        parler(texteFinal)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startListening()
            } else {
                tvStatus.text = "Permissions refusées. DJAMI a besoin du micro et des contacts."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
