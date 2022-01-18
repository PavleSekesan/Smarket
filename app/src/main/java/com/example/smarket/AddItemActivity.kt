package com.example.smarket

import BundleItem
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.initialize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddItemActivity : AppCompatActivity() {

    var addedItems = mutableListOf<BundleItem>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        findViewById<Button>(R.id.scan_barcode_button).setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.finishAddingItemsButton).setOnClickListener {
            finish()
        }

        val searchRecycler : RecyclerView = findViewById(R.id.searchResultList)
        val searchAdapter = SearchItemsAdapter(mutableListOf())
        searchRecycler.adapter = searchAdapter
        searchRecycler.layoutManager = LinearLayoutManager(this)

        val addedItemsRecycler = findViewById<RecyclerView>(R.id.addedItemsRecycler)
        val adapter2 = ShoppingItemsListAdapter(mutableListOf(), true)
        addedItemsAdapter = adapter2
        addedItemsRecycler.adapter = adapter2
        addedItemsRecycler.layoutManager = LinearLayoutManager(this)

        val bundleId = intent.getStringExtra("bundle_id")
        searchAdapter.onSearchClicked = { product ->
            GlobalScope.launch {
                val newItem = if (bundleId != null) {
                    UserData.addItemToBundle(bundleId, "kom", product, 1)
                } else {
                    UserData.addItemToFridge("kom", product, 1)
                }
                runOnUiThread {
                    adapter2.addItem(newItem as BundleItem)
                }
            }
        }

        val productSearch: SearchView = findViewById(R.id.productSearchView)
        productSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("Products")
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")

                docRef.get()
                    .addOnSuccessListener { documents ->
                        searchAdapter.clearItems()
                        for (document in documents) {
                            val prod = UserData.productFromDoc(document)
                            searchAdapter.addItem(prod)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("Kurac", "Crko")
                    }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })
    }
    companion object
    {
        lateinit var addedItemsAdapter: ShoppingItemsListAdapter
    }
}