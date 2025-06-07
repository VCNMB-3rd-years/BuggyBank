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
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
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
    private lateinit var googleSignInClient: GoogleSignInClient
    
    companion object {
        private const val RC_SIGN_IN = 9001
    }
    
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
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(true)
            .build()
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@Sign_in, Sign_up::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                finish()
            }
        })
        
        binding.SignInButton.setOnClickListener {
            signInUser()
        }
        
        binding.SignInRegister.setOnClickListener {
            startActivity(Intent(this, Sign_up::class.java))
            finish()
        }
        
        binding.vForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
        
        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }
    
    private fun signInUser() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val email = binding.SignInEmail.text.toString().trim()
                val password = binding.SignInPassword.text.toString().trim()
                auth.signInWithEmailAndPassword(email, password).await()
                
                val userId = auth.currentUser?.uid ?: throw Exception("User ID is null after sign-in")
                val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                dbRef.child("signedIn").setValue(true).await()
                
                withContext(Dispatchers.Main) {
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
    
    
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                auth.signInWithCredential(credential).await()
                
                val userId = auth.currentUser?.uid ?: throw Exception("User ID is null after Google sign-in")
                val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                dbRef.child("signedIn").setValue(true).await()
                
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@Sign_in, MenuBar::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "firebaseAuthWithGoogle: Google sign-in failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Sign_in, "Google Sign-In failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(currentUser.uid).child("signedIn")
        
        dbRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.exists() && task.result.getValue(Boolean::class.java) == true) {
                startActivity(Intent(this, MenuBar::class.java))
                finish()
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }
}
