package com.example.smarket

import UserData.ShoppingBundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.MainActivity.Companion.bundles
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NewOrderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        //var bundles = Gson().fromJson<MutableList<ShoppingBundle>>(intent.getStringExtra("bundles"), ShoppingBundle::class.java)

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)
        selectedBundlesList.layoutManager = LinearLayoutManager(this@NewOrderActivity)
        unselectedBundlesList.layoutManager = LinearLayoutManager(this@NewOrderActivity)

        var selectedBundles = mutableListOf<ShoppingBundle>()
        var unselectedBundles = bundles

        val selectedBundlesAdapter = BundleSelectionAdapter(selectedBundles)
        val unselectedBundlesAdapter = BundleSelectionAdapter(unselectedBundles)
        selectedBundlesList.adapter = selectedBundlesAdapter
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