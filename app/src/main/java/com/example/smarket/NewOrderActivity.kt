package com.example.smarket

import ShoppingBundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NewOrderActivity : AppCompatActivity() {

    private var selectedBundles = mutableListOf<ShoppingBundle>()
    private var unselectedBundles = mutableListOf<ShoppingBundle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)
        selectedBundlesList.layoutManager = LinearLayoutManager(this@NewOrderActivity)
        unselectedBundlesList.layoutManager = LinearLayoutManager(this@NewOrderActivity)

        GlobalScope.launch {
            unselectedBundles = UserData.getAllBundles() as MutableList<ShoppingBundle>

            val selectedBundlesAdapter = BundleSelectionAdapter(selectedBundles)
            val unselectedBundlesAdapter = BundleSelectionAdapter(unselectedBundles)
            selectedBundlesAdapter.onSelection = { bundle ->
                unselectedBundles.add(bundle)
                unselectedBundlesAdapter.notifyItemInserted(unselectedBundles.size)
            }
            unselectedBundlesAdapter.onSelection = { bundle ->
                selectedBundles.add(bundle)
                selectedBundlesAdapter.notifyItemInserted(selectedBundles.size)
            }

            runOnUiThread {
                selectedBundlesList.adapter = selectedBundlesAdapter
                unselectedBundlesList.adapter = unselectedBundlesAdapter
            }
        }
    }
}