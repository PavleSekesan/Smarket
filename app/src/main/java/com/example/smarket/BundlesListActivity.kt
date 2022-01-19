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
            val adapter = BundlesListAdapter(UserData.getAllBundles().toMutableList())
            runOnUiThread {
                bundlesRecyclerView.adapter = adapter
                bundlesRecyclerView.layoutManager = GridLayoutManager(this@BundlesListActivity, 2)
            }
        }

        val addBundleFab = findViewById<FloatingActionButton>(R.id.addBundleFab)
        addBundleFab.setOnClickListener {
            GlobalScope.launch {
                val newBundle = UserData.addNewBundle(getString(R.string.new_bundle_name))
                val intent = Intent(this@BundlesListActivity, EditBundleActivity::class.java)
                intent.putExtra("bundle_id", newBundle.id)
                startActivity(intent)
            }
        }
    }
}