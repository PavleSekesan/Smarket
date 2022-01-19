package com.example.smarket

import FridgeItem
import QuantityItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FridgeItemsListAdapter (private var fridgeItems: MutableList<FridgeItem>) : QuantityItemsListAdapter(fridgeItems as MutableList<QuantityItem>) {
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

    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        val item = fridgeItems[position]

        viewHolder.add.setOnClickListener {
            GlobalScope.launch {
                UserData.updateFridgeQuantity(item, 1)
            }
        }
        viewHolder.subtract.setOnClickListener {
            GlobalScope.launch {
                UserData.updateFridgeQuantity(item, -1)
            }
        }
    }
}