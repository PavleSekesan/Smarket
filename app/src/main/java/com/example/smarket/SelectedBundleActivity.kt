package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SelectedBundleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_bundle)

        val editBundleFab = findViewById<FloatingActionButton>(R.id.editBundleFab)
        val bundleItemsRecyclerView = findViewById<RecyclerView>(R.id.bundleItemsRecyclerView)
        bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        val totalSumTextView = findViewById<TextView>(R.id.totalSumTextView)
        val bundleTitleTextView = findViewById<TextView>(R.id.bundleTitleTextView)

        getAllBundles().addOnSuccessListener { allBundles ->
            val bundle = allBundles.find { it.id == intent.getStringExtra("bundle_id") }!! as ShoppingBundle

            TotalSumUpdater(totalSumTextView, bundle)

            bundleItemsRecyclerView.adapter = BundleItemsListAdapter(bundle, false)
            editBundleFab.setOnClickListener {
                val intent = Intent(this, EditBundleActivity::class.java)
                intent.putExtra("bundle_id", bundle.id)
                startActivity(intent)
            }
            bundle.name.addOnChangeListener {it,_ -> bundleTitleTextView.text = it }
            bundleTitleTextView.text = bundle.name.databaseValue
        }
    }
}