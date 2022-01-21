package com.example.smarket

import UserData.ShoppingBundle
import UserData.DatabaseEventType
import UserData.getAllBundles
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BundlesListAdapter(private var bundles : List<ShoppingBundle>) :
    RecyclerView.Adapter<BundlesListAdapter.ViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    init {
        UserData.addOnShoppingBundleModifyListener { shoppingBundle, databaseEventType ->
            val bundle = shoppingBundle as ShoppingBundle
            if (databaseEventType == DatabaseEventType.ADDED)
                addItem(bundle)
//            else if (databaseEventType == DatabaseEventType.MODIFIED)
//                // TODO Implement modificaiton
//            else if (databaseEventType == DatabaseEventType.REMOVED)
//                // TODO Implement removal
        }
    }

    private fun addItem(bundle : ShoppingBundle) {
        bundles = bundles.plus(bundle)
        notifyItemInserted(bundles.size)
    }

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
        val context = viewHolder.itemView.context
        val bundle = bundles[position]
        viewHolder.bundleName.text = bundle.name.databaseValue
        val item1 = bundle.items.getOrNull(0)?.product?.name?.databaseValue
        val item2 = bundle.items.getOrNull(1)?.product?.name?.databaseValue
        val item3 = bundle.items.getOrNull(2)?.product?.name?.databaseValue
        viewHolder.item1.text = if(item1 == null) "" else "  -  " + item1
        viewHolder.item2.text = if(item2 == null) "" else "  -  " + item2
        viewHolder.item3.text = if(item3 == null) "" else "  -  " + item3

        bundle.name.addOnChangeListener {it,_ -> viewHolder.bundleName.text = it }
        viewHolder.itemView.setOnClickListener {
            val intent = Intent(context, SelectedBundleActivity::class.java)
            intent.putExtra("bundle_id", bundle.id)
            context.startActivity(intent)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = bundles.size

}