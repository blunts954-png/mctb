package com.mctb.autoreply

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class UsageActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var usageCountText: TextView
    private lateinit var usageProgress: ProgressBar
    private lateinit var upgradeCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = AppPreferences(this)

        // Initialize views
        usageCountText = findViewById(R.id.usageCountText)
        usageProgress = findViewById(R.id.usageProgress)
        upgradeCard = findViewById(R.id.upgradeCard)
        val upgradeButton = findViewById<Button>(R.id.upgradeButton)

        // Upgrade button
        upgradeButton.setOnClickListener {
            handleUpgrade()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (prefs.isUnlimited) {
            // Show unlimited status
            usageCountText.text = getString(R.string.usage_unlimited)
            usageProgress.visibility = View.GONE
            upgradeCard.visibility = View.GONE
        } else {
            // Show current usage
            val count = prefs.autoTextCount
            usageCountText.text = getString(R.string.usage_count, count)
            usageProgress.progress = count
            usageProgress.max = AppPreferences.FREE_TIER_LIMIT

            // Show upgrade card if limit reached
            if (prefs.hasReachedLimit()) {
                upgradeCard.visibility = View.VISIBLE
            } else {
                upgradeCard.visibility = View.GONE
            }
        }
    }

    private fun handleUpgrade() {
        // In a real app, this would trigger in-app purchase
        // For MVP, we'll just unlock unlimited as a placeholder

        // TODO: Implement actual payment flow (Google Play Billing)
        // For now, this is a placeholder that just unlocks the feature

        prefs.isUnlimited = true
        Toast.makeText(this, "Upgraded to unlimited! (Demo mode)", Toast.LENGTH_LONG).show()
        updateUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
