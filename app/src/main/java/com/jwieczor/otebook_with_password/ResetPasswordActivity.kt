package com.jwieczor.otebook_with_password

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var editTextOldPassword: EditText
    private lateinit var editTextNewPassword: EditText
    private lateinit var editTextConfirmPassword: EditText

    private val weakPasswords = listOf(
        "123456", "password", "123456789", "12345678", "12345", "1234567", "1234567890", "qwerty", "abc123", "password1"
    )

    private val PASSWORD_CHANGE_INTERVAL = 60 * 60 * 24 * 60 * 1000L // 2 months in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        editTextOldPassword = findViewById(R.id.editTextOldPassword)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        val buttonResetPassword: Button = findViewById(R.id.buttonResetPassword)

        buttonResetPassword.setOnClickListener { resetPassword() }
    }

    private fun resetPassword() {
        val oldPassword = editTextOldPassword.text.toString()
        val newPassword = editTextNewPassword.text.toString()
        val confirmPassword = editTextConfirmPassword.text.toString()

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
        val lastPasswordChangeTime = sharedPreferences.getLong("lastPasswordChangeTime", 0)
        val currentTime = System.currentTimeMillis()

        if (oldPassword == savedPassword) {
            if (newPassword == confirmPassword) {
                if (isPasswordStrong(newPassword)) {
                    if (newPassword != savedPassword) {
                        if (currentTime - lastPasswordChangeTime >= PASSWORD_CHANGE_INTERVAL) {
                            with(sharedPreferences.edit()) {
                                putString("password", newPassword)
                                putLong("lastPasswordChangeTime", currentTime)
                                apply()
                            }
                            Toast.makeText(this, "Hasło zostało zresetowane", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Hasło można zmieniać co dwa miesiące", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Nowe hassło nie może być takie samo jak poprzednie", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Nowe hasło jest zbyt słabe", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Hasła nie są zgodne", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Stare hasło jest nieprawidłowe", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/`~".contains(it) }) return false
        if (weakPasswords.contains(password)) return false
        return true
    }
}