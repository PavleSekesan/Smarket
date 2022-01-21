package com.example.smarket

import android.widget.TextView
import UserData.DatabaseEventType
import UserData.ShoppingBundle
import UserData.BundleItem
import kotlin.math.round

class TotalSumUpdater(val textView : TextView, val bundle : ShoppingBundle) {
    private var totalSum : Double = 0.0
    private val currency = " RSD"
    init {
        update()
        bundle.addOnSubitemChangeListener { _, _ ->
            update()
        }
    }
    
    private fun update() {
        totalSum = 0.0
        for (item in bundle.items) {
            totalSum += item.quantity.databaseValue * item.product.price.databaseValue
        }
        val roundedTotalSum = round(totalSum * 100) / 100
        textView.text = String.format("%.2f", roundedTotalSum) + currency
    }

}