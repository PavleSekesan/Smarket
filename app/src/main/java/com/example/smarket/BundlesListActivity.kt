package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import UserData.ShoppingBundle
import UserData.getAllBundles

class BundlesListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)

        val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
        getAllBundles().addOnSuccessListener {
            val bundles = it as List<ShoppingBundle>
            bundlesRecyclerView.adapter = BundlesListAdapter(it)
        }

        bundlesRecyclerView.layoutManager = GridLayoutManager(this, 2)

        val addBundleFab = findViewById<FloatingActionButton>(R.id.addBundleFab)
        addBundleFab.setOnClickListener {
            // FIXME Uncomment when onSuccessListener is implemented
//            UserData.addNewBundle(getString(R.string.new_bundle_name), listOf()).onSuccessListener {
//                val newBundle = it
//                val intent = Intent(this@BundlesListActivity, EditBundleActivity::class.java)
//                intent.putExtra("bundle_id", newBundle.id)
//                startActivity(intent)
//            }
        }
    }
}