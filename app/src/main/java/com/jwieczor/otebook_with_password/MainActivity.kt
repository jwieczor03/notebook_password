package com.jwieczor.otebook_with_password

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
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

    private lateinit var editTextNote: EditText

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
        val buttonResetPassword: Button = findViewById(R.id.buttonResetPassword)
        buttonResetPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

        checkPermissions()
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
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        logout()
    }

    override fun onStop() {
        super.onStop()
        //logout()
    }

    private fun editNote() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "notatka.txt")
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
                editTextNote.setText(note)
                Toast.makeText(this, "Notatka załadowana do edycji.", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Błąd odczytu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Brak zapisanej notatki do edycji.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteNote() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "notatka.txt")
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
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "notatka.txt")
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
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "notatka.txt")
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