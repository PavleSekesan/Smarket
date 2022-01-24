package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        setContentView(R.layout.activity_settings)

        super.bindListenersToTopBar()
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        BottomNavigator(this, bottomNavigation)
    }
}