package com.djami.ia
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply { text = "Bienvenue sur DJAMI !"; textSize = 24f }
        setContentView(textView)
    }
}
