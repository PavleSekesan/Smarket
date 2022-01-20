package com.example.smarket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import UserData.UserOrder

class ViewSelectedDayAdapter(private var dataSet: MutableList<UserOrder>) :
    RecyclerView.Adapter<ViewSelectedDayAdapter.ViewHolder>()  {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.userOrderItemText)
        }
    }

    fun addItem(newOrder: UserOrder)
    {
        dataSet.add(newOrder)
        super.notifyItemInserted(dataSet.size)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.user_order_item_layout, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.itemView.context
        viewHolder.textView.text = dataSet[position].id
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}