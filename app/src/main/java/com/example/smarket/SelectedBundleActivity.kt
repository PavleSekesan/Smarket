package com.example.smarket

import BundleItem
import QuantityItem
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectedBundleActivity : AppCompatActivity() {
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
        bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        var bundleItemsAdapter = BundleItemsListAdapter(this, mutableListOf(), false)
        bundleItemsRecyclerView.adapter = bundleItemsAdapter

        GlobalScope.launch {
            val bundle = UserData.getAllBundles().find { it.id == bundleId }!!
            val bundleItems = bundle.items.toMutableList()

            runOnUiThread {
                bundleTitleTextView.text = bundle.name
                bundleItemsAdapter = BundleItemsListAdapter(this@SelectedBundleActivity, bundleItems, false)
                bundleItemsRecyclerView.adapter = bundleItemsAdapter
            }
        }
    }
}