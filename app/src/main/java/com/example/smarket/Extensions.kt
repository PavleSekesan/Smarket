package com.example.smarket

import UserData.UserOrder
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.lang.Long.max
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.ceil

internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)

internal fun TextView.setTextColorRes(@ColorRes color: Int) = setTextColor(context.getColorCompat(color))

fun daysOfWeekFromLocale(): Array<DayOfWeek> {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    var daysOfWeek = DayOfWeek.values()
    // Order `daysOfWeek` array so that firstDayOfWeek is at index 0.
    // Only necessary if firstDayOfWeek != DayOfWeek.MONDAY which has ordinal 0.
    if (firstDayOfWeek != DayOfWeek.MONDAY) {
        val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
        val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
        daysOfWeek = rhs + lhs
    }
    return daysOfWeek
}

// Computes all recurring dates to fill the month given by the date parameter and returns the map of dates to UserOrder ids
fun expandUserOrders(userOrders: List<UserOrder>, startDate: LocalDate, endDate: LocalDate) : Map<LocalDate, MutableList<String>> {
    val ordersOnDate: MutableMap<LocalDate, MutableList<String>> = mutableMapOf()
    for (order in userOrders) {
        val orderDate = order.date.databaseValue.toLocalDate()
        if (orderDate.isAfter(endDate)) {
            continue
        }
        if (order.recurring.databaseValue) {
            val minRepeats =
                max(0, ChronoUnit.DAYS.between(orderDate, startDate)) / order.daysToRepeat.databaseValue
            val maxRepeats = ceil(
                ChronoUnit.DAYS.between(orderDate, endDate).toDouble() / order.daysToRepeat.databaseValue
            ).toLong() + 1

            for (k in minRepeats..maxRepeats) {
                val currentDate = orderDate.plusDays(k * order.daysToRepeat.databaseValue)
                if (currentDate.isAfter(endDate) || currentDate.isBefore(startDate)) {
                    break
                }
                if (ordersOnDate.containsKey(currentDate)) {
                    ordersOnDate[currentDate]!!.add(order.id)
                } else {
                    ordersOnDate[currentDate] = mutableListOf(order.id)
                }
            }
        } else if (!orderDate.isBefore(startDate)) {
            val currentDate = orderDate
            if (ordersOnDate.containsKey(currentDate)) {
                ordersOnDate[currentDate]!!.add(order.id)
            } else {
                ordersOnDate[currentDate] = mutableListOf(order.id)
            }
        }

    }
    return ordersOnDate
}

fun displayBundlesInMaterialButtons(bundlesOnCurrentDay: List<UserData.ShoppingBundle>, bundleColor: Int, textFields: List<MaterialButton>)
{
    for (textField in textFields)
    {
        textField.text = ""
        textField.visibility = View.INVISIBLE
    }
    if (bundlesOnCurrentDay.size > textFields.size)
    {
        for(i in 0 .. textFields.size - 2)
        {
            textFields[i].visibility = View.VISIBLE
            textFields[i].text = bundlesOnCurrentDay[i].name.databaseValue
            textFields[i].setBackgroundColor(bundleColor)
        }
        textFields.last().text = "..."
        textFields.last().setBackgroundColor(bundleColor)
        textFields.last().visibility = View.VISIBLE
    }
    else
    {
        for(i in bundlesOnCurrentDay.indices)
        {
            textFields[i].text = bundlesOnCurrentDay[i].name.databaseValue
            textFields[i].visibility = View.VISIBLE
            textFields[i].setBackgroundColor(bundleColor)
        }
    }
}

fun displayBundlesInTextFields(bundlesOnCurrentDay: List<UserData.ShoppingBundle>, bundleColor: Int, textFields: List<TextView>)
{
    for (textField in textFields)
    {
        textField.text = ""
        textField.visibility = View.GONE
    }
    if (bundlesOnCurrentDay.size > textFields.size)
    {
        for(i in 0 .. textFields.size - 2)
        {
            textFields[i].text = bundlesOnCurrentDay[i].name.databaseValue
            textFields[i].visibility = View.VISIBLE
//            textFields[i].setTextColor(bundleColor)
        }
        textFields.last().text = "..."
        textFields.last().visibility = View.VISIBLE
//        textFields.last().setTextColor(bundleColor)
    }
    else
    {
        for(i in bundlesOnCurrentDay.indices)
        {
            textFields[i].text = bundlesOnCurrentDay[i].name.databaseValue
            textFields[i].visibility = View.VISIBLE
//            textFields[i].setTextColor(bundleColor)
        }
    }
}

fun formatDateSerbianLocale(date: LocalDate) : String
{
    val serbianLocale = Locale("sr")
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, serbianLocale)
    val dayOfMonth = date.dayOfMonth.toString()
    val month = date.month.getDisplayName(TextStyle.FULL, serbianLocale)
    val year = date.year.toString()
    return "$weekday, $dayOfMonth $month $year"
}