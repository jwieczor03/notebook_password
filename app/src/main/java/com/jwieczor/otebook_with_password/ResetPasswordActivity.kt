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

        if (oldPassword == savedPassword) {
            if (newPassword == confirmPassword) {
                with(sharedPreferences.edit()) {
                    putString("password", newPassword)
                    apply()
                }
                Toast.makeText(this, "Hasło zostało zresetowane", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Hasła nie są zgodne", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Stare hasło jest nieprawidłowe", Toast.LENGTH_SHORT).show()
        }
    }
}