package com.example.prescript

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var textClock: TextView
    private lateinit var textView4: TextView
    private lateinit var streakImage: ImageView
    private lateinit var streakCountText: TextView
    private lateinit var btnCompleted: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var mainLayout: View
    private lateinit var btnMenu: ImageView
    
    private var countDownTimer: CountDownTimer? = null
    private var streakCount = 0
    private var highestStreak = 0
    private var isTaskCompleted = false
    private var currentSentenceEn: String = ""
    private var currentSentenceTh: String = ""
    private var isShowingThai = false
    
    private var revealJob: Job? = null
    private val shuffleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_@#$&*?<~ "

    private lateinit var soundPool: SoundPool
    private var shuffleSoundId: Int = 0
    private var soundLoaded = false

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uid: String? get() = auth.currentUser?.uid

    private fun getPrefs() = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)

    private fun getGenerativeModel(): GenerativeModel {
        val prefs = getPrefs()
        val apiKey = prefs.getString("GEMINI_API_KEY", "") ?: ""
        var modelName = prefs.getString("GEMINI_MODEL_NAME", "models/gemini-2.5-flash") ?: "models/gemini-2.5-flash"
        
        if (!modelName.contains("/")) {
            modelName = "models/$modelName"
        }
        
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        } else {
            Log.d("MainActivity", "Notification permission denied.")
        }
    }

    @SuppressLint("SetTextI118n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (uid == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        mainLayout = findViewById(R.id.main)
        btnMenu = findViewById(R.id.btn_menu)
        textClock = findViewById(R.id.textClock)
        textView4 = findViewById(R.id.textView4)
        streakImage = findViewById(R.id.streak_image)
        streakCountText = findViewById(R.id.streak_count)
        btnCompleted = findViewById(R.id.btncompleted)
        val logoImage = findViewById<ImageView>(R.id.imageView5)

        applyBackgroundTheme()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        shuffleSoundId = soundPool.load(this, R.raw.shuffle_sound, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundLoaded = true
        }

        logoImage.setOnLongClickListener {
            getPrefs().edit().putLong("TIMER_END_TIME", System.currentTimeMillis() + 3000L).apply()
            startCountdown()
            showNotification("Dev Mode Timer Set", "Timer reset to 3 seconds for current prescript.", 103)
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnMenu.setOnClickListener {
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

        findViewById<ImageView>(R.id.translate).setOnClickListener { toggleTranslation() }

        val sharedPref = getPrefs()
        streakCount = sharedPref.getInt("STREAK_COUNT", 0)
        highestStreak = sharedPref.getInt("HIGHEST_STREAK", 0)
        isTaskCompleted = sharedPref.getBoolean("IS_TASK_COMPLETED", false)
        currentSentenceEn = sharedPref.getString("CURRENT_SENTENCE_EN", "") ?: ""
        currentSentenceTh = sharedPref.getString("CURRENT_SENTENCE_TH", "") ?: ""
        
        updateStreakUI()
        
        if (isTaskCompleted) {
            btnCompleted.isEnabled = false
            btnCompleted.text = "Completed for today"
        } else {
            btnCompleted.isEnabled = true
            btnCompleted.text = "Completed"
        }

        val storedCurrentSentenceEn = sharedPref.getString("CURRENT_SENTENCE_EN", "") ?: ""
        val storedEndTime = sharedPref.getLong("TIMER_END_TIME", 0L)
        val currentTime = System.currentTimeMillis()

        if (storedCurrentSentenceEn.isEmpty() || storedEndTime == 0L || currentTime >= storedEndTime) {
            if (storedEndTime != 0L && currentTime >= storedEndTime) {
                handleDayEndLogic()
            }
            resetToNewPrescript(24 * 60 * 60 * 1000L)
        } else {
            currentSentenceEn = storedCurrentSentenceEn
            currentSentenceTh = sharedPref.getString("CURRENT_SENTENCE_TH", "") ?: ""
            isShowingThai = sharedPref.getBoolean("IS_SHOWING_THAI", false)
            val textToDisplay = if (isShowingThai && currentSentenceTh.isNotEmpty()) currentSentenceTh else currentSentenceEn
            revealText(textToDisplay, textView4)
            startCountdown()
        }

        btnCompleted.setOnClickListener {
            if (!isTaskCompleted) {
                isTaskCompleted = true
                streakCount++
                if (streakCount > highestStreak) {
                    highestStreak = streakCount
                }
                saveStreakData()
                updateStreakUI()
                btnCompleted.isEnabled = false
                btnCompleted.text = "Completed for today"
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prescript Reminders"
            val descriptionText = "Notifications for daily prescript reminders and streak updates."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("prescript_reminder_channel", name, importance).apply {
                description = descriptionText
                setSound(null, null)
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(this, "prescript_reminder_channel")
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(ContextCompat.getSystemService(this, NotificationManager::class.java) as NotificationManager) {
            notify(notificationId, builder.build())
        }
    }

    private fun applyBackgroundTheme() {
        val sharedPref = getPrefs()
        val theme = sharedPref.getString("BACKGROUND_THEME", "Dark")
        
        val tvTime = findViewById<TextView>(R.id.texttime)
        val tvTime2 = findViewById<TextView>(R.id.texttime2)
        val tvTime3 = findViewById<TextView>(R.id.texttime3)
        val tvDailyPrescript = findViewById<TextView>(R.id.textView3)
        val ivTranslate = findViewById<ImageView>(R.id.translate)

        if (theme == "Light") {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            navView.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            navView.itemTextColor = ColorStateList.valueOf(Color.BLACK)
            navView.itemIconTintList = ColorStateList.valueOf(Color.BLACK)

            btnMenu.imageTintList = ColorStateList.valueOf(Color.BLACK)
            ivTranslate.imageTintList = ColorStateList.valueOf(Color.BLACK)

            val lightText = ContextCompat.getColor(this, R.color.light_text)
            textClock.setTextColor(lightText)
            tvTime.setTextColor(lightText)
            tvTime2.setTextColor(lightText)
            tvTime3.setTextColor(lightText)
            textView4.setTextColor(lightText)
            tvDailyPrescript.setTextColor(lightText)
            streakCountText.setTextColor(lightText)
            
            btnCompleted.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_button))
            btnCompleted.setTextColor(Color.BLACK)
        } else {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.my_new_background))
            navView.setBackgroundColor(Color.parseColor("#D91D2538"))
            navView.itemTextColor = ColorStateList.valueOf(Color.WHITE)
            navView.itemIconTintList = ColorStateList.valueOf(Color.WHITE)

            btnMenu.imageTintList = ColorStateList.valueOf(Color.WHITE)
            ivTranslate.imageTintList = ColorStateList.valueOf(Color.WHITE)

            textClock.setTextColor(Color.WHITE)
            tvTime.setTextColor(Color.WHITE)
            tvTime2.setTextColor(Color.WHITE)
            tvTime3.setTextColor(Color.WHITE)
            textView4.setTextColor(Color.WHITE)
            tvDailyPrescript.setTextColor(Color.WHITE)
            streakCountText.setTextColor(Color.WHITE)

            btnCompleted.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_color))
            btnCompleted.setTextColor(Color.WHITE)
        }
    }

    private fun toggleTranslation() {
        if (currentSentenceEn.isEmpty()) return
        if (isShowingThai) {
            revealText(currentSentenceEn, textView4)
            isShowingThai = false
        } else {
            if (currentSentenceTh.isNotEmpty()) {
                revealText(currentSentenceTh, textView4)
                isShowingThai = true
            } else {
                translateToThai()
            }
        }
    }

    private fun startLoadingShuffle(textView: TextView) {
        revealJob?.cancel()
        revealJob = lifecycleScope.launch {
            val length = 25
            var streamId = 0
            if (soundLoaded) streamId = soundPool.play(shuffleSoundId, 1.0f, 1.0f, 1, -1, 1f)
            try {
                while (true) {
                    val displayed = StringBuilder()
                    repeat(length) { displayed.append(shuffleChars.random()) }
                    textView.text = displayed.toString()
                    delay(40)
                }
            } finally {
                if (streamId != 0) soundPool.stop(streamId)
            }
        }
    }

    private fun revealText(targetText: String, textView: TextView, onComplete: () -> Unit = {}) {
        revealJob?.cancel()
        revealJob = lifecycleScope.launch {
            var iteration = 0f
            var streamId = 0
            if (soundLoaded) streamId = soundPool.play(shuffleSoundId, 1.0f, 1.0f, 1, -1, 1.2f)
            try {
                while (iteration < targetText.length) {
                    val displayed = StringBuilder()
                    for (i in targetText.indices) {
                        if (i < iteration) displayed.append(targetText[i])
                        else if (targetText[i] == '\n') displayed.append('\n')
                        else displayed.append(shuffleChars.random())
                    }
                    textView.text = displayed.toString()
                    iteration += 0.6f
                    delay(20)
                }
            } finally {
                if (streamId != 0) soundPool.stop(streamId)
            }
            textView.text = targetText
            onComplete()
        }
    }

    private fun translateToThai() {
        startLoadingShuffle(textView4)
        lifecycleScope.launch {
            try {
                val model = getGenerativeModel()
                val prompt = "Translate this English task to Thai naturally: '$currentSentenceEn'. Output only the Thai translation."
                val response = model.generateContent(prompt)
                val fullTranslation = response.text?.trim() ?: ""
                
                if (fullTranslation.isNotEmpty()) {
                    currentSentenceTh = fullTranslation
                    getPrefs().edit().putString("CURRENT_SENTENCE_TH", currentSentenceTh).apply()
                    revealText(currentSentenceTh, textView4) { isShowingThai = true }
                } else {
                    textView4.text = currentSentenceEn
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during AI translation: ", e)
                textView4.text = currentSentenceEn
            }
        }
    }

    private fun handleDayEndLogic() {
        val sharedPref = getPrefs()
        val currentTime = System.currentTimeMillis()
        val lastEndTime = sharedPref.getLong("TIMER_END_TIME", currentTime)

        if (lastEndTime != 0L && currentTime >= lastEndTime) {
            saveMissionToHistory()
            val wasTaskCompletedPreviously = isTaskCompleted
            if (!wasTaskCompletedPreviously) {
                streakCount = 0
                showNotification("Streak Reset!", "You missed your daily prescript. Your streak has been reset to 0.", 100)
            }
        }
        isTaskCompleted = false 
        saveStreakData()
    }

    private fun resetToNewPrescript(durationMillis: Long) {
        val sharedPref = getPrefs()
        val newEndTime = System.currentTimeMillis() + durationMillis
        sharedPref.edit().putLong("TIMER_END_TIME", newEndTime).apply()

        showNotification("Daily Prescript Reset", "A new daily prescript is available!", 102)
        
        currentSentenceEn = ""
        currentSentenceTh = ""
        isShowingThai = false

        btnCompleted.isEnabled = true
        btnCompleted.text = "Completed"
        updateStreakUI()

        generatePrescriptWithAI()
        startCountdown()
    }

    private fun startCountdown() {
        val sharedPref = getPrefs()
        val endTime = sharedPref.getLong("TIMER_END_TIME", 0L)
        val currentTime = System.currentTimeMillis()

        countDownTimer?.cancel()

        if (endTime == 0L || currentTime >= endTime) {
            if (endTime != 0L) {
                handleDayEndLogic()
            }
            resetToNewPrescript(24 * 60 * 60 * 1000L)
            return
        }
        
        val remainingTime = endTime - currentTime
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                textClock.text = String.format(Locale.getDefault(), "%02d : %02d : %02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                handleDayEndLogic()
                resetToNewPrescript(24 * 60 * 60 * 1000L)
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
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
            putInt("HIGHEST_STREAK", highestStreak)
            putBoolean("IS_TASK_COMPLETED", isTaskCompleted)
            putString("CURRENT_SENTENCE_EN", currentSentenceEn)
            putString("CURRENT_SENTENCE_TH", currentSentenceTh)
            putBoolean("IS_SHOWING_THAI", isShowingThai)
            apply()
        }
    }

    private fun generatePrescriptWithAI() {
        val sharedPref = getPrefs()
        val themesString = sharedPref.getString("SELECTED_THEMES", "General") ?: "General"
        val themesList = themesString.split(",").filter { it.isNotBlank() }
        val selectedTheme = if (themesList.isNotEmpty()) themesList.random() else "General"

        startLoadingShuffle(textView4)
        lifecycleScope.launch {
            try {
                val model = getGenerativeModel()
                val prompt = "Generate a short, positive, and actionable daily task related to $selectedTheme. One sentence only."
                val response = model.generateContent(prompt)
                val fullText = response.text?.trim() ?: ""
                
                if (fullText.isNotEmpty()) {
                    currentSentenceEn = fullText
                    currentSentenceTh = ""
                    isShowingThai = false
                    revealText(currentSentenceEn, textView4) { saveStreakData() }
                } else {
                    useFallback()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during AI generation: ", e)
                useFallback()
            }
        }
    }

    private fun useFallback() {
        currentSentenceEn = fallbackPrescripts.random()
        currentSentenceTh = ""
        isShowingThai = false
        revealText(currentSentenceEn, textView4) { saveStreakData() }
    }

    override fun onResume() {
        super.onResume()
        applyBackgroundTheme()
        if (isTaskCompleted) {
            btnCompleted.isEnabled = false
            btnCompleted.text = "Completed for today"
        } else {
            btnCompleted.isEnabled = true
            btnCompleted.text = "Completed"
        }
        updateStreakUI()
        startCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        soundPool.release()
    }
}