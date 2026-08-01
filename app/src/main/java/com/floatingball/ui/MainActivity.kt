package com.floatingball.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.floatingball.R
import com.floatingball.service.FloatingBallService
import com.floatingball.util.HyperOSGuideHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Setup/onboarding screen. Guides the user to enable accessibility service,
 * grant overlay permission, and configure HyperOS-specific settings.
 *
 * After setup, the floating ball is managed entirely by [FloatingBallService].
 * This activity is excluded from recents so users won't accidentally kill the service.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<android.widget.TextView>(R.id.status_text)
        val btnAccessibility = findViewById<MaterialButton>(R.id.btn_accessibility)
        val btnOverlay = findViewById<MaterialButton>(R.id.btn_overlay)
        val btnBattery = findViewById<MaterialButton>(R.id.btn_battery)
        val btnAutostart = findViewById<MaterialButton>(R.id.btn_autostart)
        val btnSettings = findViewById<MaterialButton>(R.id.btn_settings)
        val cardHyperOS = findViewById<MaterialCardView>(R.id.card_hyperos_guide)

        // Show HyperOS-specific guide on Xiaomi devices
        if (HyperOSGuideHelper.isXiaomiDevice()) {
            cardHyperOS.visibility = android.view.View.VISIBLE
            btnAutostart.visibility = android.view.View.VISIBLE
        }

        // Buttons
        btnAccessibility.setOnClickListener {
            HyperOSGuideHelper.openAccessibilitySettings(this)
        }

        btnOverlay.setOnClickListener {
            HyperOSGuideHelper.openOverlaySettings(this)
        }

        btnBattery.setOnClickListener {
            HyperOSGuideHelper.openBatteryOptimizationSettings(this)
        }

        btnAutostart.setOnClickListener {
            HyperOSGuideHelper.openAutostartSettings(this)
        }

        // Notification access button
        val btnNotification = findViewById<MaterialButton>(R.id.btn_notification)
        btnNotification.setOnClickListener {
            HyperOSGuideHelper.openNotificationAccessSettings(this)
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val statusText = findViewById<android.widget.TextView>(R.id.status_text)
        val isRunning = FloatingBallService.isServiceEnabled(this)
        statusText.text = if (isRunning) {
            getString(R.string.service_running)
        } else {
            getString(R.string.service_stopped)
        }
    }
}
