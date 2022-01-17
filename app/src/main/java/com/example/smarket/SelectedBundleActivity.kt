package com.example.smarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SelectedBundleActivity : AppCompatActivity() {
    private val bundleId = "Ya6qhvHEvEUSsU9Bjh0d" // FIXME Get id from database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_bundle)

        val editBundleFab = findViewById<FloatingActionButton>(R.id.editBundleFab)
        editBundleFab.setOnClickListener {
            val intent = Intent(this, EditBundleActivity::class.java)
            intent.putExtra("bundle_id", bundleId)
            startActivity(intent)
        }

        var bundleItems = mutableListOf(ShoppingItem("Duboko przeni kurac", "12345",3, "kom."), ShoppingItem("Plitko kuvana picka","12345", 2, "kg"))
        val bundleItemsRecyclerView = findViewById<RecyclerView>(R.id.bundleItemsRecyclerView)
        bundleItemsRecyclerView.adapter = ShoppingItemsListAdapter(bundleItems)
        bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}