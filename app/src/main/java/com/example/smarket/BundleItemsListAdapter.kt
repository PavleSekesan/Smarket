package com.example.smarket

import UserData.DatabaseEventType
import UserData.BundleItem
import UserData.ShoppingBundle
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class BundleItemsListAdapter(bundle: ShoppingBundle, private val editable: Boolean, displayPrevious : Boolean = true) : QuantityItemsListAdapter() {
    init {
        if (displayPrevious) items = bundle.items
        bundle.addOnSubitemChangeListener { databaseItem, databaseEventType ->
            val bundleItem = databaseItem as BundleItem
            if (databaseEventType == DatabaseEventType.ADDED) {
                addItem(bundleItem)
            } else if (databaseEventType == DatabaseEventType.REMOVED) {
                removeItem(bundleItem)
            }
        }
    }

    inner class ViewHolder(view: View) : QuantityViewHolder(view) {
        override val itemName : TextView = view.findViewById(R.id.bundleItemNameTextView)
        override val quantity: TextView = view.findViewById(R.id.quantityTextView)
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

    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        val item = items[position]

        val castedViewHolder = viewHolder as ViewHolder
        val currency = " RSD"
        castedViewHolder.price.text = item.product.price.databaseValue.toString() + currency
        item.product.price.addOnChangeListener { d, _ -> castedViewHolder.price.text = d.toString() + currency }

        if (!editable) {
            viewHolder.add.visibility = View.INVISIBLE
            viewHolder.subtract.visibility = View.INVISIBLE
        }
    }
}