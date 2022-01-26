package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.widget.Button
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddItemActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.add_item_activity_title))
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val search = findViewById<FloatingSearchView>(R.id.floating_search_view)
        val confirmFab = findViewById<FloatingActionButton>(R.id.confirmFab)

        confirmFab.setOnClickListener { finish() }

        search.setOnMenuItemClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            intent.putExtra("adding",true)
            startActivity(intent)
        }
        
        search.setOnQueryChangeListener { oldQuery, newQuery ->
            if (search.isSearchBarFocused) {

//                UserData.getAlgoliaProductSearch(newQuery).addOnSuccessListener { res->
//                    val
//                    search.swapSuggestions(newSuggestions)
//                }


//                val db = FirebaseFirestore.getInstance()
//                val docRef = db.collection("Products")
//                    .whereGreaterThanOrEqualTo("name", newQuery)
//                    .whereLessThanOrEqualTo("name", newQuery + "\uf8ff")
//                    .limit(8)
//
//                docRef.get()
//                    .addOnSuccessListener { documents ->
//                        val suggestions = mutableListOf<UserData.Product>()
//                        for (document in documents) {
//                            val prod = UserData.productFromDoc(document)
//                            suggestions.add(prod)
//                        }
//                        search.swapSuggestions(suggestions)
//                    }
//                    .addOnFailureListener { exception ->
//                        Log.d("Kurac", "Crko")
//                    }
            }
        }

        search.setSearchFocused(true)

        val addedItemsRecycler = findViewById<RecyclerView>(R.id.addedItemsRecycler)
        addedItemsRecycler.layoutManager = LinearLayoutManager(this)

        getAllBundles().addOnSuccessListener { allBundles ->
            val bundle = allBundles.find { it.id == intent.getStringExtra("bundle_id") } as ShoppingBundle?
            val isFridge = bundle == null

            bottomNavigation.selectedItemId = if (isFridge) R.id.fridge else R.id.bundles
            BottomNavigator(this, bottomNavigation)

            val addedItemsAdapterLocal = if (!isFridge) BundleItemsListAdapter(bundle!!, true, false) else FridgeItemsListAdapter(mutableListOf())
            addedItemsAdapter = addedItemsAdapterLocal
            addedItemsRecycler.adapter = addedItemsAdapter

            search.setOnBindSuggestionCallback { suggestionView, leftIcon, textView, item, itemPosition ->
                suggestionView.setOnClickListener {
                    if (!isFridge) {
                        bundle!!.addBundleItem("kom", item as UserData.Product, 1)
                    } else {
                        UserData.addNewFridgeItem("kom", item as UserData.Product, 1)
                    }
                    search.clearSearchFocus()
                    search.clearQuery()
                }
            }
        }
    }
    companion object
    {
        lateinit var addedItemsAdapter: QuantityItemsListAdapter
    }
}