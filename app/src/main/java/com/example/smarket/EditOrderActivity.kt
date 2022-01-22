package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import UserData.getAllUserOrders
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class EditOrderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_order)
        val selectedOrderId = intent.getStringExtra("selected_order_id")

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.calendar
        BottomNavigator(this, bottomNavigation)

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)
        selectedBundlesList.layoutManager = LinearLayoutManager(this)
        unselectedBundlesList.layoutManager = LinearLayoutManager(this)

        var selectedBundles = mutableListOf<ShoppingBundle>()
        var unselectedBundles = mutableListOf<ShoppingBundle>()
        getAllUserOrders().addOnSuccessListener { ret1->
            val allUserOrders = ret1 as List<UserData.UserOrder>
            val selectedOrder = allUserOrders.filter { order-> order.id == selectedOrderId }[0]

            getAllBundles().addOnSuccessListener { ret2 ->
                val allBundles = ret2 as MutableList<ShoppingBundle>

                selectedBundles = selectedOrder.bundles.toMutableList()
                val selectedBundlesAdapter = BundleSelectionAdapter(selectedBundles)
                selectedBundlesList.adapter = selectedBundlesAdapter
                unselectedBundles = allBundles.filter { bundle -> selectedBundles.none { selectedBundle -> selectedBundle.id == bundle.id } }.toMutableList()
                val unselectedBundlesAdapter = BundleSelectionAdapter(unselectedBundles)
                unselectedBundlesList.adapter = unselectedBundlesAdapter

                selectedOrder.addOnSubitemChangeListener { databaseItem, databaseEventType ->
                    val bundleChanged = databaseItem as ShoppingBundle
                    if (databaseEventType == UserData.DatabaseEventType.ADDED)
                    {
                        selectedBundlesAdapter.addBundle(bundleChanged)
                        unselectedBundlesAdapter.removeBundle(bundleChanged)
                    }
                    else if(databaseEventType == UserData.DatabaseEventType.REMOVED)
                    {
                        selectedBundlesAdapter.removeBundle(bundleChanged)
                        unselectedBundlesAdapter.addBundle(bundleChanged)
                    }
                }

                selectedBundlesAdapter.onSelection = { bundle ->
                    selectedOrder.removeBundle(bundle).addOnSuccessListener {
//                        unselectedBundles.add(bundle)
//                        unselectedBundlesAdapter.notifyItemInserted(unselectedBundles.size)
                    }
                }
                unselectedBundlesAdapter.onSelection = { bundle ->
                    selectedOrder.addBunlde(bundle).addOnSuccessListener {
//                        selectedBundles.add(bundle)
//                        selectedBundlesAdapter.notifyItemInserted(selectedBundles.size)
                    }
                }
            }
        }
    }
}