package com.floatingball.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.floatingball.R

/**
 * Simple settings screen using AndroidX Preference library.
 * All changes are persisted to SharedPreferences and picked up
 * by [com.floatingball.service.FloatingBallService] via OnSharedPreferenceChangeListener.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            // App picker disabled: orientation-based auto-hide is simpler and more reliable.
            // To re-enable, uncomment manifest entry and this click handler.
            // findPreference<androidx.preference.Preference>("auto_hide_apps")?.setOnPreferenceClickListener {
            //     startActivity(android.content.Intent(requireContext(), AppPickerActivity::class.java))
            //     true
            // }
        }
    }
}
