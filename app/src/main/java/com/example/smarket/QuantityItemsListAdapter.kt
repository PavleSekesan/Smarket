package com.example.smarket

import UserData.QuantityItem
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

abstract class QuantityItemsListAdapter :
    RecyclerView.Adapter<QuantityItemsListAdapter.QuantityViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    protected var items: List<QuantityItem> = listOf()

    abstract inner class QuantityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val itemName: TextView
        abstract val quantity: TextView
        abstract val measuringUnit: TextView
        abstract val add: Button
        abstract val subtract: Button
    }

//    fun clearItems() {
//        items.clear()
//        notifyDataSetChanged()
//    }
//
    fun removeItem(item : QuantityItem) {
        val removePos = items.indexOf(items.find { it == item })
        if(removePos != -1)
        {
            items = items.filter { it-> it.id !=  item.id}
            super.notifyItemRemoved(removePos)
        }
    }
//
//    fun updateItem(modifiedItem : QuantityItem) {
//        val changedItemPos = items.indexOf(items.find { it.id == modifiedItem.id })
//        if (changedItemPos != -1) {
//            Log.d("adapterUpdate", this.toString())
//            items[changedItemPos] = modifiedItem
//            notifyItemChanged(changedItemPos)
//        }
//    }
//
    fun addItem(newItem: QuantityItem) {
        items = items.plus(newItem)
        super.notifyItemInserted(items.size)
    }

    // Create new views (invoked by the layout manager)
    abstract override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): QuantityViewHolder

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: QuantityViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val context = viewHolder.itemView.context
        val item = items[position]

        viewHolder.itemName.text = item.product.name.databaseValue
        viewHolder.quantity.text = item.quantity.databaseValue.toString()
        viewHolder.measuringUnit.text = item.measuringUnit.databaseValue

        item.product.name.addOnChangeListener {it,_ -> viewHolder.itemName.text = it }
        item.quantity.addOnChangeListener {it,_ -> viewHolder.quantity.text = it.toString() }
        item.measuringUnit.addOnChangeListener {it,_ -> viewHolder.measuringUnit.text = it }
        viewHolder.add.setOnClickListener { item.quantity.databaseValue++ }
        viewHolder.subtract.setOnClickListener { item.quantity.databaseValue-- }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = items.size

}