package com.example.prescript

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class Profile : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        val emailValue = findViewById<TextView>(R.id.tv_email_value)
        val usernameValue = findViewById<TextView>(R.id.tv_username_value)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnEditTheme = findViewById<Button>(R.id.btn_edit_theme)

        // Set User Data
        val currentUser = auth.currentUser
        emailValue.text = currentUser?.email ?: "No Email"
        
        // Load username from SharedPreferences (using unique user preferences)
        val uid = currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        val username = sharedPref.getString("USER_NAME", "User")
        usernameValue.text = username

        // FIX: Apply insets to the inner layout 'main' to prevent status bar overlap
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnEditTheme.setOnClickListener {
            startActivity(Intent(this, Theme::class.java))
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, History::class.java))
                    finish()
                }
                R.id.nav_profile -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}