package com.example.smarket.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.smarket.R
import UserData.QuantityItem

class LogDeliveryAdapter(private val products : List<UserData.DeliveryItem>, val onFinish : () -> Unit) : QuantityItemsListAdapter() {

    var pos = 0
    init {
        items = listOf(products[pos])
    }

    inner class ViewHolder(view: View) : QuantityViewHolder(view) {
        override val itemName : TextView = view.findViewById(R.id.bundleItemNameTextView)
        override val quantity: TextView = view.findViewById(R.id.quantityForDelivery)
        override val measuringUnit: TextView = view.findViewById(R.id.measuringUnitTextView)
        override val add: Button = view.findViewById(R.id.addButton)
        override val subtract: Button = view.findViewById(R.id.subtractButton)
        val price: TextView = view.findViewById(R.id.priceTextView)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.bundle_item, viewGroup, false)

        return ViewHolder(view)
    }

    fun moveOver() {
        // TODO Add confirmed item (product[pos])  to fridge
        pos++
        if (pos >= products.size)
            onFinish()
        else
            items = listOf(products[pos])
    }

    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        val item = items[position]

        val castedViewHolder = viewHolder as ViewHolder
        val currency = " RSD"
        castedViewHolder.price.text = item.product.price.databaseValue.toString() + currency
        item.product.price.addOnChangeListener { d, _ -> castedViewHolder.price.text = d.toString() + currency }
    }
}