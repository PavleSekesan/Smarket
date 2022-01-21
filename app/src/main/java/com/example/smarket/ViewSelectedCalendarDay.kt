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

class ViewSelectedCalendarDay : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_selected_calendar_day)
        val selectedDay = intent.getSerializableExtra("selectedDay") as CalendarDay
        findViewById<TextView>(R.id.viewSelectedDayText).text = selectedDay.date.toString()
        val deliveryTag = findViewById<View>(R.id.viewSelectedDayDeliveryTag)
        val recycler = findViewById<RecyclerView>(R.id.viewSelectedDayRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ViewSelectedDayAdapter(mutableListOf())
        recycler.adapter = adapter


        val ordersOnCurrentDay = MainActivity.userOrdersOnSelectedDay
        for(order in ordersOnCurrentDay)
        {
            adapter.addItem(order)
        }
    }

}
