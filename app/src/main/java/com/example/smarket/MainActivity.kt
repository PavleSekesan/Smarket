package com.example.smarket

import Delivery
import ShoppingBundle
import UserData
import UserOrder
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
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit


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

class MainActivity : AppCompatActivity() {
    private fun setupListeners()
    {
        val settingsFab = findViewById<FloatingActionButton>(R.id.fabSettings)
        settingsFab.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
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

        val newOrderFab = findViewById<FloatingActionButton>(R.id.fabNewOrder)
        newOrderFab.setOnClickListener {
            val intent = Intent(this, NewOrderActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListeners()

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }


        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        GlobalScope.launch {
            val userOrders = UserData.getAllUserOrders()
            val deliveries = UserData.getAllDeliveries()
            var currentMonth = LocalDate.now().month
            var allOrdersInMonth = expandUserOrders(userOrders,LocalDate.now())
            val userOrderColors = mutableMapOf<String,Int>()

            // Logic for showing calendar cells (days)
            calendarView.dayBinder = object : DayBinder<DayViewContainer> {
                // Called only when a new container is needed.
                override fun create(view: View) = DayViewContainer(view)

                private fun displayOrdersOnDay(bundlesOnCurrentDay: List<ShoppingBundle>, bundleColor: Int, textFields: List<TextView>)
                {
                    for (textField in textFields)
                    {
                        textField.text = ""
                    }
                    if (bundlesOnCurrentDay.size > textFields.size)
                    {
                        for(i in 0 .. textFields.size - 2)
                        {
                            textFields[i].text = bundlesOnCurrentDay[i].name
                            textFields[i].setTextColor(bundleColor)
                        }
                        textFields.last().text = "..."
                    }
                    else
                    {
                        for(i in bundlesOnCurrentDay.indices)
                        {
                            textFields[i].text = bundlesOnCurrentDay[i].name
                            textFields[i].setTextColor(bundleColor)
                        }
                    }
                }

                // Called every time we need to reuse a container.
                override fun bind(container: DayViewContainer, day: CalendarDay) {
                    if(day.date.month != currentMonth)
                    {
                        currentMonth = day.date.month
                        allOrdersInMonth = expandUserOrders(userOrders, day.date)
                    }
                    runOnUiThread {
                        container.textView.text = day.date.dayOfMonth.toString()
                        container.deliveryTag.setBackgroundColor(Color.TRANSPARENT)
                    }
                    var dayColor: Int
                    for(delivery in deliveries)
                    {
                        if (delivery.date.toLocalDate() == day.date)
                        {
                            dayColor = resources.getIntArray(R.array.different_32_colors)[day.date.dayOfMonth - 1]
                            runOnUiThread {
                                container.deliveryTag.setBackgroundColor(dayColor)
                            }
                            for(userOrder in delivery.userOrders)
                            {
                                userOrderColors[userOrder.id] = dayColor
                            }
                        }
                    }

                    val ordersOnCurrentDay = allOrdersInMonth[day.date]
                    val bundlesOnDay = mutableListOf<ShoppingBundle>()
                    var ordersColor = R.color.kelly_medium_gray
                    if (ordersOnCurrentDay != null) {
                        for (userOrderId in ordersOnCurrentDay) {
                            val order = userOrders.filter { it.id == userOrderId }[0]
                            ordersColor = userOrderColors[order.id]!!
                            for(bundle in order.bundles)
                            {
                                bundlesOnDay.add(bundle)
                            }
                        }
                    }
                    val textFields = listOf(container.text1, container.text2, container.text3)
                    runOnUiThread {
                        displayOrdersOnDay(bundlesOnDay,ordersColor,textFields)
                    }
                }
            }

            val daysOfWeek = daysOfWeekFromLocale()

            // Logic for showing the month header (MON, TUE, WED ... )
            calendarView.monthHeaderBinder = object :
                MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                    runOnUiThread {
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
                    }}
                }
            }

            // Logic for showing the large month and year header
            calendarView.monthScrollListener = {
                runOnUiThread{
                findViewById<TextView>(R.id.calendarYearText).text = it.yearMonth.year.toString()
                val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
                findViewById<TextView>(R.id.calendarMonthText).text = monthTitleFormatter.format(it.yearMonth)
            }}

            // Change cell size so cells are longer
            val daySize = calendarView.daySize
            calendarView.daySize = Size(daySize.width,250)

            // Setup and show calendar
            val currentMonthCalendar = YearMonth.now()
            val firstMonth = currentMonthCalendar.minusMonths(10)
            val lastMonth = currentMonthCalendar.plusMonths(10)
            val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            runOnUiThread {
                calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
                calendarView.scrollToMonth(currentMonthCalendar)
            }
        }
    }
}