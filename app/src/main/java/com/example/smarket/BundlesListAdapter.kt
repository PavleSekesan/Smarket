package com.example.smarket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShoppingBundle(val name: String, val items: List<FridgeItem>) {

}

class BundlesListAdapter(private var bundles: List<ShoppingBundle>) :
    RecyclerView.Adapter<BundlesListAdapter.ViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bundleName: TextView
        val item1: TextView
        val item2: TextView
        val item3: TextView

        init {
            // Define click listener for the ViewHolder's View.
            bundleName = view.findViewById(R.id.titleTextView)
            item1 = view.findViewById(R.id.item1TextView)
            item2 = view.findViewById(R.id.item2TextView)
            item3 = view.findViewById(R.id.item3TextView)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.bundle, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val bundle = bundles[position]
        if (bundle != null)
        {
            viewHolder.bundleName.text = bundle.name
            viewHolder.item1.text = bundle.items.getOrNull(0)?.name ?: ""
            viewHolder.item2.text = bundle.items.getOrNull(1)?.name ?: ""
            viewHolder.item3.text = bundle.items.getOrNull(2)?.name ?: ""
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = bundles.size

}