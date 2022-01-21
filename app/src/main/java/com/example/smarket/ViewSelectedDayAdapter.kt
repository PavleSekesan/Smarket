package com.example.smarket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import UserData.UserOrder
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.firestore.auth.User

class ViewSelectedDayAdapter(private var dataSet: MutableList<UserOrder>) :
    RecyclerView.Adapter<ViewSelectedDayAdapter.ViewHolder>()  {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView = view.findViewById<TextView>(R.id.userOrderItemText)
        val bundlesTexts = listOf(view.findViewById<TextView>(R.id.userOrderBundleText1),
            view.findViewById<TextView>(R.id.userOrderBundleText2),
            view.findViewById<TextView>(R.id.userOrderBundleText3),
            view.findViewById<TextView>(R.id.userOrderBundleText4),
            view.findViewById<TextView>(R.id.userOrderBundleText5))
    }

    fun onOrderChanged(orderChanged: UserOrder)
    {
        for(i in dataSet.indices)
        {
            val order = dataSet[i]
            if (order.id == orderChanged.id)
            {
                super.notifyDataSetChanged()
            }
        }
    }

    fun addItem(newOrder: UserOrder)
    {
        dataSet.add(newOrder)
        super.notifyItemInserted(dataSet.size)
    }

    fun removeItem(orderToRemove: UserOrder)
    {
        val indexToRemove = dataSet.indexOfFirst { order -> order.id == orderToRemove.id }
        dataSet.removeAt(indexToRemove)
        super.notifyItemRemoved(indexToRemove)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.user_order_item_layout, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.itemView.context
        viewHolder.textView.text = dataSet[position].id
        viewHolder.textView.setOnClickListener {
            val intent = Intent(viewHolder.textView.context, EditOrderActivity::class.java)
            intent.putExtra("selected_order_id", dataSet[position].id)
            viewHolder.textView.context.startActivity(intent)
        }
        displayBundlesInTextFields(dataSet[position].bundles,R.color.kelly_medium_gray,viewHolder.bundlesTexts)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}