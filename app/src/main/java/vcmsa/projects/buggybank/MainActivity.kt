package vcmsa.projects.buggybank

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.buggybank.databinding.ActivityMainBinding

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var authentication: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authentication = FirebaseAuth.getInstance()
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        val currentUser = authentication.currentUser
        Log.d(TAG, "onCreate: View binding and layout set")
        
//        Handler().postDelayed({
//            if (currentUser != null) {
//                val MainPage = Intent(this, MenuBar::class.java)
//                startActivity(MainPage)
//                finish()
//            } else {
//                val Login = Intent(this, Sign_in::class.java)
//                startActivity(Login)
//                finish()
//            }
        
//        } , 2000)
        // Apply window insets to the root view safely using binding
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            Log.d(TAG, "onCreate: Applied window insets")
            insets
        }
        
        // Button click listeners
        binding.btnSignUp.setOnClickListener {
            Log.d(TAG, "onClick: Sign Up button clicked")
            val intent = Intent(this, Sign_up::class.java)
            startActivity(intent)
        }
        
        binding.btnlogin.setOnClickListener {
            Log.d(TAG, "onClick: Login button clicked")
            val intent = Intent(this, Sign_in::class.java)
            startActivity(intent)
        }
        
        binding.vForgotPassword.setOnClickListener {
            Log.d(TAG, "onClick: Forgot Password button clicked")
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}