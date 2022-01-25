package com.example.smarket

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.utils.Size
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import androidx.recyclerview.widget.DefaultItemAnimator

class ViewSelectedCalendarDay : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_selected_calendar_day)
        super.bindListenersToTopBar()

        // Get data from intent
        val selectedDay = intent.getSerializableExtra("selectedDay") as CalendarDay
        val deliveryDay = intent.getSerializableExtra("deliveryDay") as LocalDate?
        val dayColor = intent.getIntExtra("dayColor",-1)

        // Format date for the selected day
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
        findViewById<TextView>(R.id.viewSelectedDayText).text = selectedDay.date.format(formatter)

        // Setup recycler view
        val recycler = findViewById<RecyclerView>(R.id.viewSelectedDayRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ViewSelectedDayAdapter(mutableListOf())
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
        }

        val ordersOnCurrentDay = MainActivity.userOrdersOnSelectedDay
        // Set description for the selected day
        if (deliveryDay != null)
        {
            val dateString = deliveryDay.format(formatter)
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
