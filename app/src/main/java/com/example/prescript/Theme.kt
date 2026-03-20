package com.example.prescript

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout 
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class Theme : AppCompatActivity() {
    private val selectedThemes = mutableSetOf<String>()
    private lateinit var themeGrid: GridLayout
    private lateinit var mainLayout: ConstraintLayout 
    private lateinit var btnFinish: Button 
    private lateinit var tvTitle: TextView 

    private val themeList = listOf(
        "⚽ Sports", "🥳 Fun", "💀 Dangerous", "🗓 Daily Task",
        "🌙 Project Moon Game", "💪 Work out", "🤡 Prank", "🎲 Random",
        "🎨 Art", "🍳 Cooking", "🧘 Meditation", "🎮 Gaming",
        "📚 Reading", "🎵 Music", "🚫 Hardcore"
    )

    private val themeButtons = mutableListOf<MaterialButton>()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uid: String? get() = auth.currentUser?.uid

    private fun getPrefs() = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_theme)

        mainLayout = findViewById(R.id.main) 
        btnFinish = findViewById(R.id.btn_finish) 
        tvTitle = findViewById(R.id.textView2) 

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        themeGrid = findViewById(R.id.theme_grid)
        
        val sharedPref = getPrefs()
        val savedThemesString = sharedPref.getString("SELECTED_THEMES", "") ?: ""
        if (savedThemesString.isNotEmpty()) {
            selectedThemes.addAll(savedThemesString.split(","))
        }

        setupDynamicButtons()
        applyBackgroundTheme()

        btnFinish.setOnClickListener {
            if (selectedThemes.isEmpty()) {
                Toast.makeText(this, "Please select at least one theme to continue", Toast.LENGTH_SHORT).show()
            } else {
                saveThemesAndFinish()
            }
        }
    }

    private fun setupDynamicButtons() {
        themeList.forEach { themeName ->
            val button = MaterialButton(this).apply {
                text = themeName
                cornerRadius = 20.dpToPx()
                isAllCaps = false
                textSize = 14f
                setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
                
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }
                
                if (themeName.contains("Hardcore", ignoreCase = true)) {
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2, 1f)
                }
                
                layoutParams = params
                
                setOnClickListener {
                    if (selectedThemes.contains(themeName)) {
                        selectedThemes.remove(themeName)
                    } else {
                        selectedThemes.add(themeName)
                    }
                    updateSingleButtonState(this, themeName, getPrefs().getString("BACKGROUND_THEME", "Dark") ?: "Dark")
                }
            }
            
            updateSingleButtonState(button, themeName, getPrefs().getString("BACKGROUND_THEME", "Dark") ?: "Dark")
            themeGrid.addView(button)
            themeButtons.add(button)
        }
    }

    private fun updateSingleButtonState(button: MaterialButton, themeName: String, currentAppTheme: String) {
        val isSelected = selectedThemes.contains(themeName)
        val selectedBgColorRes: Int
        val selectedTextColorRes: Int
        val unselectedBgColorRes: Int
        val unselectedTextColorRes: Int

        if (currentAppTheme == "Light") {
            selectedBgColorRes = R.color.light_button
            selectedTextColorRes = R.color.light_text
            unselectedBgColorRes = R.color.google_button_color 
            unselectedTextColorRes = R.color.hint_color
        } else {
            selectedBgColorRes = R.color.hint_color
            selectedTextColorRes = R.color.white
            unselectedBgColorRes = R.color.button_color 
            unselectedTextColorRes = R.color.white
        }

        if (isSelected) {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, selectedBgColorRes))
            button.setTextColor(ContextCompat.getColor(this, selectedTextColorRes)) 
        } else {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, unselectedBgColorRes))
            button.setTextColor(ContextCompat.getColor(this, unselectedTextColorRes)) 
        }
    }

    private fun applyBackgroundTheme() {
        val sharedPref = getPrefs()
        val theme = sharedPref.getString("BACKGROUND_THEME", "Dark")

        if (theme == "Light") {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.light_text))
            btnFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_button))
            btnFinish.setTextColor(ContextCompat.getColor(this, R.color.light_text))
        } else {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.my_new_background))
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_color))
            btnFinish.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        themeButtons.forEach { button ->
            updateSingleButtonState(button, button.text.toString(), theme ?: "Dark")
        }
    }

    override fun onResume() {
        super.onResume()
        applyBackgroundTheme()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun saveThemesAndFinish() {
        val sharedPref = getPrefs()
        with(sharedPref.edit()) {
            putString("SELECTED_THEMES", selectedThemes.joinToString(","))
            apply()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}