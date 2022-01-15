package com.example.smarket

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendarview.CalendarView
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.Size
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    val deliveryTag = view.findViewById<View>(R.id.deliveryTag)
    val text1 = view.findViewById<TextView>(R.id.calendarDayText1)
    val text2 = view.findViewById<TextView>(R.id.calendarDayText2)
    val text3 = view.findViewById<TextView>(R.id.calendarDayText3)

}
class MonthViewContainer(view: View) : ViewContainer(view) {
    val legendLayout = view.findViewById<LinearLayout>(R.id.legendLayout)
}
data class Order(var date: LocalDate, var name: String, var deliveryID: Int)
data class Delivery(var date: LocalDate, var color: String)

class MainActivity : AppCompatActivity() {
    private fun setupListeners()
    {
        val settingsFab = findViewById<FloatingActionButton>(R.id.fabSettings)
        settingsFab.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val addBundleFab = findViewById<FloatingActionButton>(R.id.fabAddBundle)
        addBundleFab.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }

        val fridgeFab = findViewById<FloatingActionButton>(R.id.fabFridge)
        fridgeFab.setOnClickListener {
            val intent = Intent(this, FridgeActivity::class.java)
            startActivity(intent)
        }

        val bundlesFab = findViewById<FloatingActionButton>(R.id.fabBundles)
        bundlesFab.setOnClickListener {
            val intent = Intent(this, BundlesListActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListeners()

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null && false) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        // Dummy data
        val orders = mutableListOf<Order>()
        orders.add(Order(LocalDate.of(2022,1,15), "Rižoto",0))
        orders.add(Order(LocalDate.of(2022,1,16), "Kinoa sa piletinom",0))
        orders.add(Order(LocalDate.of(2022,1,17), "Mačka",0))
        orders.add(Order(LocalDate.of(2022,1,17), "Debeli kurac",1))
        orders.add(Order(LocalDate.of(2022,1,19), "Knedla",1))
        orders.add(Order(LocalDate.of(2022,1,19), "Knedla2",1))

        val deliveries = mutableListOf<Delivery>()
        deliveries.add(Delivery(LocalDate.of(2022, 1, 14),"#388E3C"))
        deliveries.add(Delivery(LocalDate.of(2022, 1, 16),"#EF6C00"))

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        // Logic for showing calendar cells (days)
        calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            // Called only when a new container is needed.
            override fun create(view: View) = DayViewContainer(view)

            // Called every time we need to reuse a container.
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.textView.text = day.date.dayOfMonth.toString()
                container.deliveryTag.setBackgroundColor(Color.TRANSPARENT)
                for(delivery in deliveries)
                {
                    if (delivery.date == day.date)
                    {
                        container.deliveryTag.setBackgroundColor(Color.parseColor(delivery.color))
                    }
                }
                val ordersOnCurrentDay = mutableListOf<Order>()
                for (order in orders)
                {
                    if (order.date == day.date)
                    {
                        ordersOnCurrentDay.add(order)
                    }
                }
                container.text1.text = ""
                container.text2.text = ""
                container.text3.text = ""
                val textFields = listOf(container.text1, container.text2, container.text3)

                if (ordersOnCurrentDay.size > 3)
                {
                    for(i in 0..1)
                    {
                        textFields[i].text = ordersOnCurrentDay[i].name
                        val deliveryColor = deliveries[ordersOnCurrentDay[i].deliveryID].color
                        textFields[i].setTextColor(Color.parseColor(deliveryColor))
                    }
                    container.text3.text = "..."
                }
                else
                {
                    for(i in 0 until ordersOnCurrentDay.size)
                    {
                        textFields[i].text = ordersOnCurrentDay[i].name
                        val deliveryColor = deliveries[ordersOnCurrentDay[i].deliveryID].color
                        textFields[i].setTextColor(Color.parseColor(deliveryColor))
                    }
                }

            }
        }

        val daysOfWeek = daysOfWeekFromLocale()

        // Logic for showing the month header (MON, TUE, WED ... )
        calendarView.monthHeaderBinder = object :
            MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }.forEachIndexed { index, tv ->
                        tv.text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            .toUpperCase(Locale.ENGLISH)
                        tv.setTextColorRes(R.color.example_5_text_grey)
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    month.yearMonth
                }
            }
        }

        // Logic for showing the large month and year header
        calendarView.monthScrollListener = {
            findViewById<TextView>(R.id.calendarYearText).text = it.yearMonth.year.toString()
            val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
            findViewById<TextView>(R.id.calendarMonthText).text = monthTitleFormatter.format(it.yearMonth)
        }

        // Change cell size so cells are longer
        val daySize = calendarView.daySize
        calendarView.daySize = Size(daySize.width,250)

        // Setup and show calendar
        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val lastMonth = currentMonth.plusMonths(10)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)
    }
}