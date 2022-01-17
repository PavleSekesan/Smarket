package com.example.smarket

import Product
import ShoppingBundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectedBundleActivity : AppCompatActivity() {
    private lateinit var bundle : ShoppingBundle
    private var bundleItems = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_bundle)

        val bundleId = intent.getStringExtra("bundle_id")

        val editBundleFab = findViewById<FloatingActionButton>(R.id.editBundleFab)
        editBundleFab.setOnClickListener {
            val intent = Intent(this, EditBundleActivity::class.java)
            intent.putExtra("bundle_id", bundleId)
            startActivity(intent)
        }

        val bundleItemsRecyclerView = findViewById<RecyclerView>(R.id.bundleItemsRecyclerView)
        val bundleTitleTextView = findViewById<TextView>(R.id.bundleTitleTextView)

        GlobalScope.launch {
            bundle = UserData.getAllBundles().find{ it.id == bundleId }!!
            bundleItems = bundle.products as MutableList<Product>

            bundleTitleTextView.text = bundle.name
            bundleItemsRecyclerView.adapter = ShoppingItemsListAdapter(bundleItems)
            bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this@SelectedBundleActivity)
        }
    }
}