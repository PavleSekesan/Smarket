package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.MultiSelectListPreference

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        setContentView(R.layout.activity_settings)
    }
}