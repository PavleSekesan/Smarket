package com.example.smarket.adapters

import UserData.Delivery
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarket.*
import java.time.format.DateTimeFormatter

class DeliveriesAdapter(private var deliveries : List<Delivery>, val nonconfirmed : Boolean) :
    RecyclerView.Adapter<DeliveriesAdapter.ViewHolder>()  {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView
        val deliveryActionButton : Button

        init {
            // Define click listener for the ViewHolder's View.
            dateTextView = view.findViewById(R.id.dateTextView)
            deliveryActionButton = view.findViewById(R.id.deliveryActionButton)
        }
    }

    fun addDelivery(delivery: Delivery)
    {
        deliveries = deliveries.plus(delivery)
        super.notifyItemInserted(deliveries.size)
    }

    fun removeDelivery(delivery: Delivery)
    {
        val removePos = deliveries.indexOf(deliveries.find { it.id == delivery.id })
        if(removePos != -1)
        {
            deliveries = deliveries.filter { it.id !=  delivery.id}
            super.notifyItemRemoved(removePos)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.delivery, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val context = viewHolder.itemView.context
        if (nonconfirmed)
            viewHolder.deliveryActionButton.text = context.resources.getString(R.string.action_log_delivery)
        val delivery = deliveries[position]
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val first_time = delivery.date.databaseValue.format(timeFormatter)
        val second_time = delivery.endDate.databaseValue.format(timeFormatter)
        val date = formatDateSerbianLocale(delivery.date.databaseValue.toLocalDate())
        viewHolder.dateTextView.text = date + " " + first_time + " - " + second_time
        viewHolder.deliveryActionButton.setOnClickListener {
            val intent = Intent(context, ReviewDeliveryActivity::class.java)
            intent.putExtra("delivery_id", delivery.id)
            context.startActivity(intent)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = deliveries.size

}