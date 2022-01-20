package com.example.smarket

import UserData.BundleItem
import UserData.ShoppingBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class BundleItemsListAdapter(bundle: ShoppingBundle, private val editable: Boolean, displayPrevious : Boolean = true) : QuantityItemsListAdapter() {
    init {
        // TODO Implement listener for bundle addition
        bundle.addOnSubitemChangeListener {
            notifyDataSetChanged()
        }
        if (displayPrevious || true) items = bundle.items
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
            .inflate(R.layout.bundle_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {
        super.onBindViewHolder(viewHolder, position)

        if (!editable) {
            viewHolder.add.visibility = View.INVISIBLE
            viewHolder.subtract.visibility = View.INVISIBLE
        }
    }
}