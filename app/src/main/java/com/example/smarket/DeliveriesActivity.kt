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

            val placedAdapter = DeliveriesAdapter(placedDeliveries, false)
            val nonconfirmedAdapter = DeliveriesAdapter(nonconfirmedDeliveries, true)

            nonconfirmedDeliveriesRecyclerView.adapter = nonconfirmedAdapter
            nonconfirmedDeliveriesRecyclerView.layoutManager = LinearLayoutManager(this)
            placedDeliveriesRecyclerView.adapter = placedAdapter
            placedDeliveriesRecyclerView.layoutManager = LinearLayoutManager(this)
            for (delivery in deliveries)
            {
                delivery.status.addOnChangeListener { newStatus, _ ->
                    if (newStatus == "placed")
                    {
                        placedAdapter.addDelivery(delivery)
                        nonconfirmedAdapter.removeDelivery(delivery)
                    }
                    else if (newStatus == "waiting confirmation")
                    {
                        nonconfirmedAdapter.addDelivery(delivery)
                        placedAdapter.removeDelivery(delivery)
                    }
                    else if (newStatus == "completed")
                    {
                        nonconfirmedAdapter.removeDelivery(delivery)
                        placedAdapter.removeDelivery(delivery)
                    }
                }
            }
            UserData.addOnDeliveryModifyListener { delivery, databaseEventType ->
                if((databaseEventType == UserData.DatabaseEventType.ADDED) && delivery != null)
                {
                    if (delivery.status.databaseValue == "placed")
                    {
                        placedAdapter.addDelivery(delivery)
                        nonconfirmedAdapter.removeDelivery(delivery)
                    }
                    else if (delivery.status.databaseValue == "waiting confirmation")
                    {
                        nonconfirmedAdapter.addDelivery(delivery)
                        placedAdapter.removeDelivery(delivery)
                    }
                    else if (delivery.status.databaseValue == "completed")
                    {
                        nonconfirmedAdapter.removeDelivery(delivery)
                        placedAdapter.removeDelivery(delivery)
                    }
                }
            }
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.deliveries
        BottomNavigator(this, bottomNavigation)
    }
}