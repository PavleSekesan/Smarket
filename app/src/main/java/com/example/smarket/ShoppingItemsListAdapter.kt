package com.example.smarket

import BundleItem
import Product
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShoppingItemsListAdapter(private var shoppingItems: MutableList<BundleItem>) :
    RecyclerView.Adapter<ShoppingItemsListAdapter.ViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView
        val quantity: TextView
        val measuringUnit: TextView
        val add: Button
        val subtract: Button

        init {
            // Define click listener for the ViewHolder's View.
            itemName = view.findViewById(R.id.nameTextView)
            quantity = view.findViewById(R.id.quantityTextView)
            measuringUnit = view.findViewById(R.id.measuringUnitTextView)
            add = view.findViewById(R.id.addButton)
            subtract = view.findViewById(R.id.subtractButton)
        }
    }

    fun addItem(newItem: BundleItem ) {
        shoppingItems.add(newItem)
        super.notifyItemInserted(shoppingItems.size - 1)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.shopping_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val context = viewHolder.itemView.context
        val item = shoppingItems[position]
        viewHolder.itemName.text = item.product.name
        viewHolder.quantity.text = item.quantity.toString()
        viewHolder.measuringUnit.text = item.measuringUnit
        // TODO Add and subtract quantities when buttons are clicked
        /*viewHolder.add.setOnClickListener {
            increaseItem(position)
        }
        viewHolder.subtract.setOnClickListener {
            decreaseItem(position)
        }*/
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = shoppingItems.size

}