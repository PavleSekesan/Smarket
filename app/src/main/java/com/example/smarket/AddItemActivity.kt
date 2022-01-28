package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arlib.floatingsearchview.FloatingSearchView
import com.example.smarket.adapters.BundleItemsListAdapter
import com.example.smarket.adapters.FridgeItemsListAdapter
import com.example.smarket.adapters.QuantityItemsListAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class AddItemActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.add_item_activity_title))
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val search = findViewById<FloatingSearchView>(R.id.floating_search_view)
        val confirmFab = findViewById<FloatingActionButton>(R.id.confirmFab)
        var isFridge = true
        var chosenBundleId: String? = null

        confirmFab.setOnClickListener { finish() }

        search.setOnMenuItemClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            intent.putExtra("fridge",isFridge)
            intent.putExtra("adding",true)
            if (!isFridge)
            {
                intent.putExtra("bundle_id",chosenBundleId)
            }
            startActivity(intent)
        }
        
        search.setOnQueryChangeListener { oldQuery, newQuery ->
            if (search.isSearchBarFocused) {
                UserData.getAlgoliaProductSearch(newQuery).addOnSuccessListener { res->
                    val newSuggestions = res as List<UserData.Product>
                    search.swapSuggestions(newSuggestions)
                }
            }
        }

        search.setSearchFocused(true)

        val addedItemsRecycler = findViewById<RecyclerView>(R.id.addedItemsRecycler)
        addedItemsRecycler.layoutManager = LinearLayoutManager(this)

        getAllBundles().addOnSuccessListener { allBundles ->
            val bundle = allBundles.find { it.id == intent.getStringExtra("bundle_id") } as ShoppingBundle?
            isFridge = bundle == null
            chosenBundleId = bundle?.id

            bottomNavigation.selectedItemId = if (isFridge) R.id.fridge else R.id.bundles
            BottomNavigator(this, bottomNavigation)

            val addedItemsAdapterLocal = if (!isFridge) BundleItemsListAdapter(bundle!!, true, false) else FridgeItemsListAdapter(mutableListOf())
            addedItemsAdapter = addedItemsAdapterLocal
            addedItemsRecycler.adapter = addedItemsAdapter

            search.setOnBindSuggestionCallback { suggestionView, leftIcon, textView, item, itemPosition ->
                suggestionView.setOnClickListener {
                    val product = item as UserData.Product
                    if (!isFridge) {
                        if (bundle!!.items.any { item -> item.product.id == product.id})
                        {
                            Snackbar.make(bottomNavigation, R.string.bundle_item_already_exists, Snackbar.LENGTH_SHORT).show()
                        }
                        else {
                            bundle.addBundleItem("kom", product, 1)
                        }
                    } else {
                        UserData.fridgeItemFromProduct(product).addOnSuccessListener { ret->
                            val existingQuantity = (ret as UserData.FridgeItem).quantity.databaseValue
                            Snackbar.make(bottomNavigation, R.string.fridge_item_already_exists, Snackbar.LENGTH_SHORT).show()
                            UserData.removeFridgeItemByProduct(product).addOnSuccessListener {
                                UserData.addNewFridgeItem("kom", product, existingQuantity)
                            }
                        }.addOnFailureListener {
                            UserData.addNewFridgeItem("kom", item as UserData.Product, 1)
                        }
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