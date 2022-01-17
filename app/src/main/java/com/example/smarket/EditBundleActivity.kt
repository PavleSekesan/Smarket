package com.example.smarket

import BundleItem
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EditBundleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bundle)

        val bundleId = intent.getStringExtra("bundle_id")

        val bundleItemsRecyclerView = findViewById<RecyclerView>(R.id.bundleItemsRecyclerView)
        val bundleTitleEditText = findViewById<EditText>(R.id.bundleTitleEditText)

        // Create new bundle
        if (bundleId == null) {
            var bundleItems : MutableList<BundleItem> = mutableListOf()
            bundleItemsRecyclerView.adapter = ShoppingItemsListAdapter(bundleItems)
            bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        }
        // Edit existing bundle
        else {
            GlobalScope.launch {
                val bundle = UserData.getAllBundles().find { it.id == bundleId }!!
                var bundleItems = bundle.items as MutableList<BundleItem>
                bundleTitleEditText.setText(bundle.name)
                bundleItemsRecyclerView.adapter = ShoppingItemsListAdapter(bundleItems)
                bundleItemsRecyclerView.layoutManager = LinearLayoutManager(this@EditBundleActivity)
            }
        }

        val addProductButton = findViewById<Button>(R.id.addProductButton)
        addProductButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            intent.putExtra("bundle_id", bundleId)
            startActivity(intent)
        }
    }
}