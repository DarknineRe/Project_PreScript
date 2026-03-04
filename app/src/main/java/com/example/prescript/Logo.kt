package com.example.prescript

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Logo : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        goToLogin()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_logo)
        
        val mainView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        val logoImageView = findViewById<ImageView>(R.id.imageView)
        
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fade in and then fade out animation
        val fadeIn = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f)
        fadeIn.duration = 1500
        
        val fadeOut = ObjectAnimator.ofFloat(logoImageView, "alpha", 1f, 0f)
        fadeOut.duration = 1500
        fadeOut.startDelay = 1500 // Start fade out after fade in finishes
        
        fadeIn.start()
        fadeOut.start()

        // Delay for 3 seconds to match animation duration (1.5s in + 1.5s out)
        handler.postDelayed(runnable, 3000)

        // Skip on click
        mainView.setOnClickListener {
            handler.removeCallbacks(runnable)
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}