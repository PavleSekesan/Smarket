package com.example.smarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class VerifyPhoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_phone)
        UserData.sendPhoneVerificationCode("") // Sends request to backend to send code to users phone

        val verifyButton = findViewById<MaterialButton>(R.id.verifyPhoneButton)
        val progressIndicator = findViewById<CircularProgressIndicator>(R.id.progressIndicator)
        val verificationCodeInput = findViewById<TextInputEditText>(R.id.verifyPhoneCode)

        verifyButton.setOnClickListener {
            verifyButton.isEnabled = false
            progressIndicator.visibility = View.VISIBLE
            val eneteredCode = verificationCodeInput.text.toString()
            UserData.sendPhoneVerificationCode(eneteredCode)

            UserData.setOnPhoneVerificationStatusChangeListener { status->
                if(status == 1) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                else if(status == -1)
                {
                    verificationCodeInput.error = getString(R.string.bad_phone_code)
                    verifyButton.isEnabled = true
                    progressIndicator.visibility = View.GONE
                }
            }
        }
    }
}