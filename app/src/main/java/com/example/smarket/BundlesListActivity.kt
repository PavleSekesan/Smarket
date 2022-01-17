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
    lateinit var bundles : List<ShoppingBundle>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)

        GlobalScope.launch {
            bundles = UserData.getAllBundles()
            val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
            bundlesRecyclerView.adapter = BundlesListAdapter(bundles)
            bundlesRecyclerView.layoutManager = GridLayoutManager(this@BundlesListActivity, 2)
        }

        val addBundleFab = findViewById<FloatingActionButton>(R.id.addBundleFab)
        addBundleFab.setOnClickListener {
            val intent = Intent(this, EditBundleActivity::class.java)
            startActivity(intent)
        }
    }
}