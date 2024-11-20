package com.jwieczor.otebook_with_password

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextPassword: EditText

    companion object {
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOCKOUT_DURATION = 300000 // 5 minutes in milliseconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        if (!sharedPreferences.contains("password")) {
            val intent = Intent(this, SetPasswordActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        editTextPassword = findViewById(R.id.editTextPassword)
        val buttonLogin: Button = findViewById(R.id.buttonLogin)

        buttonLogin.setOnClickListener { login() }
    }

    private fun login() {
        val password = editTextPassword.text.toString()
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val savedPassword = sharedPreferences.getString("password", null)
        val failedAttempts = sharedPreferences.getInt("failedAttempts", 0)
        val lastAttemptTime = sharedPreferences.getLong("lastAttemptTime", 0)
        val currentTime = System.currentTimeMillis()

        if (failedAttempts >= MAX_LOGIN_ATTEMPTS && currentTime - lastAttemptTime < LOCKOUT_DURATION) {
            Toast.makeText(this, "Konto zablokowane. Spróbuj ponownie później.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password == savedPassword) {
            with(sharedPreferences.edit()) {
                putBoolean("isLoggedIn", true)
                putInt("failedAttempts", 0)
                apply()
            }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            with(sharedPreferences.edit()) {
                putInt("failedAttempts", failedAttempts + 1)
                putLong("lastAttemptTime", currentTime)
                apply()
            }
            Toast.makeText(this, "Nieprawidłowe hasło", Toast.LENGTH_SHORT).show()
        }
    }
}