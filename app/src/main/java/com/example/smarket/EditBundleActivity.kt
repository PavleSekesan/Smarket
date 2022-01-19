package com.example.smarket

import BundleItem
import QuantityItem
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EditBundleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bundle)

        val bundleId = intent.getStringExtra("bundle_id")

        val editBundleItemsRecyclerView = findViewById<RecyclerView>(R.id.editBundleItemsRecyclerView)
        val bundleTitleEditText = findViewById<EditText>(R.id.bundleTitleEditText)
        val confirmEditFab = findViewById<FloatingActionButton>(R.id.confirmEditFab)
        val addProductButton = findViewById<Button>(R.id.addProductButton)

        var editBundleItemsAdapter: BundleItemsListAdapter
        editBundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)

        GlobalScope.launch {
            val bundle = UserData.getAllBundles().find { it.id == bundleId }!!
            var bundleItems = bundle.items as MutableList<BundleItem>

            runOnUiThread {
                editBundleItemsAdapter = BundleItemsListAdapter(bundleItems, true)
                bundleTitleEditText.setText(bundle.name)
                editBundleItemsRecyclerView.adapter = editBundleItemsAdapter
            }

//                UserData.addOnBundleModifyListener { bundleItem, databaseEventType ->
//                    if (databaseEventType == DatabaseEventType.MODIFIED) {
//                        // TODO Handle modification
//                    }
//                    else if (databaseEventType == DatabaseEventType.ADDED) {
//                        runOnUiThread {
//                            Log.d("velikaKitAAA", "nesto")
//                            bundleItemsAdapter.addItem(bundleItem!!)
//                            // FIXME double adding to RecyclerView
//                        }
//                    }
//                    else if (databaseEventType == DatabaseEventType.REMOVED) {
//                        // TODO Handle removal
//                    }
//                }
        }

        confirmEditFab.setOnClickListener {
            GlobalScope.launch {
                val newName : String = bundleTitleEditText.text.toString()
                UserData.changeBundleName(bundleId!!, newName)
                finish()
            }
        }

        addProductButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            intent.putExtra("bundle_id", bundleId)
            startActivity(intent)
        }
    }
}