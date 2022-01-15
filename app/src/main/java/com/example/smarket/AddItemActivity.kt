package com.example.smarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AddItemActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        findViewById<Button>(R.id.scan_barcode_button).setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.finishAddingItemsButton).setOnClickListener {
            val intent = Intent(this, FridgeActivity::class.java)
            startActivity(intent)
        }

        val searchRecycler : RecyclerView = findViewById(R.id.searchResultList)
        val adapter = SearchItemsAdapter(emptyArray())
        searchRecycler.adapter = adapter
        searchRecycler.layoutManager = LinearLayoutManager(this)

        val addedItemsRecycler = findViewById<RecyclerView>(R.id.addedItemsRecycler)
        val adapter2 = ShoppingItemsListAdapter(mutableListOf())
        addedItemsAdapter = adapter2
        addedItemsRecycler.adapter = adapter2
        addedItemsRecycler.layoutManager = LinearLayoutManager(this)

        adapter.onSearchClicked = { itemId, itemName ->
            adapter2.addItem(ShoppingItem(itemName,itemId,1,"kurac"))
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
                        adapter.clearItems()
                        for (document in documents) {
                            val nameIdPair = Pair(document.data["name"] as String, (document.data["id"] as String).toLong())
                            adapter.addItem(nameIdPair)
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