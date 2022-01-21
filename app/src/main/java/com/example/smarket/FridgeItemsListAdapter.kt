package com.example.smarket

import UserData.FridgeItem
import UserData.QuantityItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class FridgeItemsListAdapter(fridgeItems: List<FridgeItem>, displayPrevious : Boolean = true) : QuantityItemsListAdapter() {
    init {
        if (displayPrevious) items = fridgeItems
        UserData.addOnFridgeModifyListener { fridgeItem, databaseEventType ->
            if (databaseEventType == UserData.DatabaseEventType.ADDED) {
                if (fridgeItem != null) {
                    addItem(fridgeItem)
                }
            }
        }
    }

    inner class ViewHolder(view: View) : QuantityViewHolder(view) {
        override val itemName : TextView = view.findViewById(R.id.fridgeItemNameTextView)
        override val quantity: TextView = view.findViewById(R.id.fridgeItemQuantityTextView)
        override val measuringUnit: TextView = view.findViewById(R.id.fridgeItemMeasuringUnitTextView)
        override val add: Button = view.findViewById(R.id.fridgeAddButton)
        override val subtract: Button = view.findViewById(R.id.fridgeSubtractButton)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.fridge_item, viewGroup, false)

        return ViewHolder(view)
    }
}