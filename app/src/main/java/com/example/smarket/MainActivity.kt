package com.example.smarket

import UserData.Delivery
import UserData.ShoppingBundle
import UserData
import UserData.UserOrder
import UserData.removeUserOrder
import android.graphics.Color
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.navigation.*
import java.time.LocalDateTime
import android.app.Activity
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.core.view.updateLayoutParams
import com.algolia.search.client.ClientSearch
import com.algolia.search.dsl.attributesToRetrieve
import com.algolia.search.dsl.query
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MonthViewContainer(view: View) : ViewContainer(view) {
    val legendLayout = view.findViewById<LinearLayout>(R.id.legendLayout)
}

class MainActivity : BaseActivity() {
    companion object {
        lateinit var userOrdersOnSelectedDay: List<UserOrder>
        lateinit var allOrdersInMonth: Map<LocalDate, MutableList<String>>
        lateinit var dayColors: MutableList<Int>
        lateinit var userOrders: List<UserOrder>
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0)
        {
            val editedOrder = EditOrderActivity.selectedOrder
            if (editedOrder.bundles.isEmpty())
            {
                removeUserOrder(editedOrder)
            }
        }
    }

    fun setLocale(activity: Activity, languageCode: String?) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = activity.resources
        val config = resources.getConfiguration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.getDisplayMetrics())
    }

    fun setupCalendarDayCell(container: DayViewContainerLarge, day: CalendarDay, deliveries: List<Delivery>, userOrderColors: MutableMap<String,Int>,
                             userOrderDeliveryDates: MutableMap<String,LocalDate>)
    {
        val dateFormatter = DateTimeFormatter.ofPattern("dd")
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

        container.textViewDate.text = day.date.format(dateFormatter)
        container.textViewMonth.text = day.date.format(monthFormatter)
        container.textViewWeekday.text = day.date.format(dayFormatter)

        //container.deliveryTag.setBackgroundColor(Color.TRANSPARENT)
        container.deliveryTag.setTextColor(Color.WHITE)

        var dayColor: Int
        for(delivery in deliveries)
        {
            if (delivery.date.databaseValue.toLocalDate() == day.date)
            {
                dayColor = dayColors.first()
                dayColors.removeFirst()
                //container.deliveryTag.setBackgroundColor(dayColor)
                container.deliveryTag.setTextColor(dayColor)

                for(userOrder in delivery.userOrders)
                {
                    userOrderColors[userOrder.id] = dayColor
                    userOrderDeliveryDates[userOrder.id] = delivery.date.databaseValue.toLocalDate()
                }
            }
        }

        val ordersOnCurrentDay = allOrdersInMonth[day.date]
        val bundlesOnDay = mutableListOf<ShoppingBundle>()
        if (ordersOnCurrentDay != null) {
            for (userOrderId in ordersOnCurrentDay) {
                val order = userOrders.filter { it.id == userOrderId }[0]

                container.dayColor = if(userOrderColors.containsKey(order.id)) userOrderColors[order.id]!! else Color.GRAY
                container.deliveryDay = if(userOrderDeliveryDates.containsKey(order.id)) userOrderDeliveryDates[order.id]!! else null
                for(bundle in order.bundles)
                {
                    bundlesOnDay.add(bundle)
                }
                order.addOnSubitemChangeListener { databaseItem, databaseEventType ->
                    val bundleChanged = databaseItem as ShoppingBundle
                    if(databaseEventType == UserData.DatabaseEventType.ADDED)
                    {
                        bundlesOnDay.add(bundleChanged)
                    }
                    else if (databaseEventType == UserData.DatabaseEventType.REMOVED)
                    {
                        bundlesOnDay.removeIf { bundle-> bundle.id == bundleChanged.id}
                    }
                    else
                    {
                    }
                    val textFields = listOf(container.text1, container.text2, container.text3)
                    displayBundlesInMaterialButtons(bundlesOnDay,container.dayColor,textFields)
                }
            }
        }
        val textFields = listOf(container.text1, container.text2, container.text3)
        displayBundlesInMaterialButtons(bundlesOnDay,container.dayColor,textFields)
    }


    fun setupCalendarDayCellWrapperListener(container: DayViewContainerLarge)
    {
        container.wrapper.setOnClickListener {
            val orders = mutableListOf<UserOrder>()
            val idsOnSelectedDay = allOrdersInMonth[container.day.date]
            if (idsOnSelectedDay != null) {
                for (userOrderId in idsOnSelectedDay) {
                    val order = userOrders.filter { it.id == userOrderId }[0]
                    orders.add(order)
                }
            }

            userOrdersOnSelectedDay = orders
            val intent = Intent(container.wrapper.context,ViewSelectedCalendarDay::class.java)
            intent.putExtra("selectedDay", container.day)
            intent.putExtra("deliveryDay", container.deliveryDay)
            intent.putExtra("dayColor", container.dayColor)

            container.wrapper.context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        super.bindListenersToTopBar()
        setLocale(this,"sr")
        UserData.loadPreferencesFromFirebase(this)

        val newOrderFab = findViewById<FloatingActionButton>(R.id.fabAddNewOrder)
        newOrderFab.setOnClickListener {
            UserData.addNewUserOrder(LocalDateTime.now(),false,-1).addOnSuccessListener {
                val intent = Intent(this, EditOrderActivity::class.java)
                intent.putExtra("selected_order_id",it.id)
                startActivityForResult(intent,0)
            }
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.calendar
        BottomNavigator(this, bottomNavigation)

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        if (calendarView.maxRowCount == 1 || true)
        {
            val calendarHeader = findViewById<LinearLayout>(R.id.calendarHeader)
            calendarHeader.visibility = View.GONE
        }

        UserData.getAllUserOrders().addOnSuccessListener { res1->
            userOrders = res1 as List<UserOrder>
            UserData.getAllDeliveries().addOnSuccessListener { res2 ->

                var deliveries = res2 as List<Delivery>
                val userOrderColors = mutableMapOf<String,Int>()
                val userOrderDeliveryDates = mutableMapOf<String,LocalDate>()

                var currentMonth = LocalDate.now().month
                allOrdersInMonth = expandUserOrders(userOrders,
                    LocalDate.now().minusMonths(11),
                    LocalDate.now().plusMonths(11))
                dayColors = resources.getIntArray(R.array.different_32_colors).toMutableList()

                // Logic for showing calendar cells (days)
                calendarView.dayBinder = object : DayBinder<DayViewContainerLarge> {
                    // Called only when a new container is needed.
                    override fun create(view: View) = DayViewContainerLarge(view)

                    // Called every time we need to reuse a container.
                    override fun bind(container: DayViewContainerLarge, day: CalendarDay) {
                        container.day = day

                        if (day.date.isAfter(LocalDate.now()))
                        {
                            setupCalendarDayCellWrapperListener(container)
                        }
                        else
                        {
                            container.wrapper.setBackgroundColor(Color.GRAY)
                        }

                        if(day.date.month != currentMonth)
                        {
                            currentMonth = day.date.month
                            dayColors = resources.getIntArray(R.array.different_32_colors).toMutableList()
                        }
                        setupCalendarDayCell(container,day,deliveries,userOrderColors,userOrderDeliveryDates)
                        UserData.addOnUserOrderModifyListener { userOrder, databaseEventType ->
                            // Check if the order has not yet been added or removed
                            if (userOrder != null)
                            {
                                if(databaseEventType == UserData.DatabaseEventType.ADDED && userOrders.none { ord-> ord.id == userOrder.id })
                                {
                                    userOrders = userOrders.plus(userOrder)
                                }
                                else if(databaseEventType == UserData.DatabaseEventType.REMOVED && userOrders.any { ord-> ord.id == userOrder.id })
                                {
                                    userOrders = userOrders.filter { order-> order.id !=  userOrder.id}
                                }

                                // Reset the available colors and recompute all expanded orders
                                dayColors = resources.getIntArray(R.array.different_32_colors).toMutableList()
                                allOrdersInMonth = expandUserOrders(userOrders,
                                    LocalDate.now().minusMonths(11),
                                    LocalDate.now().plusMonths(11))
                            }
                            setupCalendarDayCell(container,day,deliveries,userOrderColors,userOrderDeliveryDates)
                        }

                        UserData.addOnDeliveryModifyListener { delivery, databaseEventType ->
                            if(delivery != null)
                            {
                                if(databaseEventType == UserData.DatabaseEventType.ADDED && deliveries.none { d -> d.id == delivery.id })
                                {
                                    deliveries = deliveries.plus(delivery)
                                }
                                else if(databaseEventType == UserData.DatabaseEventType.REMOVED && deliveries.any { d -> d.id == delivery.id })
                                {
                                    for(userOrder in delivery.userOrders)
                                    {
                                        userOrderColors.remove(userOrder.id)
                                        userOrderDeliveryDates.remove(userOrder.id)
                                    }
                                    deliveries = deliveries.filter { d -> d.id != delivery.id }
                                }
                                dayColors = resources.getIntArray(R.array.different_32_colors).toMutableList()
                            }
                            setupCalendarDayCell(container,day,deliveries,userOrderColors,userOrderDeliveryDates)
                        }
                    }
                }

                val daysOfWeek = daysOfWeekFromLocale()

                if (calendarView.maxRowCount != 1 && false)
                {
                    // Logic for showing the month header (MON, TUE, WED ... )
                    calendarView.monthHeaderBinder = object :
                        MonthHeaderFooterBinder<MonthViewContainer> {
                        override fun create(view: View) = MonthViewContainer(view)
                        override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                            // Setup each header day text if we have not done that already.
                            if (container.legendLayout.tag == null) {
                                container.legendLayout.tag = month.yearMonth
                                container.legendLayout.children.map { it as TextView }.forEachIndexed { index, tv ->
                                    tv.text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                        .toUpperCase(Locale.ENGLISH)
                                    tv.setTextColorRes(R.color.kelly_medium_gray)
                                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                }
                                month.yearMonth
                            }}
                    }
                    // Logic for showing the large month and year header
                    calendarView.monthScrollListener = {
                        findViewById<TextView>(R.id.calendarYearText).text =
                            it.yearMonth.year.toString()
                        val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
                        findViewById<TextView>(R.id.calendarMonthText).text =
                            monthTitleFormatter.format(it.yearMonth)
                    }
                }

                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels

                // Change cell size so cells are longer
                val daySize = calendarView.daySize
                calendarView.daySize = Size(resources.getDimensionPixelSize(R.dimen.day_size_large),screenHeight)

                // Setup and show calendar
                val currentMonthCalendar = YearMonth.now()
                val firstMonth = currentMonthCalendar.minusMonths(10)
                val lastMonth = currentMonthCalendar.plusMonths(10)
                val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

                calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
                //calendarView.scrollToMonth(currentMonthCalendar)
                calendarView.scrollToDate(LocalDate.now())
            }
        }
    }

    class DayViewContainerSmall(view: View) : ViewContainer(view) {
        val textView = view.findViewById<TextView>(R.id.calendarDayTextMonth)
        val deliveryTag = view.findViewById<MaterialButton>(R.id.calendarDayTextMonth)

        val text1 = view.findViewById<MaterialButton>(R.id.calendarDayText1)
        val text2 = view.findViewById<MaterialButton>(R.id.calendarDayText2)
        val text3 = view.findViewById<MaterialButton>(R.id.calendarDayText3)
        val wrapper = view.findViewById<ConstraintLayout>(R.id.calendarDayWrapper)
        lateinit var day: CalendarDay
        var deliveryDay: LocalDate? = null
        var dayColor = -1
        init {

        }
    }

    class DayViewContainerLarge(view: View) : ViewContainer(view) {
        val textViewMonth = view.findViewById<TextView>(R.id.calendarDayTextMonth)
        val textViewDate = view.findViewById<TextView>(R.id.calendarDayTextDate)
        val textViewWeekday = view.findViewById<TextView>(R.id.calendarDayTextWeekday)
        val deliveryTag = textViewDate

        val text1 = view.findViewById<MaterialButton>(R.id.calendarDayText1)
        val text2 = view.findViewById<MaterialButton>(R.id.calendarDayText2)
        val text3 = view.findViewById<MaterialButton>(R.id.calendarDayText3)
        val wrapper = view.findViewById<ConstraintLayout>(R.id.calendarDayWrapper)
        lateinit var day: CalendarDay
        var deliveryDay: LocalDate? = null
        var dayColor = -1
        init {
            wrapper.setOnClickListener {
                val orders = mutableListOf<UserOrder>()
                val idsOnSelectedDay = allOrdersInMonth[day.date]
                if (idsOnSelectedDay != null) {
                    for (userOrderId in idsOnSelectedDay) {
                        val order = userOrders.filter { it.id == userOrderId }[0]
                        orders.add(order)
                    }
                }

                userOrdersOnSelectedDay = orders
                val intent = Intent(view.context,ViewSelectedCalendarDay::class.java)
                intent.putExtra("selectedDay", day)
                intent.putExtra("deliveryDay",deliveryDay)
                intent.putExtra("dayColor",dayColor)

                view.context.startActivity(intent)
            }
        }
    }
}