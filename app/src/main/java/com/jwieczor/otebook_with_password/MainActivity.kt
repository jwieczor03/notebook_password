package com.jwieczor.otebook_with_password

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException
class MainActivity : AppCompatActivity() {

    private val MAX_LOGIN_ATTEMPTS = 5
    private val LOCKOUT_DURATION = 300000

    private lateinit var editTextNote: EditText
    private var isNavigatingToResetPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        editTextNote = findViewById(R.id.editTextNote)
        val buttonSave: Button = findViewById(R.id.buttonSave)
        val buttonShow: Button = findViewById(R.id.buttonShow)
        val buttonEdit: Button = findViewById(R.id.buttonEdit)
        val buttonDelete: Button = findViewById(R.id.buttonDelete)
        val buttonLogout: Button = findViewById(R.id.buttonLogout)

        buttonSave.setOnClickListener { saveNote() }
        buttonShow.setOnClickListener { showNote() }
        buttonEdit.setOnClickListener { editNote() }
        buttonDelete.setOnClickListener { deleteNote() }

        buttonLogout.setOnClickListener { logout() }

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (!isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        isNavigatingToResetPassword = false
    }

    override fun onStop() {
        super.onStop()
        if (!isNavigatingToResetPassword) {
            logout()
        }
    }

    private fun isLoggedIn(): Boolean {
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
        val failedAttempts = sharedPreferences.getInt("failedAttempts", 0)
        val lastAttemptTime = sharedPreferences.getLong("lastAttemptTime", 0)
        val currentTime = System.currentTimeMillis()

        if (failedAttempts >= MAX_LOGIN_ATTEMPTS && currentTime - lastAttemptTime < LOCKOUT_DURATION) {
            return false
        }

        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun logout() {
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
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", false)
            putInt("failedAttempts", 0)
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun editNote() {
        val file = File(filesDir, "notatka.txt")
        if (file.exists()) {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedFile = EncryptedFile.Builder(
                this,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            try {
                val note = encryptedFile.openFileInput().bufferedReader().use { it.readText() }
                val editText = EditText(this)
                editText.setText(note)

                AlertDialog.Builder(this)
                    .setTitle("Edytuj notatkę")
                    .setView(editText)
                    .setPositiveButton("Zapisz") { dialog, _ ->
                        val newNote = editText.text.toString()
                        if (newNote.isNotEmpty()) {
                            if (file.delete()) {
                                val newEncryptedFile = EncryptedFile.Builder(
                                    this,
                                    file,
                                    masterKey,
                                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                                ).build()
                                newEncryptedFile.openFileOutput().use { output ->
                                    output.write(newNote.toByteArray())
                                }
                                Toast.makeText(this, "Notatka zapisana!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Błąd podczas usuwania starej notatki.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Notatka nie może być pusta.", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Anuluj") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } catch (e: IOException) {
                Toast.makeText(this, "Błąd odczytu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Brak zapisanej notatki do edycji.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteNote() {
        val file = File(filesDir, "notatka.txt")
        if (file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "Notatka usunięta.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Błąd podczas usuwania notatki.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Brak zapisanej notatki do usunięcia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNote() {
        val note = editTextNote.text.toString()
        if (note.isNotEmpty()) {
            val file = File(filesDir, "notatka.txt")
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedFile = EncryptedFile.Builder(
                this,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            try {
                encryptedFile.openFileOutput().use { output ->
                    output.write(note.toByteArray())
                }
                Toast.makeText(this, "Notatka zapisana!", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Błąd zapisu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Notatka nie może być pusta.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNote() {
        val file = File(filesDir, "notatka.txt")
        if (file.exists()) {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedFile = EncryptedFile.Builder(
                this,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            try {
                val note = encryptedFile.openFileInput().bufferedReader().use { it.readText() }
                AlertDialog.Builder(this)
                    .setTitle("Twoja notatka")
                    .setMessage(note)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: IOException) {
                Toast.makeText(this, "Błąd odczytu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Brak zapisanej notatki.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }
}