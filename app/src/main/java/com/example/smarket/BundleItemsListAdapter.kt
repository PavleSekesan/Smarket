package com.example.smarket

import BundleItem
import QuantityItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.content.Context


class BundleItemsListAdapter(private val context : Context, private var bundleItems: MutableList<BundleItem>, private val editable : Boolean) : QuantityItemsListAdapter(context, bundleItems as MutableList<QuantityItem>) {
    init {
        // TODO handle bundle title modification
        UserData.addOnBundleModifyListener { bundleItem, databaseEventType ->
            when (databaseEventType) {
                DatabaseEventType.MODIFIED -> {
                    (context as Activity).runOnUiThread {
                        update(bundleItem!!)
                    }
                }
                DatabaseEventType.ADDED -> {
                    (context as Activity).runOnUiThread {
                        addItem(bundleItem!!)
                    }
                }
                DatabaseEventType.REMOVED -> {
                    // TODO Handle removal
                }
            }
        }
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
        val item = bundleItems[position]

        if (editable) {
            viewHolder.add.setOnClickListener {
                GlobalScope.launch {
                    UserData.updateBundleItemQuantity(item, 1)
                }
            }
            viewHolder.subtract.setOnClickListener {
                GlobalScope.launch {
                    UserData.updateBundleItemQuantity(item, -1)
                }
            }
        }
        else {
            viewHolder.add.visibility = View.INVISIBLE
            viewHolder.subtract.visibility = View.INVISIBLE
        }
    }
}