package com.example.smarket

import UserOrder
import android.content.Context
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import java.lang.Long.max
import java.time.DayOfWeek
import java.time.LocalDate
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
fun expandUserOrders(userOrders: List<UserOrder>, date: LocalDate) : Map<LocalDate, MutableList<String>> {
    val monthStart = date.withDayOfMonth(1)
    val monthEnd = date.withDayOfMonth(date.lengthOfMonth())
    val ordersOnDate: MutableMap<LocalDate, MutableList<String>> = mutableMapOf()
    for (order in userOrders) {
        val orderDate = order.date.toLocalDate()
        if (orderDate.isAfter(monthEnd)) {
            continue
        }
        if (order.recurring) {
            val minRepeats =
                max(0, ChronoUnit.DAYS.between(orderDate, monthStart)) / order.daysToRepeat
            val maxRepeats = ceil(
                ChronoUnit.DAYS.between(orderDate, monthEnd).toDouble() / order.daysToRepeat
            ).toLong() + 1

            for (k in minRepeats..maxRepeats) {
                val currentDate = orderDate.plusDays(k * order.daysToRepeat)
                if (currentDate.isAfter(monthEnd) || currentDate.isBefore(monthStart)) {
                    break
                }
                if (ordersOnDate.containsKey(currentDate)) {
                    ordersOnDate[currentDate]!!.add(order.id)
                } else {
                    ordersOnDate[currentDate] = mutableListOf(order.id)
                }
            }
        } else if (!orderDate.isBefore(monthStart)) {
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
