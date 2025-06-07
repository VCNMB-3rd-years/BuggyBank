package vcmsa.projects.buggybank

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vcmsa.projects.buggybank.databinding.ActivitySignInBinding

private const val TAG = "SignInActivity"

class Sign_in : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = Firebase.auth
        Log.d(TAG, "onCreate: UI initialized")
        
        // Apply window insets to the root view safely using binding
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            Log.d(TAG, "onCreate: Applied window insets")
            insets
        }
        
        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressed: Going to sign up page")
                val intent = Intent(this@Sign_in, Sign_up::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                finish()
            }
        })
        
        // Sign in button click
        binding.SignInButton.setOnClickListener {
            Log.d(TAG, "onClick: Sign In button clicked")
            signInUser()
        }
        
        // Sign up button click
        binding.SignInRegister.setOnClickListener {
            Log.d(TAG, "onClick: Register button clicked")
            startActivity(Intent(this, Sign_up::class.java))
            finish()
        }
        
        // Forgot password button click
        binding.vForgotPassword.setOnClickListener {
            Log.d(TAG, "onClick: Forgot Password button clicked")
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }
    
    private fun signInUser() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "signInUser: Starting Firebase login")
                auth.signInWithEmailAndPassword(
                    binding.SignInEmail.text.toString().trim(),
                    binding.SignInPassword.text.toString().trim()
                    
                ).await()
                
                val userId = auth.currentUser?.uid ?: throw Exception("User ID is null after sign-in")
                val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                dbRef.child("signedIn").setValue(true)
                
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "signInUser: Login successful, navigating to MenuBar")
                    startActivity(Intent(this@Sign_in, MenuBar::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "signInUser: Login failed", e)
                withContext(Dispatchers.Main) {
                    handleAuthException(e)
                }
            }
        }
    }
    
    private fun handleAuthException(e: Exception) {
        val message = when (e) {
            is FirebaseAuthInvalidUserException -> "User not found. Please register first."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthEmailException -> "Invalid email format."
            is FirebaseAuthWeakPasswordException -> "Password too weak."
            is FirebaseNetworkException -> "Network error. Please check your connection."
            else -> "Authentication failed: ${e.localizedMessage}"
        }
        
        Log.w(TAG, "handleAuthException: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Checking if user is already signed in")
        
        val currentUser = auth.currentUser ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(currentUser.uid).child("signedIn")
        
        dbRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.exists() && task.result.getValue(Boolean::class.java) == true) {
                Log.d(TAG, "onStart: User already signed in, navigating to MenuBar")
                startActivity(Intent(this, MenuBar::class.java))
                finish()
            } else {
                Log.d(TAG, "onStart: User is not signed in or data missing")
            }
        }
    }
}