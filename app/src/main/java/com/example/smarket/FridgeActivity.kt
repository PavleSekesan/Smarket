package com.example.smarket

import UserData.FridgeItem
import UserData.getAllFridgeItems
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.adapters.FridgeItemsListAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FridgeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fridge)
        super.bindListenersToTopBar()
        super.removeBackButton()
        super.setTitle(getString(R.string.fridge_activity_title))

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.fridge
        BottomNavigator(this, bottomNavigation)

        val fridgeItemsRecyclerView = findViewById<RecyclerView>(R.id.fridgeItemsRecyclerView)
        val fridgeAddProductButton = findViewById<Button>(R.id.fridgeAddProductButton)

        //val searchView = findViewById<FloatingSearchView>(R.id.floating_search_view)


        fridgeItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        getAllFridgeItems().addOnSuccessListener { allFridgeItems ->
            val fridgeItems = allFridgeItems as List<FridgeItem>
            val adapter = FridgeItemsListAdapter(fridgeItems)
            fridgeItemsRecyclerView.adapter = adapter
            //searchView.setOnQueryChangeListener { oldQuery, newQuery -> adapter.displaySearchedItems(newQuery) }
        }

        findViewById<FloatingActionButton>(R.id.removeBarcodeFab).setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            intent.putExtra("adding",false)
            startActivity(intent)
        }

        fridgeAddProductButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }
    }
}