package com.example.smarket

import ShoppingBundle
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BundleSelectionAdapter(
    var bundles: MutableList<ShoppingBundle>,
    var onSelection: (bundle: ShoppingBundle) -> Unit = { b : ShoppingBundle -> }
) :
    RecyclerView.Adapter<BundleSelectionAdapter.ViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bundleName: TextView = view.findViewById(R.id.bundleNameTextView)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.bundle_simple, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val bundle = bundles[position]
        viewHolder.bundleName.text = bundle.name
        viewHolder.itemView.setOnClickListener {
            onSelection(bundle)
            bundles.removeAt(viewHolder.bindingAdapterPosition)
            notifyItemRemoved(viewHolder.bindingAdapterPosition)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = bundles.size
}