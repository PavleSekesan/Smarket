package com.example.smarket.adapters

import UserData.ShoppingBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.R
import com.example.smarket.TotalSumUpdater
import kotlin.math.round

class BundleSelectionAdapter(
    val bundles: MutableList<ShoppingBundle>, val unselected : Boolean,
    var onSelection: (bundle: ShoppingBundle) -> Unit = { b : ShoppingBundle -> }
) :
    RecyclerView.Adapter<BundleSelectionAdapter.ViewHolder>()  {

    var totalPrice : Double = 0.0
    lateinit var recyclerView : RecyclerView

    init {
        for (bundle in bundles) {
            for (item in bundle.items) {
                totalPrice += item.quantity.databaseValue * item.product.price.databaseValue
            }
        }
    }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bundleName: TextView = view.findViewById(R.id.bundleNameTextView)
        val price : TextView = view.findViewById(R.id.priceTextView)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item

        val view = if (unselected) LayoutInflater.from(viewGroup.context).inflate(R.layout.bundle_simple, viewGroup, false)
        else LayoutInflater.from(viewGroup.context).inflate(R.layout.bundle_simple_selected, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val bundle = bundles[position]
        viewHolder.bundleName.text = bundle.name.databaseValue
        TotalSumUpdater(viewHolder.price, bundle)
        bundle.name.addOnChangeListener {it,_ -> viewHolder.bundleName.text = it }
        viewHolder.itemView.setOnClickListener {
            onSelection(bundle)
        }
    }

    fun addBundle(newBundle: ShoppingBundle)
    {
        bundles.add(newBundle)
        for (item in newBundle.items) {
            totalPrice += item.quantity.databaseValue * item.product.price.databaseValue
        }
        super.notifyItemInserted(bundles.size)
        recyclerView.layoutManager?.scrollToPosition(bundles.size - 1)
    }

    fun removeBundle(bundleToRemove: ShoppingBundle)
    {
        val indexToRemove = bundles.indexOfFirst { bundle-> bundle.id == bundleToRemove.id }
        if (indexToRemove != -1)
        {
            bundles.removeAt(indexToRemove)
            for (item in bundleToRemove.items) {
                totalPrice -= item.quantity.databaseValue * item.product.price.databaseValue
            }
            super.notifyItemRemoved(indexToRemove)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = bundles.size
}