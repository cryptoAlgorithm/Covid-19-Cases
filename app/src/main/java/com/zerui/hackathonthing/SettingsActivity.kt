package com.zerui.hackathonthing

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        when (PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            "theme",
            "light"
        )) {
            "black" -> {
                setTheme(R.style.settingsDark)
            }
            "dark" -> {
                setTheme(R.style.settingsGrey)
            }
            else -> {
                setTheme(R.style.settingsLight)
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.settingsToolbar))
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SearchSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.search_preferences, rootKey)
        }
    }

    class appearanceSettingsFragment : PreferenceFragmentCompat() {
        private fun promptRestart() {
            Snackbar.make(
                requireView(),
                "Please restart app for changes to take effect",
                Snackbar.LENGTH_LONG
            ).show()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.appearance_preferences, rootKey)

            findPreference<EditTextPreference>("radius")?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

            findPreference<EditTextPreference>("radius")?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val value = newValue.toString().toULong()
                    if (value in 0u..50u) {
                        promptRestart()
                        true
                    } else {
                        // Invalid value
                        Snackbar.make(
                            requireView(),
                            "Radius must be in the range 0-50",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        false // Don't save preference
                    }
                }

            findPreference<ListPreference>("theme")?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    promptRestart()
                    true
                }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("preferencesDump")?.setOnPreferenceClickListener {
                Log.d("PreferenceDump", PreferenceManager.getDefaultSharedPreferences(requireContext()).all.toString().replace("=", ":"))
                return@setOnPreferenceClickListener true
            }
        }
    }
}