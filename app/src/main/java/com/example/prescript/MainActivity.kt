package com.example.prescript

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var textClock: TextView
    private lateinit var textView4: TextView
    private var countDownTimer: CountDownTimer? = null

    // Replace with your actual Gemini API Key from https://aistudio.google.com/
    private val apiKey = "YOUR_API_KEY"
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    private val fallbackPrescripts = listOf(
        "Say Thank you 5 times to the first person you see.",
        "Drink a glass of water and take 3 deep breaths.",
        "Complement someone on their work today.",
        "Write down 3 things you are grateful for.",
        "Smile at a stranger today.",
        "Take a 5-minute walk outside.",
        "Stretch your body for 2 minutes.",
        "Call a friend or family member just to say hi.",
        "Read 5 pages of a book.",
        "Declutter one small area of your desk."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        textClock = findViewById(R.id.textClock)
        textView4 = findViewById(R.id.textView4)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startCountdown()
        generatePrescriptWithAI()
    }

    private fun startCountdown() {
        val totalTime = 24 * 60 * 60 * 1000L

        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                textClock.text = String.format(Locale.getDefault(), "%02d : %02d : %02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                generatePrescriptWithAI()
                startCountdown()
            }
        }.start()
    }

    private fun generatePrescriptWithAI() {
        val sharedPref = getSharedPreferences("PreScriptPrefs", Context.MODE_PRIVATE)
        val currentTheme = sharedPref.getString("SELECTED_THEME", "General") ?: "General"

        lifecycleScope.launch {
            try {
                val prompt = "Generate a short, positive, and actionable daily task related to $currentTheme (like 'drink a glass of water' or 'complement a stranger') for a 'Daily Prescript' app. One sentence only."
                val response = generativeModel.generateContent(prompt)
                textView4.text = response.text?.trim() ?: getRandomFallback()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error generating AI content", e)
                textView4.text = getRandomFallback()
            }
        }
    }

    private fun getRandomFallback(): String {
        return fallbackPrescripts[Random.nextInt(fallbackPrescripts.size)]
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}