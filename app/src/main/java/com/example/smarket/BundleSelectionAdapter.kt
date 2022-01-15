package com.example.smarket

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BundleSelectionAdapter(var context : Context, private var bundles: MutableList<ShoppingBundle>, var clickable : Boolean) :
    RecyclerView.Adapter<BundleSelectionAdapter.ViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bundleName: TextView

        init {
            // Define click listener for the ViewHolder's View.
            bundleName = view.findViewById(R.id.bundleNameTextView)
        }
    }

    private val callback : BundleSelectionInterface? = context as? BundleSelectionInterface


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
        val context = viewHolder.itemView.context

        val bundle = bundles[position]
        viewHolder.bundleName.text = bundle.name
        if (clickable) {
            viewHolder.itemView.setOnClickListener {
                callback?.selectBundle(bundle, position)
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = bundles.size
}