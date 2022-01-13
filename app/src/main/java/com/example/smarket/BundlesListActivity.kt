package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BundlesListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)

        val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
        var bundles: List<ShoppingBundle> = listOf(ShoppingBundle("Rizoto sa pecurkama", listOf(FridgeItem("Rizoto", 1, "kom."), FridgeItem("pecurka", 1, "kom."))), ShoppingBundle("kuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuurac", listOf(FridgeItem("kurac1", 2, "kom."), FridgeItem("kurac2", 2, "kom."), FridgeItem("kurac3", 5, "kom."))))
        bundlesRecyclerView.adapter = BundlesListAdapter(bundles)
        bundlesRecyclerView.layoutManager = GridLayoutManager(this, 2)
    }
}