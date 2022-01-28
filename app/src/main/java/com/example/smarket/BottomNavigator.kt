package com.example.smarket

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.IllegalStateException

// FIXME Should become singleton or static
class BottomNavigator(private val context : Context, bottomNavigationView : BottomNavigationView) {
    init {
        // Maps activity to its top ancestor activity
        val groupMap = mapOf<Class<*>, Class<*>>(
            SelectedBundleActivity::class.java to BundlesListActivity::class.java,
            EditBundleActivity::class.java to BundlesListActivity::class.java
        ).withDefault { it }

        bottomNavigationView.setOnNavigationItemSelectedListener {
            val currentActivity = (context as AppCompatActivity)::class.java
            val currentGroup = groupMap.getValue(currentActivity)
            val clickedActivity : Class<*> =
            when (it.itemId) {
                R.id.fridge -> {
                    FridgeActivity::class.java
                }
                R.id.calendar -> {
                    MainActivity::class.java
                }
                R.id.bundles -> {
                    BundlesListActivity::class.java
                }
                R.id.deliveries -> {
                    DeliveriesActivity::class.java
                }
                else -> { throw IllegalStateException("This activity doesn't exist")}
            }
            if (clickedActivity != currentGroup)
                startActivity(clickedActivity)
            true
        }
    }

    private fun startActivity(activity : Class<*>) {
        val intent = Intent(context, activity)
        context.startActivity(intent)
    }
}