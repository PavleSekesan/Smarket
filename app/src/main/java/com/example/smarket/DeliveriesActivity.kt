package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import UserData.Delivery
import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.adapters.DeliveriesAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class DeliveriesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deliveries)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.deliveries_activity_title))

        val nonconfirmedDeliveriesTextView = findViewById<TextView>(R.id.nonconfirmedDeliveriesLabel)
        val placedDeliveriesTextView = findViewById<TextView>(R.id.placedDeliveriesLabel)
        val nonconfirmedDeliveriesRecyclerView = findViewById<RecyclerView>(R.id.nonconfirmedDeliveriesRecyclerView)
        val placedDeliveriesRecyclerView = findViewById<RecyclerView>(R.id.placedDeliveriesRecyclerView)
        UserData.getAllDeliveries().addOnSuccessListener { delivs ->
            val deliveries = delivs as List<UserData.Delivery>
            val nonconfirmedDeliveries = deliveries.filter { it.status.databaseValue == "waiting confirmation"}
            val placedDeliveries = deliveries.filter { it.status.databaseValue == "placed" }
            nonconfirmedDeliveriesRecyclerView.adapter = DeliveriesAdapter(nonconfirmedDeliveries, true)
            nonconfirmedDeliveriesRecyclerView.layoutManager = LinearLayoutManager(this)
            placedDeliveriesRecyclerView.adapter = DeliveriesAdapter(placedDeliveries, false)
            placedDeliveriesRecyclerView.layoutManager = LinearLayoutManager(this)
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.deliveries
        BottomNavigator(this, bottomNavigation)
    }
}