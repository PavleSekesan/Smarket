package com.example.smarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EditBundleActivity : AppCompatActivity() {
    private val bundleId = "Ya6qhvHEvEUSsU9Bjh0d" // FIXME Get id from database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bundle)

        val bundleItemsRecyclerView = findViewById<RecyclerView>(R.id.bundleItemsRecyclerView)
        bundleItemsRecyclerView.adapter = ShoppingItemsListAdapter(mutableListOf())
        bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)

        val addProductButton = findViewById<Button>(R.id.addProductButton)
        addProductButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            intent.putExtra("bundle_id", bundleId)
            startActivity(intent)
        }
    }
}