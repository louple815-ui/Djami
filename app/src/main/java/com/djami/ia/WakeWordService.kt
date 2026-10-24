package com.djami.ia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val CHANNEL_ID = "DJAMI_WAKE_CHANNEL"
    private val TAG = "DJAMI_WAKE"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }

        // Démarre l'écoute en boucle
        startListeningLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notification obligatoire pour Android 14
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("DJAMI est à l'écoute")
            .setContentText("Dis 'Hey DJAMI' pour me réveiller.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        
        startForeground(1, notification)
        return START_STICKY
    }

    private fun startListeningLoop() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // En cas d'erreur (silence, bruit), on relance l'écoute après 1 seconde
                restartListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texte = matches[0].lowercase(Locale.getDefault())
                    Log.d(TAG, "Entendu en fond : $texte")
                    
                    // SI ON ENTEND "DJAMI" OU "HEY DJAMI"
                    if (texte.contains("djami") || texte.contains("jami")) {
                        declencherAction(texte)
                    }
                }
                // On relance l'écoute
                restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texte = matches[0].lowercase(Locale.getDefault())
                    if (texte.contains("djami") || texte.contains("jami")) {
                        declencherAction(texte)
                    }
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur micro", e)
        }
    }

    private fun restartListening() {
        // Petite pause de 1,5 seconde pour ne pas faire chauffer le téléphone
        Thread {
            Thread.sleep(1500)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        }.start()
    }

    private fun declencherAction(texte: String) {
        // Le téléphone vibre pour te dire qu'il a entendu
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }

        tts?.speak("Oui, je vous écoute.", TextToSpeech.QUEUE_FLUSH, null, "DJAMI_TTS")

        // Ouvre l'application au premier plan
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Service d'écoute DJAMI",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
