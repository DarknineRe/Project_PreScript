package com.example.prescript

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

class History : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

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
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_history -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
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

        val historyList = loadHistory()
        recyclerView.adapter = HistoryAdapter(historyList.reversed())

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
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
}