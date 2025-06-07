package vcmsa.projects.buggybank

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.buggybank.databinding.ActivitySignUpBinding
import java.security.MessageDigest

private const val TAG = "SignUpActivity"

@Suppress("DEPRECATION")
class Sign_up : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        
        // Window insets for edge-to-edge content
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Apply fade in animation to the entire layout
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)
        
        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressed: Going to sign in page")
                val intent = Intent(this@Sign_up, Sign_in::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                finish()
            }
        })
        
        // Go to sign in page when login button clicked
        binding.SignUpLogin.setOnClickListener {
            Log.d(TAG, "onClick: Going to sign in page")
            val intent = Intent(this@Sign_up, Sign_in::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }
        
        // Sign up button click listener
        binding.SignUpButton.setOnClickListener {
            val email = binding.signUpEmail.text.toString()
            val password = binding.SignUpPassword.text.toString()
            val passwordConfirm = binding.SignUpPasswordConfirm.text.toString()
            val username = binding.username.text.toString()
            val hashedPassword = sha256(password)
            if (email.isNotEmpty() && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "Creating user with email: $email and password: $password")
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        val user = result.user
                        if (user != null) {
                            Log.d(TAG, "User created with UID: ${user.uid}")
                            
                            // Save user details to Firebase Realtime Database
                            val db = FirebaseDatabase.getInstance()
                            val usersRef = db.getReference("users")
                            val userRef = usersRef.child(user.uid)
                            userRef.child("details").setValue(null)
                            userRef.child("details").child("username").setValue(username)
                            userRef.child("signedIn").setValue(true)
                            userRef.child("details").child("password").setValue(hashedPassword)
                            userRef.child("details").child("email").setValue(email)
                            userRef.child("transactions").setValue("null")
                            userRef.child("categories").setValue("null")
                            userRef.child("budgets").setValue("null")
                            userRef.child("reports").setValue("null")
                            
                            val intent = Intent(this@Sign_up, Sign_in::class.java)
                            startActivity(intent)
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            finish()
                            
                            Log.d(TAG, "Sign up successful")
                            Toast.makeText(
                                this@Sign_up,
                                "Sign up successful",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        } else {
                            Log.d(TAG, "Sign up failed")
                            Toast.makeText(
                                this@Sign_up,
                                "Sign up failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: FirebaseAuthWeakPasswordException) {
                        Log.d(TAG, "Password is too weak")
                        Toast.makeText(
                            this@Sign_up,
                            "Password is too weak",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: FirebaseAuthUserCollisionException) {
                        Log.d(TAG, "Email already in use")
                        Toast.makeText(
                            this@Sign_up,
                            "Email already in use",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.d(TAG, "Sign up failed: ${e.message}")
                        Toast.makeText(
                            this@Sign_up,
                            "Sign up failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun sha256(base: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(base.toByteArray(charset("UTF-8")))
            val hexString = StringBuffer()
            
            for (i in hash.indices) {
                val hex = Integer.toHexString(0xff and hash[i].toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            
            return hexString.toString()
        } catch (ex: java.lang.Exception) {
            throw RuntimeException(ex)
        }
    }
}