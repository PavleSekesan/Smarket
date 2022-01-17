package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NewOrderActivity : AppCompatActivity() {

    private var selectedBundles = mutableListOf<ShoppingBundle>()
    private var unselectedBundles = mutableListOf<ShoppingBundle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        unselectedBundles = mutableListOf(ShoppingBundle("Rizoto sa pecurkama", listOf(ShoppingItem("Rizoto", "12345", 1, "kom."), ShoppingItem("pecurka", "12345", 1, "kom."))), ShoppingBundle("kuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuurac", listOf(ShoppingItem("kurac1", "12345", 2, "kom."), ShoppingItem("kurac2","12345", 2, "kom."), ShoppingItem("kurac3","12345", 5, "kom."))))

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)

        var selectedBundlesAdapter = BundleSelectionAdapter(selectedBundles)
        var unselectedBundlesAdapter = BundleSelectionAdapter(unselectedBundles)
        selectedBundlesAdapter.onSelection = { bundle ->
            unselectedBundles.add(bundle)
            unselectedBundlesAdapter.notifyItemInserted(unselectedBundles.size)
        }
        unselectedBundlesAdapter.onSelection = { bundle ->
            selectedBundles.add(bundle)
            selectedBundlesAdapter.notifyItemInserted(selectedBundles.size)
        }

        selectedBundlesList.adapter = selectedBundlesAdapter
        unselectedBundlesList.adapter = unselectedBundlesAdapter
        selectedBundlesList.layoutManager = LinearLayoutManager(this)
        unselectedBundlesList.layoutManager = LinearLayoutManager(this)

    }
}