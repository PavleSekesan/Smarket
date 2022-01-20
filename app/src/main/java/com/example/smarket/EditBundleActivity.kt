package com.example.smarket

import UserData.ShoppingBundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EditBundleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bundle)

        //val bundle = Gson().fromJson(intent.getStringExtra("bundle"), ShoppingBundle::class.java)
        val bundle = MainActivity.bundles.find { it.id == intent.getStringExtra("bundle_id") }!!

        val editBundleItemsRecyclerView = findViewById<RecyclerView>(R.id.editBundleItemsRecyclerView)
        editBundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        editBundleItemsRecyclerView.adapter = BundleItemsListAdapter(bundle, true)
        val bundleTitleEditText = findViewById<EditText>(R.id.bundleTitleEditText)
        bundleTitleEditText.setText(bundle.name.databaseValue)
        val confirmEditFab = findViewById<FloatingActionButton>(R.id.confirmEditFab)
        val addProductButton = findViewById<Button>(R.id.addProductButton)


        bundle.name.addOnChangeListener { bundleTitleEditText.setText(it) }

//        GlobalScope.launch {
//            val bundle = UserData.getAllBundles().find { it.id == bundleId }!!
//            var bundleItems = bundle.items.toMutableList()
//
//            runOnUiThread {
//                editBundleItemsAdapter = BundleItemsListAdapter(bundleItems, true)
//                bundleTitleEditText.setText(bundle.name)
//                editBundleItemsRecyclerView.adapter = editBundleItemsAdapter
//            }
//        }

        confirmEditFab.setOnClickListener {
            val newName : String = bundleTitleEditText.text.toString()
            bundle.name.databaseValue = newName
            finish()
        }

        addProductButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            //intent.putExtra("bundle", Gson().toJson(bundle))
            intent.putExtra("bundle_id", bundle.id)
            startActivity(intent)
        }
    }
}