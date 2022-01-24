package com.example.smarket

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.res.ResourcesCompat.getDrawable
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.top_app_bar.*

open class BaseActivity : AppCompatActivity() {
    fun bindListenersToTopBar()
    {
        val topAppBar: MaterialToolbar = findViewById(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    val auth = Firebase.auth
                    auth.signOut()
                    val intent = Intent(this,MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home -> {
                    this.finish()
                    true
                }
                else -> false
            }
        }
    }

    fun removeBackButton() {
        topAppBar.navigationIcon = null
    }

    fun setTitle(title : String) {
        topAppBar.title = title
    }
}