package vcmsa.projects.buggybank

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.fragment.app.DialogFragment



class logoutFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_logout, container, false)

        val btnCancel: Button = view.findViewById(R.id.cancelBtn)
        btnCancel.setOnClickListener {
          requireActivity().supportFragmentManager.popBackStack()
        }

        val btnLogout: Button = view.findViewById(R.id.btnlogout)
        btnLogout.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: throw Exception("User ID is null after sign-in")
            val dbRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
            dbRef.child(userId).child("signedIn").setValue(false)
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, Sign_in::class.java)
            startActivity(intent)
            dismiss()
        }

        return view
    }
    
    
}