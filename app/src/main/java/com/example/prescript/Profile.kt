package com.example.prescript

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class Profile : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mainLayout: View
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var avatarImage: ShapeableImageView
    private lateinit var usernameValue: TextView
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cameraIcon: ImageView
    private val modelDisplayNames = arrayOf(
        "Gemini 2.5 Flash",
        "Gemini 2.5 Pro",
        "Gemini 3.1 Pro Preview",
        "Gemini 3.1 Flash Lite Preview"
    )
    
    private val modelIds = arrayOf(
        "models/gemini-2.5-flash",
        "models/gemini-2.5-pro",
        "models/gemini-3.1-pro-preview",
        "models/gemini-3.1-flash-lite-preview"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        mainLayout = findViewById(R.id.main)
        navView = findViewById(R.id.nav_view)
        btnMenu = findViewById(R.id.btn_menu)

        avatarImage = findViewById(R.id.avatar_image)
        cameraIcon = findViewById(R.id.imageView6)
        val emailValue = findViewById<TextView>(R.id.tv_email_value)
        usernameValue = findViewById<TextView>(R.id.tv_username_value)
        val btnChangeUsername = findViewById<Button>(R.id.btn_change_username)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnEditTheme = findViewById<Button>(R.id.btn_edit_theme)
        val btnAISettings = findViewById<Button>(R.id.btn_ai_settings)
        val rgBgTheme = findViewById<RadioGroup>(R.id.rg_bg_theme)
        val rbDark = findViewById<RadioButton>(R.id.rb_dark)
        val rbLight = findViewById<RadioButton>(R.id.rb_light)

        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)

        emailValue.text = currentUser?.email ?: "No Email"
        usernameValue.text = sharedPref.getString("USER_NAME", "User")
        
        sharedPref.edit().remove("PROFILE_IMAGE_URI").apply()
        avatarImage.setImageDrawable(null)

        applyBackgroundTheme()

        val currentTheme = sharedPref.getString("BACKGROUND_THEME", "Dark")
        if (currentTheme == "Light") {
            rbLight.isChecked = true
        } else {
            rbDark.isChecked = true
        }

        rgBgTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = if (checkedId == R.id.rb_light) "Light" else "Dark"
            sharedPref.edit().putString("BACKGROUND_THEME", theme).apply()
            applyBackgroundTheme()
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val internalUri = copyUriToInternalStorage(uri)
                if (internalUri != null) {
                    avatarImage.setImageURI(internalUri)
                    sharedPref.edit().putString("PROFILE_IMAGE_URI", internalUri.toString()).apply()
                }
            }
        }

        avatarImage.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        cameraIcon.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnChangeUsername.setOnClickListener {
            showChangeUsernameDialog()
        }

        btnAISettings.setOnClickListener {
            showAISettingsDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnMenu.setOnClickListener {
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

    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, "profile_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyBackgroundTheme() {
        val uid = auth.currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        val theme = sharedPref.getString("BACKGROUND_THEME", "Dark")
        
        val emailValue = findViewById<TextView>(R.id.tv_email_value)
        val usernameValue = findViewById<TextView>(R.id.tv_username_value)
        val emailLabel = findViewById<TextView>(R.id.tv_email_label)
        val usernameLabel = findViewById<TextView>(R.id.tv_username_label)
        val avatarHint = findViewById<TextView>(R.id.tv_avatar_hint)
        val themeLabel = findViewById<TextView>(R.id.tv_theme_label)
        val rbDark = findViewById<RadioButton>(R.id.rb_dark)
        val rbLight = findViewById<RadioButton>(R.id.rb_light)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnChangeUsername = findViewById<Button>(R.id.btn_change_username)

        if (theme == "Light") {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            navView.setBackgroundColor(ContextCompat.getColor(this, R.color.light_background))
            navView.itemTextColor = ColorStateList.valueOf(Color.BLACK)
            navView.itemIconTintList = ColorStateList.valueOf(Color.BLACK)

            btnMenu.imageTintList = ColorStateList.valueOf(Color.BLACK)
            cameraIcon.imageTintList = ColorStateList.valueOf(Color.BLACK)

            val lightText = ContextCompat.getColor(this, R.color.light_text)
            emailValue.setTextColor(lightText)
            usernameValue.setTextColor(lightText)
            emailLabel.setTextColor(lightText)
            usernameLabel.setTextColor(lightText)
            avatarHint.setTextColor(lightText)
            themeLabel.setTextColor(lightText)
            rbDark.setTextColor(lightText)
            rbLight.setTextColor(lightText)
            rbDark.buttonTintList = ColorStateList.valueOf(lightText)
            rbLight.buttonTintList = ColorStateList.valueOf(lightText)
            btnLogout.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_button))
            btnLogout.setTextColor(Color.BLACK)
            btnChangeUsername.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
            btnChangeUsername.setTextColor(Color.WHITE)
        } else {
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.my_new_background))
            navView.setBackgroundColor(Color.parseColor("#D91D2538"))
            navView.itemTextColor = ColorStateList.valueOf(Color.WHITE)
            navView.itemIconTintList = ColorStateList.valueOf(Color.WHITE)

            btnMenu.imageTintList = ColorStateList.valueOf(Color.WHITE)
            cameraIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)

            emailValue.setTextColor(Color.WHITE)
            usernameValue.setTextColor(Color.WHITE)
            emailLabel.setTextColor(Color.WHITE)
            usernameLabel.setTextColor(Color.WHITE)
            avatarHint.setTextColor(Color.WHITE)
            themeLabel.setTextColor(Color.WHITE)
            rbDark.setTextColor(Color.WHITE)
            rbLight.setTextColor(Color.WHITE)
            rbDark.buttonTintList = ColorStateList.valueOf(Color.WHITE)
            rbLight.buttonTintList = ColorStateList.valueOf(Color.WHITE)
            btnLogout.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_color))
            btnLogout.setTextColor(Color.WHITE)
            btnChangeUsername.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            btnChangeUsername.setTextColor(Color.BLACK)
        }
    }

    private fun showAISettingsDialog() {
        val uid = auth.currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("AI Settings")

        val layout = LayoutInflater.from(this).inflate(R.layout.dialog_ai_settings, null)
        val etApiKey = layout.findViewById<EditText>(R.id.et_api_key)
        val spModelName = layout.findViewById<Spinner>(R.id.sp_model_name)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelDisplayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spModelName.adapter = adapter

        val currentModelId = sharedPref.getString("GEMINI_MODEL_NAME", "models/gemini-2.5-flash")
        val currentIndex = modelIds.indexOf(currentModelId).coerceAtLeast(0)
        spModelName.setSelection(currentIndex)

        etApiKey.setText(sharedPref.getString("GEMINI_API_KEY", BuildConfig.GEMINI_API_KEY))

        builder.setView(layout)
        builder.setPositiveButton("Save") { _, _ ->
            val newApiKey = etApiKey.text.toString().trim()
            val selectedModelId = modelIds[spModelName.selectedItemPosition]
            
            if (newApiKey.isNotEmpty()) {
                sharedPref.edit().apply {
                    putString("GEMINI_API_KEY", newApiKey)
                    putString("GEMINI_MODEL_NAME", selectedModelId)
                    apply()
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
