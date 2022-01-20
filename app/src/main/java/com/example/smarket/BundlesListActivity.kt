package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.MainActivity.Companion.bundles
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BundlesListActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bundles_list)

        //var bundles = Gson().fromJson<MutableList<ShoppingBundle>>(intent.getStringExtra("bundles"), ShoppingBundle::class.java)

//        UserData.addOnShoppingBundleModifyListener { shoppingBundle, databaseEventType ->
//            bundles.add(shoppingBundle!!)
//        }


        val bundlesRecyclerView = findViewById<RecyclerView>(R.id.bundlesRecyclerView)
        bundlesRecyclerView.adapter = BundlesListAdapter(bundles)
        bundlesRecyclerView.layoutManager = GridLayoutManager(this, 2)

        val addBundleFab = findViewById<FloatingActionButton>(R.id.addBundleFab)
        addBundleFab.setOnClickListener {
            GlobalScope.launch {
                val newBundle = UserData.suspendAddNewBundle(getString(R.string.new_bundle_name), listOf())
                val intent = Intent(this@BundlesListActivity, EditBundleActivity::class.java)
                //intent.putExtra("bundle", Gson().toJson(newBundle))
                intent.putExtra("bundle_id", newBundle.id)
                startActivity(intent)
            }
        }
    }
}