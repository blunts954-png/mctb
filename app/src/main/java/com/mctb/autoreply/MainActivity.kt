package com.mctb.autoreply

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var statusText: TextView
    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var permissionsWarning: MaterialCardView

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionsWarning.visibility = View.GONE
            requestBatteryOptimization()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = AppPreferences(this)

        // Initialize views
        statusText = findViewById(R.id.statusText)
        enableSwitch = findViewById(R.id.enableSwitch)
        permissionsWarning = findViewById(R.id.permissionsWarning)

        val editMessageButton = findViewById<Button>(R.id.editMessageButton)
        val activeHoursButton = findViewById<Button>(R.id.activeHoursButton)
        val usageButton = findViewById<Button>(R.id.usageButton)
        val grantPermissionsButton = findViewById<Button>(R.id.grantPermissionsButton)

        // Set up listeners
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleToggleChange(isChecked)
        }

        editMessageButton.setOnClickListener {
            startActivity(Intent(this, MessageEditorActivity::class.java))
        }

        activeHoursButton.setOnClickListener {
            startActivity(Intent(this, ActiveHoursActivity::class.java))
        }

        usageButton.setOnClickListener {
            startActivity(Intent(this, UsageActivity::class.java))
        }

        grantPermissionsButton.setOnClickListener {
            requestPermissions()
        }

        // Check permissions on start
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        enableSwitch.isChecked = prefs.isEnabled

        statusText.text = if (prefs.isEnabled) {
            getString(R.string.auto_reply_active)
        } else {
            getString(R.string.auto_reply_inactive)
        }
    }

    private fun handleToggleChange(isEnabled: Boolean) {
        if (isEnabled) {
            // Check permissions before enabling
            if (!hasAllPermissions()) {
                enableSwitch.isChecked = false
                requestPermissions()
                return
            }

            // Check if reached limit
            if (prefs.hasReachedLimit()) {
                enableSwitch.isChecked = false
                showLimitReachedDialog()
                return
            }

            // Enable auto-reply
            prefs.isEnabled = true
            CallMonitorService.start(this)
            statusText.text = getString(R.string.auto_reply_active)
        } else {
            // Disable auto-reply
            prefs.isEnabled = false
            CallMonitorService.stop(this)
            statusText.text = getString(R.string.auto_reply_inactive)
        }
    }

    private fun checkPermissions() {
        if (hasAllPermissions()) {
            permissionsWarning.visibility = View.GONE
        } else {
            permissionsWarning.visibility = View.VISIBLE
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Battery optimization settings not available
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_required)
            .setMessage(R.string.permission_rationale)
            .setPositiveButton(R.string.grant_permissions) { _, _ ->
                requestPermissions()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLimitReachedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.limit_reached)
            .setMessage(R.string.limit_message)
            .setPositiveButton(R.string.upgrade_now) { _, _ ->
                startActivity(Intent(this, UsageActivity::class.java))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
