package vcmsa.projects.buggybank

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CreateTransactionFragment : Fragment() {
    
    private lateinit var etTitle: EditText
    private lateinit var spType: Spinner
    private lateinit var etAmount: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spPayment: Spinner
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnAddImage: FrameLayout
    private lateinit var imagePreview: ImageView
    private var imageUri: Uri? = null

    private val storage = Firebase.storage.reference
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val filename = UUID.randomUUID().toString()
                val imageRef = storage.child("images/$filename")
                imageRef.putFile(it).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        imageRef.downloadUrl.addOnCompleteListener { uri ->
                            imageUri = uri.result
                            imagePreview.setImageURI(imageUri)
                        }
                    }
                }
            }
        }
    override fun onStart() {
        super.onStart()
        
        // Show this fragment's tutorial overlay once per install/user
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCreateCategoryTut = prefs.getBoolean("hasSeenCreateCategoryTut", false)
        
        if (!hasSeenCreateCategoryTut) {
            Log.d(ContentValues.TAG, "onStart: Launching Create Category tutorial overlay")
            
            // Build the tutorial page explaining this screen's controls
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.wap,  // Replace with your appropriate drawable
                // Explanatory text for creating a category:
                "If you wanna create a new Transaction, you can do so here./n" +
                        "1. Enter the needed information into the fields./n" +
                        "2. press 'Add Transaction'./n" +
                        "3. Dont forget to add a receipt image./n" +
                        "4. and boom you have a new Transaction."
            )
            
            // Overlay the tutorial on top of the existing fragmentContainerView
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay)
                .commit()
            
            // Mark as seen so it won't show next time
            prefs.edit()
                .putBoolean("hasSeenCreateCategoryTut", true)
                .apply()
        }
    }
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imageUri != null) {
                imagePreview.setImageURI(imageUri)
                val filename = UUID.randomUUID().toString()
                val imageRef = storage.child("images/$filename")
                imageRef.putFile(imageUri!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        imageRef.downloadUrl.addOnCompleteListener { uri ->
                            imageUri = uri.result
                        }
                    }
                }
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_create_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etTitle = view.findViewById(R.id.etTitle)
        spType = view.findViewById(R.id.spType)
        etAmount = view.findViewById(R.id.etAmount)
        spCategory = view.findViewById(R.id.spCategory)
        spPayment = view.findViewById(R.id.spPayment)
        etDate = view.findViewById(R.id.etDate)
        etStartTime = view.findViewById(R.id.etStartTime)
        etEndTime = view.findViewById(R.id.etEndTime)
        etDescription = view.findViewById(R.id.editTextDescription)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        imagePreview = view.findViewById(R.id.imagePreview)

        listOf(etDate, etStartTime, etEndTime).forEach {
            it.isFocusable = false
            it.isClickable = true
        }

        spType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Select", "Expense", "Income")
        )


        spCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Select","Clothing", "Entertainment", "Food", "Fuel", "Groceries", "Health", "Housing", "Internet", "Insurance", "Salary", "Wages", "Investments")
        )

        spPayment.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Select","Cash", "Credit Card", "Debit Card")
        )

        etDate.setOnClickListener { showDatePicker(etDate) }
        etStartTime.setOnClickListener { showTimePicker(etStartTime) }
        etEndTime.setOnClickListener { showTimePicker(etEndTime) }
        btnAddImage.setOnClickListener { showImagePickerDialog() }

        btnAdd.setOnClickListener {
            saveTransaction()
        }
    }

    private fun saveTransaction() {
        val title = etTitle.text.toString().trim()
        val type = spType.selectedItem as String
        val rawAmount = etAmount.text.toString()
        val parsedAmount = try {
            NumberFormat.getInstance(Locale.US).parse(rawAmount)?.toDouble() ?: 0.0
        } catch (e: ParseException) {
            Toast.makeText(requireContext(), "Invalid amount format", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = parsedAmount

        val category = spCategory.selectedItem as String
        val payment = spPayment.selectedItem as String
        val dateOfTransaction = etDate.text.toString()
        val start = etStartTime.text.toString()
        val end = etEndTime.text.toString()
        val desc = etDescription.text.toString().trim()

        if (category == "Select" || payment == "Select" || type == "Select") {
            Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty() || amount <= 0.0 || dateOfTransaction.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill Title, Amount & Date", Toast.LENGTH_SHORT).show()
            return
        }
        val transaction = Expense(title, type, amount, category, payment, dateOfTransaction, start, end, desc, imageUri?.toString())

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("transactions")
        val newTransactionId = dbRef.push().key

        if (newTransactionId != null) {
            dbRef.child(newTransactionId).setValue(transaction)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show()
                    clearForm()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save transaction", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                cal.set(y, m, d)
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                target.setText(fmt.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(target: EditText) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, h, min -> target.setText(String.format("%02d:%02d", h, min)) },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Check for camera permission
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                requireContext(),
                                android.Manifest.permission.CAMERA
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        } else {
                            takePhoto()
                        }
                    }
                    1 -> pickFromGallery()
                }
            }.show()
    }


    private fun pickFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile?.let {
            imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )

            cameraLauncher.launch(imageUri)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(requireContext().filesDir, "Receipt_images")
            if (!storageDir.exists()) storageDir.mkdirs()

            val photoFile = File.createTempFile("IMG_$timestamp", ".jpg", storageDir)

            Log.d("DEBUG", "File exists: ${photoFile.exists()} at ${photoFile.absolutePath}")

            photoFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun clearForm() {
        etTitle.text.clear()
        etAmount.text.clear()
        etDate.text.clear()
        etStartTime.text.clear()
        etEndTime.text.clear()
        etDescription.text.clear()
        spType.setSelection(0)
        spCategory.setSelection(0)
        spPayment.setSelection(0)
        imageUri = null
        imagePreview.setImageDrawable(null)
    }
}