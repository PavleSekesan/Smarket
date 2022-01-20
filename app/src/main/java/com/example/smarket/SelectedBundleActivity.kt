package com.example.smarket

import UserData.ShoppingBundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.example.smarket.MainActivity.Companion.bundles

class SelectedBundleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_bundle)

        //val bundle = Gson().fromJson(intent.getStringExtra("bundle"), ShoppingBundle::class.java)
        val bundle = bundles.find { it.id == intent.getStringExtra("bundle_id") }!!

        val editBundleFab = findViewById<FloatingActionButton>(R.id.editBundleFab)
        editBundleFab.setOnClickListener {
            val intent = Intent(this, EditBundleActivity::class.java)
            //intent.putExtra("bundle", Gson().toJson(bundle))
            intent.putExtra("bundle_id", bundle.id)
            startActivity(intent)
        }

        val bundleItemsRecyclerView = findViewById<RecyclerView>(R.id.bundleItemsRecyclerView)
        bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        bundleItemsRecyclerView.adapter = BundleItemsListAdapter(bundle, false)
        val bundleTitleTextView = findViewById<TextView>(R.id.bundleTitleTextView)
        bundleTitleTextView.text = bundle.name.databaseValue

        bundle.name.addOnChangeListener { bundleTitleTextView.text = it }

//        GlobalScope.launch {
//            val bundle = UserData.getAllBundles().find { it.id == bundleId }!!
//            val bundleItems = bundle.items.toMutableList()
//
//            runOnUiThread {
//                bundleTitleTextView.text = bundle.name
//                bundleItemsAdapter = BundleItemsListAdapter(this@SelectedBundleActivity, bundleItems, false)
//                bundleItemsRecyclerView.adapter = bundleItemsAdapter
//            }
//        }
    }
}