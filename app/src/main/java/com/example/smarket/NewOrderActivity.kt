package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.FieldPosition

class NewOrderActivity : BundleSelectionInterface, AppCompatActivity() {

    private var selectedBundles = mutableListOf<ShoppingBundle>()
    private var unselectedBundles = mutableListOf<ShoppingBundle>()
    private var selectedBundlesAdapter = BundleSelectionAdapter(this, selectedBundles, false)
    private var unselectedBundlesAdapter = BundleSelectionAdapter(this, unselectedBundles, true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        unselectedBundles = mutableListOf(ShoppingBundle("Rizoto sa pecurkama", listOf(ShoppingItem("Rizoto", 1, "kom."), ShoppingItem("pecurka", 1, "kom."))), ShoppingBundle("kuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuurac", listOf(ShoppingItem("kurac1", 2, "kom."), ShoppingItem("kurac2", 2, "kom."), ShoppingItem("kurac3", 5, "kom."))))

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)

        selectedBundlesAdapter = BundleSelectionAdapter(this, selectedBundles, false)
        unselectedBundlesAdapter = BundleSelectionAdapter(this, unselectedBundles, true)

        selectedBundlesList.adapter = selectedBundlesAdapter
        unselectedBundlesList.adapter = unselectedBundlesAdapter
        selectedBundlesList.layoutManager = LinearLayoutManager(this)
        unselectedBundlesList.layoutManager = LinearLayoutManager(this)

    }

    override fun selectBundle(bundle: ShoppingBundle, position: Int) {
        selectedBundles.add(bundle)
        unselectedBundles.removeAt(position)
        selectedBundlesAdapter.notifyItemInserted(selectedBundles.size)
        unselectedBundlesAdapter.notifyItemRemoved(position)
    }
}