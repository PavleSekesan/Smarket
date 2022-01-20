package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NewOrderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)
        selectedBundlesList.layoutManager = LinearLayoutManager(this)
        unselectedBundlesList.layoutManager = LinearLayoutManager(this)

        var selectedBundles = mutableListOf<ShoppingBundle>()
        val selectedBundlesAdapter = BundleSelectionAdapter(selectedBundles)
        selectedBundlesList.adapter = selectedBundlesAdapter
        getAllBundles().addOnSuccessListener { allBundles ->
            val bundles = allBundles as MutableList<ShoppingBundle>
            var unselectedBundles = bundles
            val unselectedBundlesAdapter = BundleSelectionAdapter(unselectedBundles)
            unselectedBundlesList.adapter = unselectedBundlesAdapter
            selectedBundlesAdapter.onSelection = { bundle ->
                unselectedBundles.add(bundle)
                unselectedBundlesAdapter.notifyItemInserted(unselectedBundles.size)
            }
            unselectedBundlesAdapter.onSelection = { bundle ->
                selectedBundles.add(bundle)
                selectedBundlesAdapter.notifyItemInserted(selectedBundles.size)
            }
        }
    }
}