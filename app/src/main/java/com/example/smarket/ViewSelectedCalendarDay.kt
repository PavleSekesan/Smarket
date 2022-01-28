package com.example.smarket

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kizitonwose.calendarview.model.CalendarDay
import java.time.LocalDate
import com.example.smarket.adapters.ViewSelectedDayAdapter
import com.typesafe.config.ConfigException
import java.time.temporal.ChronoUnit

class ViewSelectedCalendarDay : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_selected_calendar_day)
        super.bindListenersToTopBar()

        // Get data from intent
        val selectedDay = intent.getSerializableExtra("selectedDay") as CalendarDay
        val deliveryDay = intent.getSerializableExtra("deliveryDay") as LocalDate?
        val dayColor = intent.getIntExtra("dayColor",-1)
        val editable = selectedDay.date.isAfter(LocalDate.now()) || deliveryDay == null

        // Format date for the selected day
        findViewById<TextView>(R.id.viewSelectedDayText).text = formatDateSerbianLocale(selectedDay.date)

        // Setup recycler view
        val recycler = findViewById<RecyclerView>(R.id.viewSelectedDayRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ViewSelectedDayAdapter(mutableListOf(), editable)
        recycler.adapter = adapter


        UserData.addOnUserOrderModifyListener { userOrder, databaseEventType ->
            if(databaseEventType == UserData.DatabaseEventType.ADDED)
            {
                if (userOrder != null) {
                    adapter.addItem(userOrder)
                }
            }
            else if(databaseEventType == UserData.DatabaseEventType.REMOVED)
            {
                if (userOrder != null)
                {
                    adapter.removeItem(userOrder)
                }
            }
            else if(databaseEventType == UserData.DatabaseEventType.MODIFIED)
            {
                if (userOrder != null)
                {
                    if (userOrder.recurring.databaseValue)
                    {
                        val dayDifference = ChronoUnit.DAYS.between(selectedDay.date, userOrder.date.databaseValue)
                        if (dayDifference > 0 || dayDifference % userOrder.daysToRepeat.databaseValue != 0L)
                        {
                            adapter.removeItem(userOrder)
                        }
                    }
                    else
                    {
                        if (userOrder.date.databaseValue.toLocalDate() != selectedDay.date)
                        {
                            adapter.removeItem(userOrder)
                        }
                    }
                }
            }
        }

        val ordersOnCurrentDay = MainActivity.userOrdersOnSelectedDay
        // Set description for the selected day
        if (deliveryDay != null)
        {
            val dateString = formatDateSerbianLocale(deliveryDay)
            findViewById<TextView>(R.id.viewSelectedDayDescription).text =
                resources.getQuantityString(R.plurals.view_selected_day_has_delivery, ordersOnCurrentDay.size,ordersOnCurrentDay.size,dateString)
        }
        else
        {
            findViewById<TextView>(R.id.viewSelectedDayDescription).text =
                resources.getQuantityString(R.plurals.view_selected_day_no_delivery, ordersOnCurrentDay.size,ordersOnCurrentDay.size)
        }
        for(order in ordersOnCurrentDay)
        {
            adapter.addItem(order)
            order.addOnSubitemChangeListener { databaseItem, databaseEventType ->
                adapter.onOrderChanged(order)
            }
        }
    }

}
