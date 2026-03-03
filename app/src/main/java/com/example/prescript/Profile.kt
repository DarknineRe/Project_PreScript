package com.example.prescript

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class Profile : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var avatarImage: ShapeableImageView
    private lateinit var usernameValue: TextView
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        avatarImage = findViewById(R.id.avatar_image)
        val emailValue = findViewById<TextView>(R.id.tv_email_value)
        usernameValue = findViewById<TextView>(R.id.tv_username_value)
        val btnChangeUsername = findViewById<Button>(R.id.btn_change_username)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnEditTheme = findViewById<Button>(R.id.btn_edit_theme)

        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)

        // Set User Data
        emailValue.text = currentUser?.email ?: "No Email"
        usernameValue.text = sharedPref.getString("USER_NAME", "User")
        
        // Load Avatar Image
        val savedImageUri = sharedPref.getString("PROFILE_IMAGE_URI", null)
        if (savedImageUri != null) {
            avatarImage.setImageURI(Uri.parse(savedImageUri))
        }

        // Initialize Photo Picker
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                // Grant persistable permission if possible or just set it
                // For simplicity here, we just set it. 
                // Note: On some devices, URIs from picker might expire after reboot unless persisted.
                avatarImage.setImageURI(uri)
                sharedPref.edit().putString("PROFILE_IMAGE_URI", uri.toString()).apply()
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
            }
        }

        avatarImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnChangeUsername.setOnClickListener {
            showChangeUsernameDialog()
        }

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

    private fun showChangeUsernameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change Username")

        val input = EditText(this)
        input.setText(usernameValue.text)
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                val uid = auth.currentUser?.uid
                val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
                sharedPref.edit().putString("USER_NAME", newName).apply()
                usernameValue.text = newName
                Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}