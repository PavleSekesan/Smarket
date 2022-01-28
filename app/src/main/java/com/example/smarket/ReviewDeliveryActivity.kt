package com.example.smarket

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.adapters.ReviewDeliveryAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ReviewDeliveryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_delivery)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.log_delivery_activity_title))

        val acceptCurrentFab = findViewById<FloatingActionButton>(R.id.acceptCurrentFab)
        val itemsToConfirmRecyclerView = findViewById<RecyclerView>(R.id.itemsToConfirmRecyclerView)

        UserData.getAllDeliveries().addOnSuccessListener { allDeliveries ->
            val delivery = allDeliveries.find { it.id == intent.getStringExtra("delivery_id") }!! as UserData.Delivery
            val items = delivery.deliveryItems as List<UserData.DeliveryItem>
            val editable = delivery.status.databaseValue == "waiting confirmation"
            val adapter = ReviewDeliveryAdapter(items, editable) {
                if (editable) {
                    delivery.status.databaseValue = "completed"
                }
                finish()
            }
            itemsToConfirmRecyclerView.adapter = adapter
            itemsToConfirmRecyclerView.layoutManager = LinearLayoutManager(this)
            acceptCurrentFab.setOnClickListener {
                adapter.moveOver()
            }
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.deliveries
        BottomNavigator(this, bottomNavigation)
    }
}