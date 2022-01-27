package com.example.smarket.adapters

import UserData.FridgeItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.smarket.R

class FridgeItemsListAdapter(fridgeItems: List<FridgeItem>, displayPrevious : Boolean = true) : QuantityItemsListAdapter() {
    var hiddenItems : List<FridgeItem> = listOf()

    init {
        if (displayPrevious) items = fridgeItems
        UserData.addOnFridgeModifyListener { fridgeItem, databaseEventType ->
            if (fridgeItem != null) {
                if (databaseEventType == UserData.DatabaseEventType.ADDED) {
                    addItem(fridgeItem)
                } else if (databaseEventType == UserData.DatabaseEventType.REMOVED) {
                    removeItem(fridgeItem)
                }
            }
        }
    }

    fun getItemsFittingSearch(query : String) : List<FridgeItem> {
        var fittingItems = mutableListOf<FridgeItem>()
        var allItems = items as MutableList<FridgeItem>
        for (item in hiddenItems) {
            if (!allItems.contains(item))
                allItems.add(item)
        }
        if (query == "") {
            fittingItems = allItems
        } else {
            for (item in allItems) {
                if (item.product.name.databaseValue.startsWith(query)) {
                    fittingItems.add(item)
                }
            }
        }

        return fittingItems
    }

    fun displaySearchedItems(query : String) {
        val fittingItems = getItemsFittingSearch(query)
        val newHiddenItems = mutableListOf<FridgeItem>()
        for (item in items) {
            if (!fittingItems.contains(item))
                newHiddenItems.add(item as FridgeItem)
        }
        for (item in hiddenItems) {
            if (!fittingItems.contains(item))
                newHiddenItems.add(item)
        }

        items = items.filter { fittingItems.contains(it) }
        hiddenItems = newHiddenItems
        super.notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : QuantityViewHolder(view) {
        override val itemName : TextView = view.findViewById(R.id.bundleItemNameTextView)
        override val quantity: TextView = view.findViewById(R.id.quantityTextView)
        override val measuringUnit: TextView = view.findViewById(R.id.measuringUnitTextView)
        override val add: Button = view.findViewById(R.id.addButton)
        override val subtract: Button = view.findViewById(R.id.subtractButton)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.fridge_item, viewGroup, false)

        return ViewHolder(view)
    }
}