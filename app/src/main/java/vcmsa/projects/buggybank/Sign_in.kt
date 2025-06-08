// Sign_in.kt
package vcmsa.projects.buggybank

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@Sign_in, Sign_up::class.java))
                finish()
            }
        })
        
        binding.SignInButton.setOnClickListener { signInUser() }
        
        binding.SignInRegister.setOnClickListener {
            startActivity(Intent(this, Sign_up::class.java))
            finish()
        }
        
        binding.vForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }
    
    private fun signInUser() {
        val email = binding.SignInEmail.text.toString().trim()
        val password = binding.SignInPassword.text.toString().trim()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("User is null")
                
                if (!user.isEmailVerified) {
                    withContext(Dispatchers.Main) {
                        showVerificationDialog(user)
                    }
                    return@launch
                }
                
                FirebaseDatabase.getInstance().getReference("users").child(user.uid)
                    .child("signedIn").setValue(true).await()
                
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@Sign_in, MenuBar::class.java))
                    finish()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleAuthException(e)
                }
            }
        }
    }
    
    private fun showVerificationDialog(user: FirebaseUser) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email Not Verified")
        builder.setMessage("Please verify your email before logging in.")
        
        builder.setPositiveButton("Resend Email") { _, _ ->
            user.sendEmailVerification().addOnCompleteListener { task ->
                Toast.makeText(
                    this,
                    if (task.isSuccessful) "Verification email resent." else "Failed to send email.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        builder.setNeutralButton("Change Email") { _, _ ->
            auth.signOut()
            startActivity(Intent(this, Sign_up::class.java))
            finish()
        }
        
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
