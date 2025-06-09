package vcmsa.projects.buggybank

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class FragProfile : Fragment() {
    
    private var nameTextInputLayout: TextInputLayout? = null
    private var emailTextInputLayout: TextInputLayout? = null
    private var ageTextInputLayout: TextInputLayout? = null
    private var phoneTextInputLayout: TextInputLayout? = null
    private var surnameTextInputLayout: TextInputLayout? = null
    private var addressTextInputLayout: TextInputLayout? = null
    private var btnSave: Button? = null
    private var imagePreview: ShapeableImageView? = null
    private var profileImageUri: Uri? = null
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_frag_profile, container, false)
        nameTextInputLayout = view.findViewById(R.id.txtName)
        emailTextInputLayout = view.findViewById(R.id.txtProfileEmail)
        ageTextInputLayout = view.findViewById(R.id.txtBirthday)
        phoneTextInputLayout = view.findViewById(R.id.txtPhone)
        surnameTextInputLayout = view.findViewById(R.id.txtSurname)
        addressTextInputLayout = view.findViewById(R.id.txtAddress)
        btnSave = view.findViewById(R.id.btnSave)
        imagePreview = view.findViewById(R.id.imgProfile)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
        emailTextInputLayout?.editText?.apply {
            setText(currentEmail)
            isEnabled = false
            isFocusable = false
        }
        
        imagePreview?.setOnClickListener {
            val options = arrayOf("Camera", "Gallery")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Image")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> requestCameraPermission()
                        1 -> requestGalleryPermission()
                    }
                }.show()
        }
        
        btnSave?.setOnClickListener {
            val name = nameTextInputLayout?.editText?.text.toString()
            val age = ageTextInputLayout?.editText?.text.toString()
            val phone = phoneTextInputLayout?.editText?.text.toString()
            val surname = surnameTextInputLayout?.editText?.text.toString()
            val address = addressTextInputLayout?.editText?.text.toString()
            
            updateDetails(name, age, phone, surname, address)
        }
        
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            nameTextInputLayout?.editText?.setText(dataSnapshot.child("username").value as? String ?: "")
                            ageTextInputLayout?.editText?.setText(dataSnapshot.child("age").value as? String ?: "")
                            phoneTextInputLayout?.editText?.setText(dataSnapshot.child("phone").value as? String ?: "")
                            surnameTextInputLayout?.editText?.setText(dataSnapshot.child("surname").value as? String ?: "")
                            addressTextInputLayout?.editText?.setText(dataSnapshot.child("address").value as? String ?: "")
                        } else {
                            nameTextInputLayout?.editText?.setText("No Data")
                            ageTextInputLayout?.editText?.setText("No Data")
                            phoneTextInputLayout?.editText?.setText("No Data")
                            surnameTextInputLayout?.editText?.setText("No Data")
                            addressTextInputLayout?.editText?.setText("No Data")
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FragProfile", "Database error: ${error.message}")
                    }
                })
        }
    }
    
    private fun updateDetails(name: String, age: String, phone: String, surname: String, address: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
            userRef.child("username").setValue(name)
            userRef.child("age").setValue(age)
            userRef.child("phone").setValue(phone)
            userRef.child("surname").setValue(surname)
            userRef.child("address").setValue(address)
            
            if (profileImageUri != null) {
                val storageReference = FirebaseStorage.getInstance().reference.child("images/$uid")
                storageReference.putFile(profileImageUri!!)
                    .addOnSuccessListener {
                        Log.d("FragProfile", "Image uploaded successfully")
                    }
                    .addOnFailureListener {
                        Log.e("FragProfile", "Image upload failed", it)
                    }
            }
        }
    }
    
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            takePictureWithCamera()
        }
    }
    
    private fun requestGalleryPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 102)
        } else {
            selectImageFromGallery()
        }
    }
    
    private fun takePictureWithCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 100)
    }
    
    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            profileImageUri = data?.data
            imagePreview?.setImageBitmap(imageBitmap)
        } else if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            profileImageUri = data?.data
            imagePreview?.setImageURI(profileImageUri)
        }
    }
}
