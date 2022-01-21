package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EditBundleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bundle)

        val editBundleItemsRecyclerView = findViewById<RecyclerView>(R.id.editBundleItemsRecyclerView)
        editBundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        val bundleTitleEditText = findViewById<EditText>(R.id.bundleTitleEditText)
        val confirmEditFab = findViewById<FloatingActionButton>(R.id.confirmEditFab)
        val addProductButton = findViewById<Button>(R.id.addProductButton)
        val totalSumTextView = findViewById<TextView>(R.id.totalSumTextView)

        getAllBundles().addOnSuccessListener { allBundles ->
            val bundle = allBundles.find { it.id == intent.getStringExtra("bundle_id") }!! as ShoppingBundle
            TotalSumUpdater(totalSumTextView, bundle)
            confirmEditFab.setOnClickListener {
                val newName: String = bundleTitleEditText.text.toString()
                bundle.name.databaseValue = newName
                finish()
            }
            addProductButton.setOnClickListener {
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("bundle_id", bundle.id)
                startActivity(intent)
            }
            bundle.name.addOnChangeListener {it,_ -> bundleTitleEditText.setText(it) }
            editBundleItemsRecyclerView.adapter = BundleItemsListAdapter(bundle, true)
            bundleTitleEditText.setText(bundle.name.databaseValue)
        }
    }
}