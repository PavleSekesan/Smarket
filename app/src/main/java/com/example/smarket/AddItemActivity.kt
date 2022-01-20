package com.example.smarket

import UserData.ShoppingBundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.MainActivity.Companion.bundles
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class AddItemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        //val bundle = Gson().fromJson(intent.getStringExtra("bundle"), ShoppingBundle::class.java)
        val bundle = bundles.find { it.id == intent.getStringExtra("bundle_id") }
        val isFridge = bundle == null

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
        val addedItemsAdapterLocal = if (!isFridge) BundleItemsListAdapter(bundle!!, true, false) else FridgeItemsListAdapter(mutableListOf())
        addedItemsAdapter = addedItemsAdapterLocal
        addedItemsRecycler.adapter = addedItemsAdapterLocal
        addedItemsRecycler.layoutManager = LinearLayoutManager(this)


        searchAdapter.onSearchClicked = { product ->
            // FIXME Uncomment when suspend functions are added
            throw NotImplementedError("Search should be fixed")
//            GlobalScope.launch {
//                val newItem = if (!isFridge) {
//                    UserData.suspendAddItemToBundle(bundle, "kom", product, 1)
//                } else {
//                    UserData.suspendAddItemToFridge("kom", product, 1)
//                }
//            }
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
        lateinit var addedItemsAdapter: QuantityItemsListAdapter
    }
}