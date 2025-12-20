package com.mctb.autoreply

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MessageEditorActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var messageInput: TextInputEditText
    private lateinit var characterCounter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_editor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = AppPreferences(this)

        // Initialize views
        messageInput = findViewById(R.id.messageInput)
        characterCounter = findViewById(R.id.characterCounter)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Load current message
        messageInput.setText(prefs.message)
        updateCharacterCount()

        // Set up character counter
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCharacterCount()
            }
        })

        // Reset button
        resetButton.setOnClickListener {
            messageInput.setText(AppPreferences.DEFAULT_MESSAGE)
        }

        // Save button
        saveButton.setOnClickListener {
            saveMessage()
        }
    }

    private fun updateCharacterCount() {
        val length = messageInput.text?.length ?: 0
        characterCounter.text = getString(R.string.character_count, length)
    }

    private fun saveMessage() {
        val message = messageInput.text?.toString()?.trim() ?: ""

        if (message.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.message = message
        Toast.makeText(this, R.string.message_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
