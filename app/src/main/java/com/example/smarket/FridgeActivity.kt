package com.example.smarket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FridgeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fridge)

        val itemsRecyclerView : RecyclerView = findViewById(R.id.bundlesRecyclerView)
        var fridgeItems : List<FridgeItem> = listOf(FridgeItem("Duboko przeni kurac", 3, "kom."), FridgeItem("Plitko kuvana picka", 2, "kg"))
        itemsRecyclerView.adapter = FridgeItemsAdapter(fridgeItems)
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}