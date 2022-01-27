package com.example.smarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class VerifyPhoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_phone)

        findViewById<MaterialButton>(R.id.verifyPhoneButton).setOnClickListener {
            val eneteredCode = findViewById<TextInputEditText>(R.id.verifyPhoneCode).text.toString()
            UserData.sendPhoneVerificationCode(eneteredCode)
            UserData.setOnPhoneVerificationStatusChangeListener { status->
                if(status == 1) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                else if(status == -1)
                {
                    findViewById<TextView>(R.id.verifyPhonePrompt).setText(R.string.bad_phone_code)
                    findViewById<TextView>(R.id.verifyPhonePrompt).setTextAppearance(R.style.Widget_Smarket_TextViewError)
                }
            }
        }
    }
}