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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        imagePreview = view.findViewById(R.id.imgProfile)
        imagePreview?.setOnClickListener {
            val options = arrayOf("Camera", "Gallery")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Image")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            Log.d("FragProfile", "Camera selected")
                            requestCameraPermission()
                        }
                        1 -> {
                            Log.d("FragProfile", "Gallery selected")
                            requestGalleryPermission()
                        }
                    }
                }
                .show()
        }
        btnSave = view.findViewById(R.id.btnSave)
        btnSave?.setOnClickListener {
            val name = nameTextInputLayout?.editText?.text.toString()
            val email = emailTextInputLayout?.editText?.text.toString()
            val age = ageTextInputLayout?.editText?.text.toString()
            val phone = phoneTextInputLayout?.editText?.text.toString()
            val surname = surnameTextInputLayout?.editText?.text.toString()
            val address = addressTextInputLayout?.editText?.text.toString()
            
            Log.d("FragProfile", "Save button clicked")
            Log.d("FragProfile", "Name: $name")
            Log.d("FragProfile", "Email: $email")
            Log.d("FragProfile", "Age: $age")
            Log.d("FragProfile", "Phone: $phone")
            Log.d("FragProfile", "Surname: $surname")
            Log.d("FragProfile", "Address: $address")
            Log.d("FragProfile", "Profile image URI: $profileImageUri")
            updateDetails(name, email, age, phone, surname, address)
        }
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val name = dataSnapshot.child("username").value as String
                            val email = dataSnapshot.child("email").value as String
                            val age = dataSnapshot.child("age").value as String
                            val phone = dataSnapshot.child("phone").value as String
                            val surname = dataSnapshot.child("surname").value as String
                            val address = dataSnapshot.child("address").value as String

                            phoneTextInputLayout?.editText?.setText(phone)
                            surnameTextInputLayout?.editText?.setText(surname)
                            addressTextInputLayout?.editText?.setText(address)
                            nameTextInputLayout?.editText?.setText(name)
                            emailTextInputLayout?.editText?.setText(email)
                            ageTextInputLayout?.editText?.setText(age)
                        } else {
                            val nodata = "No Data"
                            phoneTextInputLayout?.editText?.setText(nodata)
                            surnameTextInputLayout?.editText?.setText(nodata)
                            addressTextInputLayout?.editText?.setText(nodata)
                            nameTextInputLayout?.editText?.setText(nodata)
                            emailTextInputLayout?.editText?.setText(nodata)
                            ageTextInputLayout?.editText?.setText(nodata)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Error", error.message)
                    }
                })
        }
    }

    private fun updateDetails(name: String, email: String, age: String, phone: String, surname: String, address: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .child("username").setValue(name)
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .child("email").setValue(email)
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .child("age").setValue(age)
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .child("phone").setValue(phone)
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .child("surname").setValue(surname)
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("details")
                .child("address").setValue(address)

            if (profileImageUri != null) {
                val storageReference = FirebaseStorage.getInstance().reference.child("images/$uid")
                storageReference.putFile(profileImageUri!!)
                    .addOnSuccessListener {
                        Log.d("FragProfile", "Image uploaded to Firebase Storage")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FragProfile", "Error uploading image to Firebase Storage", e)
                    }
            }
        }
    }
    

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        } else {
            takePictureWithCamera()
        }
    }

    private fun requestGalleryPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                102
            )
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
            val imageBitmap = data?.extras?.get("data") as Bitmap
            profileImageUri = data?.data
            imagePreview?.setImageBitmap(imageBitmap)
        } else if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            profileImageUri = data?.data
            imagePreview?.setImageURI(profileImageUri)
        }
    }
}