package vcmsa.projects.buggybank

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var txtEmailAddress: EditText
    private lateinit var btnSubmit: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        auth = FirebaseAuth.getInstance()
        txtEmailAddress = findViewById(R.id.txtEmailAddress)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnSubmit.setOnClickListener {
            val email = txtEmailAddress.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter email address.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Email sent.",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                        val intent = Intent(this,Sign_in::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this,
                            task.exception!!.message.toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    }

}