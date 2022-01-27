package com.example.smarket.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import UserData.UserOrder
import android.content.Intent
import android.widget.ImageButton
import com.example.smarket.EditOrderActivity
import com.example.smarket.R
import com.example.smarket.displayBundlesInTextFields
import com.example.smarket.formatDateSerbianLocale
import com.google.android.material.card.MaterialCardView

class ViewSelectedDayAdapter(private var dataSet: MutableList<UserOrder>, private val editable: Boolean) :
    RecyclerView.Adapter<ViewSelectedDayAdapter.ViewHolder>()  {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card = view.findViewById<MaterialCardView>(R.id.userOrderItemCard)
        val textView = view.findViewById<TextView>(R.id.userOrderItemText)
        val bundlesTexts = listOf<TextView>(
            view.findViewById(R.id.userOrderBundleText1),
            view.findViewById(R.id.userOrderBundleText2),
            view.findViewById(R.id.userOrderBundleText3),
            view.findViewById(R.id.userOrderBundleText4),
            view.findViewById(R.id.userOrderBundleText5))
        val bundleIcons = listOf<ImageButton>(
            view.findViewById(R.id.imageButton1),
            view.findViewById(R.id.imageButton2),
            view.findViewById(R.id.imageButton3),
            view.findViewById(R.id.imageButton4),
            view.findViewById(R.id.imageButton5))
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
        if (indexToRemove != -1) {
            dataSet.removeAt(indexToRemove)
            super.notifyItemRemoved(indexToRemove)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.user_order_item_layout, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.itemView.context
        val dateStr = formatDateSerbianLocale(dataSet[position].date.databaseValue.toLocalDate())
        viewHolder.textView.text =
            if(dataSet[position].recurring.databaseValue)
                context.getString(R.string.view_selected_day_order_recurring, dataSet[position].daysToRepeat.databaseValue, dateStr)
            else
                context.getString(R.string.view_selected_day_order_not_recurring, dateStr)

        if (editable) {
            viewHolder.card.setOnClickListener {
                val intent = Intent(viewHolder.textView.context, EditOrderActivity::class.java)
                intent.putExtra("selected_order_id", dataSet[position].id)
                viewHolder.textView.context.startActivity(intent)
            }
        }
        displayBundlesInTextFields(dataSet[position].bundles, R.color.kelly_medium_gray,viewHolder.bundlesTexts)
        for(i in viewHolder.bundlesTexts.indices)
        {
            viewHolder.bundleIcons[i].visibility = viewHolder.bundlesTexts[i].visibility
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}