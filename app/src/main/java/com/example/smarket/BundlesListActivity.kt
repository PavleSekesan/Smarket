package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BundlesListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)

        val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
        var bundles: List<ShoppingBundle> = listOf(ShoppingBundle("Rizoto sa pecurkama", listOf(ShoppingItem("Rizoto", "12345", 1, "kom."), ShoppingItem("pecurka","12345", 1, "kom."))), ShoppingBundle("kuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuurac", listOf(ShoppingItem("kurac1","12345", 2, "kom."), ShoppingItem("kurac2","12345", 2, "kom."), ShoppingItem("kurac3","12345", 5, "kom."))))
        bundlesRecyclerView.adapter = BundlesListAdapter(bundles)
        bundlesRecyclerView.layoutManager = GridLayoutManager(this, 2)
    }
}