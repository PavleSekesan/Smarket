package com.example.smarket.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.smarket.R
import UserData.QuantityItem
import UserData.addNewFridgeItem

class ReviewDeliveryAdapter(private val products : List<UserData.DeliveryItem>, private val editable: Boolean, val onFinish : () -> Unit) : QuantityItemsListAdapter(editable) {

    var pos = 0
    init {
        if (products.isEmpty())
            items = emptyList()
        else
            items = listOf(products[pos])
    }

    inner class ViewHolder(view: View) : QuantityViewHolder(view) {
        override val itemName : TextView = view.findViewById(R.id.bundleItemNameTextView)
        override val quantity: TextView = view.findViewById(R.id.quantityForDelivery)
        override val measuringUnit: TextView = view.findViewById(R.id.measuringUnitTextView)
        override val add: Button = view.findViewById(R.id.addButton)
        override val subtract: Button = view.findViewById(R.id.subtractButton)
        val quantityInFridge: TextView = view.findViewById(R.id.quantityInFridge)
        val quantityInBundle: TextView = view.findViewById(R.id.quantityInBundle)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.delivery_item, viewGroup, false)

        return ViewHolder(view)
    }

    fun moveOver() {
        if (editable && pos < products.size) {
            val item = products[pos]
            UserData.fridgeItemFromProduct(item.product).addOnSuccessListener { ret ->
                val fridgeItem = ret as UserData.FridgeItem
                fridgeItem.quantity.databaseValue += item.quantity.databaseValue
            }.addOnFailureListener {
                addNewFridgeItem(
                    item.measuringUnit.databaseValue,
                    item.product,
                    item.quantity.databaseValue
                )
            }
        }
        pos++
        if (pos >= products.size)
            onFinish()
        else {
            items = listOf(products[pos])
            super.notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        val item = items[position]
        val castViewHolder = viewHolder as ViewHolder
        castViewHolder.quantityInBundle.text = item.quantity.databaseValue.toString()
        UserData.fridgeItemFromProduct(item.product).addOnSuccessListener { ret->
            val fridgeItem = ret as UserData.FridgeItem
            castViewHolder.quantityInFridge.text = fridgeItem.quantity.databaseValue.toString()
            fridgeItem.quantity.addOnChangeListener { newQuantity, databaseFieldEventType ->
                castViewHolder.quantityInFridge.text = newQuantity.toString()
            }
        }.addOnFailureListener {
            castViewHolder.quantityInFridge.text = "0"
        }

    }
}