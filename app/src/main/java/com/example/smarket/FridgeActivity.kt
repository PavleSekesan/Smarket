package com.example.smarket

import UserData.FridgeItem
import UserData.getAllFridgeItems
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_fridge.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FridgeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fridge)

        val fridgeItemsRecyclerView = findViewById<RecyclerView>(R.id.fridgeItemsRecyclerView)
        val fridgeAddProductButton = findViewById<Button>(R.id.fridgeAddProductButton)

        fridgeItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        getAllFridgeItems().addOnSuccessListener { allFridgeItems ->
            val fridgeItems = allFridgeItems as List<FridgeItem>
            fridgeItemsRecyclerView.adapter = FridgeItemsListAdapter(fridgeItems)
        }


        fridgeAddProductButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }
    }
}