package com.arkdev.a3dmodelapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arkdev.a3dmodelapp.activities.UserActivity
import com.arkdev.a3dmodelapp.auth.AuthManager
import com.arkdev.a3dmodelapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        authManager = AuthManager(this)

        if (authManager.isLoggedIn()) {
            navigateToHome()
            return
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Please fill all fields")
                return@setOnClickListener
            }

            if (authManager.login(username, password)) {
                navigateToHome()
            } else {
                showError("Invalid credentials")
            }
        }
    }
    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun navigateToHome() {
        val intent = when (authManager.getRole()) {
            AuthManager.Companion.ROLE_ADMIN -> Intent(this, AdminActivity::class.java)
            else -> Intent(this, UserActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}