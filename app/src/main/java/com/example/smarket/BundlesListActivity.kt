package com.example.smarket

import ShoppingBundle
import android.app.appsearch.GlobalSearchSession
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BundlesListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)

        val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
        GlobalScope.launch {
            val adapter = BundlesListAdapter(UserData.getAllBundles() as MutableList<ShoppingBundle>)
            runOnUiThread {
                bundlesRecyclerView.adapter = adapter
                bundlesRecyclerView.layoutManager = GridLayoutManager(this@BundlesListActivity, 2)
            }
        }

        val addBundleFab = findViewById<FloatingActionButton>(R.id.addBundleFab)
        addBundleFab.setOnClickListener {
            val intent = Intent(this, EditBundleActivity::class.java)
            startActivity(intent)
        }
    }
}