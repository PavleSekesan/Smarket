package com.example.smarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {

    private val TAG = "SignInActivity"

    private fun updateUI(user: FirebaseUser?)
    {
        if(user != null || true)
        {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser()
    {
        val auth = Firebase.auth

        val email = findViewById<EditText>(R.id.sign_in_email_edit_text).text.toString()
        val password = findViewById<EditText>(R.id.sign_in_password_edit_text).text.toString()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        findViewById<Button>(R.id.sign_in_button).setOnClickListener {
            loginUser()
        }
        findViewById<TextView>(R.id.sign_up_link).setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}