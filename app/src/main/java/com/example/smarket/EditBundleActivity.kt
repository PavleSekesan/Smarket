package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.adapters.BundleItemsListAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class EditBundleActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bundle)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.edit_bundle_activity_title))

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.bundles
        BottomNavigator(this, bottomNavigation)

        val editBundleItemsRecyclerView = findViewById<RecyclerView>(R.id.editBundleItemsRecyclerView)
        editBundleItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        val bundleTitleEditText = findViewById<EditText>(R.id.bundleTitleEditText)
        val addProductButton = findViewById<Button>(R.id.addProductButton)
        val totalSumTextView = findViewById<TextView>(R.id.totalSumTextView)

        getAllBundles().addOnSuccessListener { allBundles ->
            val bundle = allBundles.find { it.id == intent.getStringExtra("bundle_id") }!! as ShoppingBundle
            TotalSumUpdater(totalSumTextView, bundle)
            bundleTitleEditText.doAfterTextChanged { text ->
                bundle.name.databaseValue = text.toString()
            }
            addProductButton.setOnClickListener {
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("bundle_id", bundle.id)
                startActivity(intent)
            }
            // FIXME This listener causes an infinite loop paired with doAfterTextChanged.
//            bundle.name.addOnChangeListener {it,_ ->
//                    bundleTitleEditText.setText(it)
//            }
            editBundleItemsRecyclerView.adapter = BundleItemsListAdapter(bundle, true)
            bundleTitleEditText.setText(bundle.name.databaseValue)
        }
    }
}