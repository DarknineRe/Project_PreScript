package com.example.prescript

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!, account.displayName)
                } catch (e: ApiException) {
                    Log.e("Register", "Google sign in failed", e)
                    Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val emailLayout = findViewById<TextInputLayout>(R.id.inputemail_layout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.inputpass_layout)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.inputcpass_layout)
        val nameLayout = findViewById<TextInputLayout>(R.id.inputuser_layout)
        
        val emailInput = findViewById<TextInputEditText>(R.id.inputemail)
        val passwordInput = findViewById<TextInputEditText>(R.id.inputpass)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.inputcpass)
        val nameInput = findViewById<TextInputEditText>(R.id.inputuser)
        
        val registerButton = findViewById<Button>(R.id.btnreg2)
        val googleButton = findViewById<Button>(R.id.btngoogle)

        registerButton.setOnClickListener {
            emailLayout.error = null
            passwordLayout.error = null
            confirmPasswordLayout.error = null
            nameLayout.error = null

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val name = nameInput.text.toString().trim()

            var hasError = false
            
            if (email.isEmpty()) { 
                emailLayout.error = "Required"; hasError = true 
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Invalid email format"; hasError = true
            }
            
            if (name.isEmpty()) { nameLayout.error = "Required"; hasError = true }
            
            if (password.isEmpty()) { 
                passwordLayout.error = "Required"; hasError = true 
            } else if (password.length < 6) { 
                passwordLayout.error = "Must be at least 6 characters"; hasError = true 
            }
            
            if (confirmPassword.isEmpty()) { 
                confirmPasswordLayout.error = "Required"; hasError = true 
            } else if (password != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"; hasError = true
            }

            if (hasError) return@setOnClickListener

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        saveDefaultSettings(auth.currentUser?.uid, name)
                        navigateToTheme()
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, displayName: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveDefaultSettings(auth.currentUser?.uid, displayName)
                    navigateToTheme()
                } else {
                    Toast.makeText(this, "Google Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveDefaultSettings(uid: String?, name: String?) {
        if (uid != null) {
            val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                if (!name.isNullOrEmpty()) {
                    putString("USER_NAME", name)
                }
                putString("GEMINI_API_KEY", "AIzaSyCECdl7Nx__P0aSnaQQ4qu7N0MR6ORKojA")
                putString("GEMINI_MODEL_NAME", "models/gemini-2.5-flash")
                apply()
            }
        }
    }

    private fun navigateToTheme() {
        val intent = Intent(this, Theme::class.java)
        startActivity(intent)
        finish()
    }
}