package com.example.prescript

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var textClock: TextView
    private lateinit var textView4: TextView
    private lateinit var streakImage: ImageView
    private lateinit var streakCountText: TextView
    private lateinit var btnCompleted: Button
    private lateinit var drawerLayout: DrawerLayout
    
    private var countDownTimer: CountDownTimer? = null
    private var streakCount = 0
    private var isTaskCompleted = false
    private var currentSentenceEn: String = ""
    private var currentSentenceTh: String = ""
    private var isShowingThai = false

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uid: String? get() = auth.currentUser?.uid

    private val apiKey = "AIzaSyBqPG3GFOZ1UclMkbfh2LEdQtiEMSG4YAo"
    
    // Updated to remove explicit v1beta which was causing 404 for this model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
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

    private fun getPrefs() = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (uid == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        textClock = findViewById(R.id.textClock)
        textView4 = findViewById(R.id.textView4)
        streakImage = findViewById(R.id.streak_image)
        streakCountText = findViewById(R.id.streak_count)
        btnCompleted = findViewById(R.id.btncompleted)
        val logoImage = findViewById<ImageView>(R.id.imageView5)

        logoImage.setOnLongClickListener {
            Toast.makeText(this, "Dev Mode: Resetting Timer...", Toast.LENGTH_SHORT).show()
            getPrefs().edit().putLong("TIMER_END_TIME", System.currentTimeMillis() + 1000).apply()
            startCountdown()
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_main -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_history -> startActivity(Intent(this, History::class.java))
                R.id.nav_profile -> startActivity(Intent(this, Profile::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        findViewById<ImageView>(R.id.translate).setOnClickListener {
            toggleTranslation()
        }

        val sharedPref = getPrefs()
        streakCount = sharedPref.getInt("STREAK_COUNT", 0)
        isTaskCompleted = sharedPref.getBoolean("IS_TASK_COMPLETED", false)
        currentSentenceEn = sharedPref.getString("CURRENT_SENTENCE_EN", "") ?: ""
        currentSentenceTh = sharedPref.getString("CURRENT_SENTENCE_TH", "") ?: ""
        
        updateStreakUI()
        
        if (isTaskCompleted) {
            btnCompleted.isEnabled = false
            btnCompleted.text = "Completed for today"
        }

        if (currentSentenceEn.isEmpty()) {
            generatePrescriptWithAI()
        } else {
            textView4.text = if (isShowingThai && currentSentenceTh.isNotEmpty()) currentSentenceTh else currentSentenceEn
        }

        btnCompleted.setOnClickListener {
            if (!isTaskCompleted) {
                isTaskCompleted = true
                streakCount++
                saveStreakData()
                updateStreakUI()
                btnCompleted.isEnabled = false
                btnCompleted.text = "Completed for today"
                Toast.makeText(this, "Task Completed! Streak: $streakCount", Toast.LENGTH_SHORT).show()
            }
        }

        startCountdown()
    }

    private fun toggleTranslation() {
        if (currentSentenceEn.isEmpty()) return

        if (isShowingThai) {
            textView4.text = currentSentenceEn
            isShowingThai = false
        } else {
            if (currentSentenceTh.isNotEmpty()) {
                textView4.text = currentSentenceTh
                isShowingThai = true
            } else {
                translateToThai()
            }
        }
    }

    private fun translateToThai() {
        lifecycleScope.launch {
            try {
                val prompt = "Translate this English task to Thai naturally: '$currentSentenceEn'. Output only the Thai translation."
                val response = generativeModel.generateContent(prompt)
                currentSentenceTh = response.text?.trim() ?: ""
                
                if (currentSentenceTh.isNotEmpty()) {
                    getPrefs().edit().putString("CURRENT_SENTENCE_TH", currentSentenceTh).apply()
                    textView4.text = currentSentenceTh
                    isShowingThai = true
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Translation Problem: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Translation failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCountdown() {
        val sharedPref = getPrefs()
        var endTime = sharedPref.getLong("TIMER_END_TIME", 0L)
        val currentTime = System.currentTimeMillis()

        if (endTime == 0L || currentTime >= endTime) {
            if (endTime != 0L) {
                saveMissionToHistory()
            }
            endTime = currentTime + (24 * 60 * 60 * 1000L)
            sharedPref.edit().putLong("TIMER_END_TIME", endTime).apply()
            
            isTaskCompleted = false
            currentSentenceEn = ""
            currentSentenceTh = ""
            saveStreakData()
            generatePrescriptWithAI()
        }

        val remainingTime = endTime - currentTime

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                textClock.text = String.format(Locale.getDefault(), "%02d : %02d : %02d", hours, minutes, seconds)
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                saveMissionToHistory()
                if (!isTaskCompleted) streakCount = 0
                isTaskCompleted = false
                currentSentenceEn = ""
                currentSentenceTh = ""
                saveStreakData()
                updateStreakUI()
                btnCompleted.isEnabled = true
                btnCompleted.text = "I have completed the prescript."
                getPrefs().edit().putLong("TIMER_END_TIME", 0L).apply()
                startCountdown()
            }
        }.start()
    }

    private fun saveMissionToHistory() {
        val sharedPref = getPrefs()
        val historyJson = sharedPref.getString("MISSION_HISTORY", null)
        val historyList = if (historyJson != null) {
            val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
            Gson().fromJson<MutableList<HistoryItem>>(historyJson, type)
        } else {
            mutableListOf<HistoryItem>()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.format(Date())
        
        if (currentSentenceEn.isNotEmpty()) {
            historyList.add(HistoryItem(currentSentenceEn, isTaskCompleted, date))
            sharedPref.edit().putString("MISSION_HISTORY", Gson().toJson(historyList)).apply()
        }
    }

    private fun updateStreakUI() {
        streakCountText.text = streakCount.toString()
        val colorRes = if (!isTaskCompleted) R.color.black else {
            when {
                streakCount >= 101 -> R.color.purple
                streakCount >= 11 -> R.color.blue
                streakCount >= 1 -> R.color.orange
                else -> R.color.black
            }
        }
        streakImage.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
    }

    private fun saveStreakData() {
        val sharedPref = getPrefs()
        with(sharedPref.edit()) {
            putInt("STREAK_COUNT", streakCount)
            putBoolean("IS_TASK_COMPLETED", isTaskCompleted)
            putString("CURRENT_SENTENCE_EN", currentSentenceEn)
            putString("CURRENT_SENTENCE_TH", currentSentenceTh)
            apply()
        }
    }

    private fun generatePrescriptWithAI() {
        val sharedPref = getPrefs()
        val themes = sharedPref.getString("SELECTED_THEMES", "General") ?: "General"

        textView4.text = "Generating task..."

        lifecycleScope.launch {
            try {
                val prompt = "Generate a short, positive, and actionable daily task related to $themes. One sentence only."
                val response = generativeModel.generateContent(prompt)
                val aiText = response.text?.trim()
                
                if (!aiText.isNullOrEmpty()) {
                    currentSentenceEn = aiText
                    currentSentenceTh = ""
                    isShowingThai = false
                    textView4.text = currentSentenceEn
                    saveStreakData()
                } else {
                    Log.e("MainActivity", "AI Generation Problem: Empty response")
                    useFallback()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "AI Generation Problem: ${e.message}", e)
                useFallback()
            }
        }
    }

    private fun useFallback() {
        currentSentenceEn = getRandomFallback()
        currentSentenceTh = ""
        isShowingThai = false
        textView4.text = currentSentenceEn
        saveStreakData()
    }

    private fun getRandomFallback(): String {
        return fallbackPrescripts[Random.nextInt(fallbackPrescripts.size)]
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}