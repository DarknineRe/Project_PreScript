package com.example.prescript

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.card.MaterialCardView

class History : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView
    private lateinit var textView5: TextView
    private lateinit var tvHighestStreak: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvIncompletedCount: TextView
    private lateinit var btnBack: Button
    private lateinit var cardCompletedCount: MaterialCardView
    private lateinit var cardIncompletedCount: MaterialCardView

    private lateinit var historyAdapter: HistoryAdapter
    private var allHistoryList: List<HistoryItem> = emptyList()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uid: String? get() = auth.currentUser?.uid
    private fun getPrefs() = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        drawerLayout = findViewById(R.id.drawer_layout)
        mainLayout = findViewById(R.id.main)
        navView = findViewById(R.id.nav_view)
        btnMenu = findViewById(R.id.btn_menu)
        textView5 = findViewById(R.id.textView5)
        tvHighestStreak = findViewById(R.id.tv_highest_streak)
        tvCompletedCount = findViewById(R.id.tv_completed_count)
        tvIncompletedCount = findViewById(R.id.tv_incompleted_count)
        btnBack = findViewById(R.id.btn_back)
        cardCompletedCount = findViewById(R.id.card_completed_count)
        cardIncompletedCount = findViewById(R.id.card_incompleted_count)

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
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_history -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val recyclerView = findViewById<RecyclerView>(R.id.history_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        allHistoryList = loadHistory()
        val currentTheme = getPrefs().getString("BACKGROUND_THEME", "Dark") ?: "Dark"
        historyAdapter = HistoryAdapter(allHistoryList.reversed(), currentTheme)
        recyclerView.adapter = historyAdapter

        btnBack.setOnClickListener { finish() }
        
        cardCompletedCount.setOnClickListener { filterHistory(true) }
        cardIncompletedCount.setOnClickListener { filterHistory(false) }

        applyBackgroundTheme()
        updateStreakAndCountsUI(allHistoryList)
        filterHistory(null)
    }

    override fun onResume() {
        super.onResume()
        allHistoryList = loadHistory()
        applyBackgroundTheme()
        val currentTheme = getPrefs().getString("BACKGROUND_THEME", "Dark") ?: "Dark"
        historyAdapter.updateTheme(currentTheme)
        updateStreakAndCountsUI(allHistoryList)
        filterHistory(null)
    }

    private fun applyBackgroundTheme() {
        val sharedPref = getPrefs()
        val theme = sharedPref.getString("BACKGROUND_THEME", "Dark")

        if (theme == "Light") {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            navView.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            navView.itemTextColor = ColorStateList.valueOf(Color.BLACK)
            navView.itemIconTintList = ColorStateList.valueOf(Color.BLACK)
            btnMenu.imageTintList = ColorStateList.valueOf(Color.BLACK)
            
            val lightText = ContextCompat.getColor(this, R.color.light_text)
            textView5.setTextColor(lightText)
            tvHighestStreak.setTextColor(lightText)
            
            cardCompletedCount.setCardBackgroundColor(ContextCompat.getColor(this, R.color.history_card_background_light))
            tvCompletedCount.setTextColor(ContextCompat.getColor(this, R.color.good_count_light))
            cardIncompletedCount.setCardBackgroundColor(ContextCompat.getColor(this, R.color.history_card_background_light))
            tvIncompletedCount.setTextColor(ContextCompat.getColor(this, R.color.bad_count_light))

            btnBack.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_button))
            btnBack.setTextColor(Color.BLACK)
        } else {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.my_new_background))
            navView.setBackgroundColor(Color.parseColor("#D91D2538"))
            navView.itemTextColor = ColorStateList.valueOf(Color.WHITE)
            navView.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
            btnMenu.imageTintList = ColorStateList.valueOf(Color.WHITE)

            textView5.setTextColor(Color.WHITE)
            tvHighestStreak.setTextColor(Color.WHITE)
            
            cardCompletedCount.setCardBackgroundColor(ContextCompat.getColor(this, R.color.history_card_background_dark))
            tvCompletedCount.setTextColor(ContextCompat.getColor(this, R.color.good_count))
            cardIncompletedCount.setCardBackgroundColor(ContextCompat.getColor(this, R.color.history_card_background_dark))
            tvIncompletedCount.setTextColor(ContextCompat.getColor(this, R.color.bad_count))

            btnBack.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_color))
            btnBack.setTextColor(Color.WHITE)
        }
    }

    private fun loadHistory(): List<HistoryItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        val historyJson = sharedPref.getString("MISSION_HISTORY", null)
        return if (historyJson != null) {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            Gson().fromJson(historyJson, type)
        } else {
            emptyList()
        }
    }

    private fun updateStreakAndCountsUI(historyList: List<HistoryItem>) {
        val sharedPref = getPrefs()
        val highestStreak = sharedPref.getInt("HIGHEST_STREAK", 0)
        tvHighestStreak.text = "Highest Streak: $highestStreak"

        var completedCount = 0
        var incompletedCount = 0
        for (item in historyList) {
            if (item.isCompleted) completedCount++ else incompletedCount++
        }
        tvCompletedCount.text = "Completed: $completedCount"
        tvIncompletedCount.text = "Incompleted: $incompletedCount"
    }

    private fun filterHistory(isCompleted: Boolean?) {
        val filteredList = when (isCompleted) {
            true -> allHistoryList.filter { it.isCompleted }
            false -> allHistoryList.filter { !it.isCompleted }
            null -> allHistoryList
        }
        historyAdapter.updateData(filteredList.reversed())
    }
}