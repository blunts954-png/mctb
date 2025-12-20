package com.mctb.autoreply

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class ActiveHoursActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var alwaysOnSwitch: SwitchMaterial
    private lateinit var timeSelectionContainer: LinearLayout
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button

    private var startHour: Int = 0
    private var startMinute: Int = 0
    private var endHour: Int = 0
    private var endMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_hours)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = AppPreferences(this)

        // Initialize views
        alwaysOnSwitch = findViewById(R.id.alwaysOnSwitch)
        timeSelectionContainer = findViewById(R.id.timeSelectionContainer)
        startTimeButton = findViewById(R.id.startTimeButton)
        endTimeButton = findViewById(R.id.endTimeButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Load current settings
        loadSettings()

        // Always On switch
        alwaysOnSwitch.setOnCheckedChangeListener { _, isChecked ->
            timeSelectionContainer.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Start time picker
        startTimeButton.setOnClickListener {
            showTimePicker(startHour, startMinute) { hour, minute ->
                startHour = hour
                startMinute = minute
                updateStartTimeButton()
            }
        }

        // End time picker
        endTimeButton.setOnClickListener {
            showTimePicker(endHour, endMinute) { hour, minute ->
                endHour = hour
                endMinute = minute
                updateEndTimeButton()
            }
        }

        // Save button
        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        alwaysOnSwitch.isChecked = prefs.isAlwaysOn
        timeSelectionContainer.visibility = if (prefs.isAlwaysOn) View.GONE else View.VISIBLE

        startHour = prefs.startHour
        startMinute = prefs.startMinute
        endHour = prefs.endHour
        endMinute = prefs.endMinute

        updateStartTimeButton()
        updateEndTimeButton()
    }

    private fun showTimePicker(hour: Int, minute: Int, callback: (Int, Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                callback(selectedHour, selectedMinute)
            },
            hour,
            minute,
            true // 24-hour format
        ).show()
    }

    private fun updateStartTimeButton() {
        startTimeButton.text = String.format("%02d:%02d", startHour, startMinute)
    }

    private fun updateEndTimeButton() {
        endTimeButton.text = String.format("%02d:%02d", endHour, endMinute)
    }

    private fun saveSettings() {
        prefs.isAlwaysOn = alwaysOnSwitch.isChecked

        if (!alwaysOnSwitch.isChecked) {
            prefs.startHour = startHour
            prefs.startMinute = startMinute
            prefs.endHour = endHour
            prefs.endMinute = endMinute
        }

        Toast.makeText(this, R.string.active_hours_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
