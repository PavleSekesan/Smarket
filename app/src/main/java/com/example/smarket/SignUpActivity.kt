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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

private val TAG = "SignUpActivity"

class PersonalUserData (val firstName: String, val lastName: String, val address: String, val phoneNumber: String, )
{
    init {

    }
    fun submitToDatabase()
    {
        val db = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        val data = hashMapOf(
            "address" to address,
            "first_name" to firstName,
            "last_name" to lastName,
            "phone_number" to phoneNumber
        )
        db.collection("UserData").document(auth.uid.toString()).collection("Settings").document("personal_info")
            .set(data)
            .addOnSuccessListener { Log.d(TAG, "UserData successfully written!") }
            .addOnFailureListener {
                    e -> Log.w(TAG, "Error writing UserData document", e)
                TODO("Delete user and report unrecoverable error")
            }
    }
}

class SignUpActivity : AppCompatActivity() {

    private fun updateUI(user:FirebaseUser?)
    {
        if(user != null)
        {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun checkData(userData: PersonalUserData): Boolean
    {
        return true
    }

    private fun collectUserData(): PersonalUserData {
        val firstName = findViewById<EditText>(R.id.first_name_edit_text).text.toString()
        val lastName = findViewById<EditText>(R.id.last_name_edit_text).text.toString()
        val address = findViewById<EditText>(R.id.address_edit_text).text.toString()
        val phoneNumber = findViewById<EditText>(R.id.phone_number_edit_text).text.toString()
        return PersonalUserData(firstName,lastName, address, phoneNumber)
    }

    private fun signUpUser()
    {
        val auth = Firebase.auth
        val email = findViewById<EditText>(R.id.sign_up_email_edit_text).text.toString()
        val password = findViewById<EditText>(R.id.sign_up_password_edit_text).text.toString()
        val userData = collectUserData()
        if(checkData(userData))
        {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        userData.submitToDatabase()
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, getString(R.string.error_generic),
                            Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
        else
        {
            Toast.makeText(baseContext, getString(R.string.error_personal_info),Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        findViewById<TextView>(R.id.sign_in_link).setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.sign_up_button).setOnClickListener {
            signUpUser()
        }
    }
}