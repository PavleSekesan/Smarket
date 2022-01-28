package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.adapters.LogDeliveryAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LogDeliveryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_delivery)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.log_delivery_activity_title))

        val acceptCurrentFab = findViewById<FloatingActionButton>(R.id.acceptCurrentFab)
        val itemsToConfirmRecyclerView = findViewById<RecyclerView>(R.id.itemsToConfirmRecyclerView)



        UserData.getAllDeliveries().addOnSuccessListener { allDeliveries ->
            val delivery = allDeliveries.find { it.id == intent.getStringExtra("delivery_id") }!! as UserData.Delivery
            val items = delivery.orderedItems as List<UserData.QuantityItem>
            val adapter = LogDeliveryAdapter(items) {
                // TODO Set delivery status to completed
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