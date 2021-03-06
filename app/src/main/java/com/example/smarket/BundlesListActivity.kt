package com.example.smarket

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import com.example.smarket.adapters.BundlesListAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class BundlesListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)
        super.bindListenersToTopBar()
        super.removeBackButton()
        super.setTitle(getString(R.string.bundles_list_activity_title))

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.bundles
        BottomNavigator(this, bottomNavigation)


        val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
        getAllBundles().addOnSuccessListener {
            val bundles = it as List<ShoppingBundle>
            bundlesRecyclerView.adapter = BundlesListAdapter(bundles)
        }

        bundlesRecyclerView.layoutManager = GridLayoutManager(this, 2)

        val addBundleFab = findViewById<FloatingActionButton>(R.id.addBundleFab)
        addBundleFab.setOnClickListener {
            UserData.addNewBundle(getString(R.string.new_bundle_name), listOf()).addOnSuccessListener {
                val newBundle = it as ShoppingBundle
                val intent = Intent(this@BundlesListActivity, EditBundleActivity::class.java)
                intent.putExtra("bundle_id", newBundle.id)
                startActivity(intent)
            }
        }
    }
}