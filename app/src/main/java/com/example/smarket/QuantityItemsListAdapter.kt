package com.example.smarket

import BundleItem
import FridgeItem
import QuantityItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class QuantityItemsListAdapter(private var quantityItems: MutableList<QuantityItem>) :
    RecyclerView.Adapter<QuantityItemsListAdapter.QuantityViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    abstract inner class QuantityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val itemName: TextView
        abstract val quantity: TextView
        abstract val measuringUnit: TextView
        abstract val add: Button
        abstract val subtract: Button
    }

    fun clearItems() {
        quantityItems.clear()
        super.notifyDataSetChanged()
    }

    fun addItem(newItem: QuantityItem) {
        quantityItems.add(newItem)
        super.notifyItemInserted(quantityItems.size)
    }

    // Create new views (invoked by the layout manager)
    abstract override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): QuantityViewHolder //{
        // Create a new view, which defines the UI of the list item
//        val bundleItemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.bundle_item, viewGroup, false)
//        val fridgeItemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.fridge_item, viewGroup, false)
//
//        var view : View = bundleItemView // When collection is empty ????????????????????????
//
//        if (quantityItems.isNotEmpty()) {
//            if (quantityItems[0] is BundleItem) view = bundleItemView
//            else if (quantityItems[0] is FridgeItem) view = fridgeItemView
//        }
//        return ViewHolder(view)
//    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val context = viewHolder.itemView.context
        val item = quantityItems[position]
        viewHolder.itemName.text = item.product.name
        viewHolder.quantity.text = item.quantity.toString()
        viewHolder.measuringUnit.text = item.measuringUnit

//        if (editable) {
//            viewHolder.add.setOnClickListener {
//                GlobalScope.launch {
//                    if (item is BundleItem) UserData.updateBundleItemQuantity(item, 1)
//                    else if (item is FridgeItem) UserData.updateFridgeQuantity(item, 1)
//                }
//            }
//            viewHolder.subtract.setOnClickListener {
//                GlobalScope.launch {
//                    if (item is BundleItem) UserData.updateBundleItemQuantity(item, -1)
//                    else if (item is FridgeItem) UserData.updateFridgeQuantity(item, -1)
//                }
//            }
//        }
//        else {
//            viewHolder.add.visibility = View.INVISIBLE
//            viewHolder.subtract.visibility = View.INVISIBLE
//        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = quantityItems.size

}