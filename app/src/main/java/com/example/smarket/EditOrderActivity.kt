package com.example.smarket

import UserData.ShoppingBundle
import UserData.getAllBundles
import UserData.getAllUserOrders
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.w3c.dom.Text
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class EditOrderActivity : BaseActivity() {
    companion object{
        lateinit var selectedOrder: UserData.UserOrder
        val TAG = "EditOrderActivity"
    }

    fun updateRecurringText(textViewToUpdate: TextView)
    {
        textViewToUpdate.text =
        if(selectedOrder.recurring.databaseValue)
            getString(R.string.edit_order_activity_recurring_description, selectedOrder.daysToRepeat.databaseValue)
        else
            getString(R.string.edit_order_activity_not_recurring)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_order)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.edit_order_activity_title))

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.edit_order_activity_pick_date)).setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        val recurringPickerItems = resources.getStringArray(R.array.recurring_times)
        val recurringPickerValues = resources.getIntArray(R.array.recurring_times_values)

        val recurringPicker = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.edit_order_activity_pick_recurring))

        val selectedOrderId = intent.getStringExtra("selected_order_id")

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.calendar
        BottomNavigator(this, bottomNavigation)

        val selectedBundlesList = findViewById<RecyclerView>(R.id.selectedBundlesRecyclerView)
        val unselectedBundlesList = findViewById<RecyclerView>(R.id.unselectedBundlesRecyclerView)
        selectedBundlesList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        unselectedBundlesList.layoutManager = GridLayoutManager(this, 2)

        var selectedBundles = mutableListOf<ShoppingBundle>()
        var unselectedBundles = mutableListOf<ShoppingBundle>()
        getAllUserOrders().addOnSuccessListener { ret1->
            val allUserOrders = ret1 as List<UserData.UserOrder>
            selectedOrder = allUserOrders.filter { order-> order.id == selectedOrderId }[0]
            val formatter = DateTimeFormatter.ofPattern("E, d MMMM yyyy")

            // Selected day text
            findViewById<TextView>(R.id.editOrderSelectedDate).text = selectedOrder.date.databaseValue.toLocalDate().format(formatter)
            selectedOrder.date.addOnChangeListener { localDateTime, databaseFieldEventType ->
                findViewById<TextView>(R.id.editOrderSelectedDate).text = localDateTime.toLocalDate().format(formatter)
            }

            // Recurring order text
            val recurringOrderTextView = findViewById<TextView>(R.id.editOrderSelectedRepeat)
            updateRecurringText(recurringOrderTextView)
            selectedOrder.daysToRepeat.addOnChangeListener { recurring, databaseFieldEventType ->
                updateRecurringText(recurringOrderTextView)
            }
            selectedOrder.recurring.addOnChangeListener { b, databaseFieldEventType ->
                updateRecurringText(recurringOrderTextView)
            }

            // Selected day card onclick
            findViewById<MaterialCardView>(R.id.editOrderSection1).setOnClickListener {
                datePicker.addOnPositiveButtonClickListener {
                    if(datePicker.selection != null) {
                        selectedOrder.date.databaseValue = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(datePicker.selection!!),
                            ZoneId.systemDefault()
                        );
                    }}
                datePicker.show(supportFragmentManager,TAG)
            }

            // Recurring card onclick
            findViewById<MaterialCardView>(R.id.editOrderSection2).setOnClickListener {
                recurringPicker.setItems(recurringPickerItems) { dialog, which ->
                    if (which < recurringPickerValues.size-1) {
                        selectedOrder.recurring.databaseValue = which > 0
                        selectedOrder.daysToRepeat.databaseValue = recurringPickerValues[which]
                    }
                    // TODO Handle "other" recurring choice
                }.show()
            }

            getAllBundles().addOnSuccessListener { ret2 ->
                val allBundles = ret2 as MutableList<ShoppingBundle>

                selectedBundles = selectedOrder.bundles.toMutableList()
                val selectedBundlesAdapter = BundleSelectionAdapter(selectedBundles, false)
                selectedBundlesList.adapter = selectedBundlesAdapter
                unselectedBundles = allBundles.filter { bundle -> selectedBundles.none { selectedBundle -> selectedBundle.id == bundle.id } }.toMutableList()
                val unselectedBundlesAdapter = BundleSelectionAdapter(unselectedBundles, true)
                unselectedBundlesList.adapter = unselectedBundlesAdapter

                selectedOrder.addOnSubitemChangeListener { databaseItem, databaseEventType ->
                    val bundleChanged = databaseItem as ShoppingBundle
                    if (databaseEventType == UserData.DatabaseEventType.ADDED)
                    {
                        selectedBundlesAdapter.addBundle(bundleChanged)
                        unselectedBundlesAdapter.removeBundle(bundleChanged)
                    }
                    else if(databaseEventType == UserData.DatabaseEventType.REMOVED)
                    {
                        selectedBundlesAdapter.removeBundle(bundleChanged)
                        unselectedBundlesAdapter.addBundle(bundleChanged)
                    }
                }

                selectedBundlesAdapter.onSelection = { bundle ->
                    selectedOrder.removeBundle(bundle).addOnSuccessListener {
//                        unselectedBundles.add(bundle)
//                        unselectedBundlesAdapter.notifyItemInserted(unselectedBundles.size)
                    }
                }
                unselectedBundlesAdapter.onSelection = { bundle ->
                    selectedOrder.addBunlde(bundle).addOnSuccessListener {
//                        selectedBundles.add(bundle)
//                        selectedBundlesAdapter.notifyItemInserted(selectedBundles.size)
                    }
                }
            }
        }
    }
}