package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView

class ReviewDeliveryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_delivery)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.deliveries_activity_title))

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.deliveries
        BottomNavigator(this, bottomNavigation)
    }
}