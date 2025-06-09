// Sign_up.kt
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

class Sign_up : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@Sign_up, Sign_in::class.java))
                finish()
            }
        })
        
        binding.SignUpLogin.setOnClickListener {
            startActivity(Intent(this@Sign_up, Sign_in::class.java))
            finish()
        }
        
        binding.SignUpButton.setOnClickListener {
            val email = binding.signUpEmail.text.toString().trim()
            val password = binding.SignUpPassword.text.toString().trim()
            val confirm = binding.SignUpPasswordConfirm.text.toString().trim()
            val username = binding.username.text.toString().trim()
            val hashedPassword = sha256(password)
            
            if (email.isNotEmpty() && password.isNotEmpty() && confirm == password) {
                lifecycleScope.launch {
                    try {
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        val user = result.user ?: throw Exception("User is null")
                        
                        user.sendEmailVerification().await()
                        
                        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
                        dbRef.child("details").child("username").setValue(username)
                        dbRef.child("details").child("password").setValue(hashedPassword)
                        dbRef.child("details").child("email").setValue(email)
                        dbRef.child("signedIn").setValue(false)
                        dbRef.child("transactions").setValue("null")
                        dbRef.child("categories").setValue("null")
                        dbRef.child("budgets").setValue("null")
                        dbRef.child("reports").setValue("null")
                        
                        Toast.makeText(this@Sign_up, "Verification email sent. Please verify before login.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@Sign_up, Sign_in::class.java))
                        finish()
                        
                    } catch (e: FirebaseAuthWeakPasswordException) {
                        Toast.makeText(this@Sign_up, "Password is too weak", Toast.LENGTH_SHORT).show()
                    } catch (e: FirebaseAuthUserCollisionException) {
                        Toast.makeText(this@Sign_up, "Email already in use", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@Sign_up, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please check inputs", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun sha256(base: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(base.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
