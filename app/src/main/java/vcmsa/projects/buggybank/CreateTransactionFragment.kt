package vcmsa.projects.buggybank

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
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

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            imagePreview.setImageURI(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            imagePreview.setImageURI(imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_create_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
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

        //for stepTwo's input prevention
        val btnNextToTwo = view.findViewById<Button>(R.id.btnNextToTwo)
        val layoutStepTwo = view.findViewById<LinearLayout>(R.id.layoutStepTwo)

        //for stepThree's input prevention
        val btnNextToThree = view.findViewById<Button>(R.id.btnNextToThree)
        val layoutStepThree = view.findViewById<LinearLayout>(R.id.layoutStepThree)

        //back to stepOne and stepTwo
        val btnBackToOne = view.findViewById<Button>(R.id.btnBackToOne)
        val btnBackToTwo = view.findViewById<Button>(R.id.BackToTwo)

        // Make date/time fields non-editable
        listOf(etDate, etStartTime, etEndTime).forEach {
            it.isFocusable = false
            it.isClickable = true
        }

        // Setup spinners
        val types = listOf("Select", "Income", "Expense")
        val typeAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            types
        ) {
            override fun isEnabled(position: Int): Boolean = position != 0 // Disable "Select type"

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(if (position == 0) android.graphics.Color.GRAY else android.graphics.Color.BLACK)
                return view
            }
        }
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spType.adapter = typeAdapter


        val paymentOptions = listOf("Select", "Cash", "Credit Card", "Debit Card")
        val paymentAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            paymentOptions
        ) {
            override fun isEnabled(position: Int) = position != 0
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(
                    if (position == 0) android.graphics.Color.GRAY else android.graphics.Color.BLACK
                )
                return view
            }
        }
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPayment.adapter = paymentAdapter


        loadCategoriesFromFirebase()

        // Set listeners
        etDate.setOnClickListener { showDatePicker(etDate) }
        etStartTime.setOnClickListener { showTimePicker(etStartTime) }
        etEndTime.setOnClickListener { showTimePicker(etEndTime) }
        btnAddImage.setOnClickListener { showImagePickerDialog() }
        btnAdd.setOnClickListener { saveTransaction() }

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        val txtStepOne = view.findViewById<TextView>(R.id.txtStepOne)
        val txtStepTwo = view.findViewById<TextView>(R.id.txtStepTwo)
        val txtStepThree = view.findViewById<TextView>(R.id.txtStepThree)

        btnBackToOne.setOnClickListener {
            scrollView.post {
                scrollView.smoothScrollTo(0, txtStepOne.top)
            }
        }

        btnBackToTwo.setOnClickListener {
            scrollView.post {
                scrollView.smoothScrollTo(0, txtStepTwo.top)
            }
        }

        btnNextToTwo.setOnClickListener {
            val titleText = etTitle.text.toString()
            val amountText = etAmount.text.toString()
            val typePosition = spType.selectedItemPosition
            val categoryPosition = spCategory.selectedItemPosition

            var isValid = true

// Colors
            val defaultColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            val errorColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)

// Reset to default first
            etTitle.background.setTint(defaultColor)
            etAmount.background.setTint(defaultColor)
            spType.background.setTint(defaultColor)
            spCategory.background.setTint(defaultColor)

// Validate each field
            if (titleText.isEmpty()) {
                etTitle.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Title field is empty", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (amountText.isEmpty()) {
                etAmount.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Amount field is empty", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (typePosition == 0) {
                spType.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Please select a transaction type", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (categoryPosition == 0) {
                spCategory.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                isValid = false
            }

// If all fields valid, show next step
            if (isValid) {
                layoutStepTwo.visibility = View.VISIBLE
                layoutStepTwo.alpha = 1f
                layoutStepTwo.isEnabled = true
                layoutStepTwo.isClickable = true
                layoutStepTwo.isFocusable = true

                scrollView.post {
                    scrollView.smoothScrollTo(0, txtStepTwo.top)
                }
            } else {
                scrollView.post {
                    scrollView.smoothScrollTo(0, txtStepOne.top)
                }
            }
        }



//these step is optional
        btnNextToThree.setOnClickListener {
            val dateText = etDate.text.toString()
            val startTimeText = etStartTime.text.toString()
            val endTimeText = etEndTime.text.toString()
            val paymentSelected = spPayment.selectedItemPosition != 0

            var isValid = true

// Reset background tints to default (optional grey)
            val defaultColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            val errorColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)

            etDate.background.setTint(defaultColor)
            etStartTime.background.setTint(defaultColor)
            etEndTime.background.setTint(defaultColor)
            spPayment.background.setTint(defaultColor)

// Validate each field and apply red tint if invalid
            if (dateText.isEmpty()) {
                etDate.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Date field is empty", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (startTimeText.isEmpty()) {
                etStartTime.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Start Time field is empty", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (endTimeText.isEmpty()) {
                etEndTime.background.setTint(errorColor)
                Toast.makeText(requireContext(), "End Time field is empty", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (!paymentSelected) {
                spPayment.background.setTint(errorColor)
                Toast.makeText(requireContext(), "Please select a payment method", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (isValid) {
                layoutStepThree.visibility = View.VISIBLE
                layoutStepThree.alpha = 1f
                layoutStepThree.isEnabled = true
                layoutStepThree.isClickable = true
                layoutStepThree.isFocusable = true

                scrollView.post {
                    scrollView.smoothScrollTo(0, txtStepThree.top)
                }
            } else {
                scrollView.post {
                    scrollView.smoothScrollTo(0, txtStepTwo.top)
                }
            }
        }
    }

    private fun loadCategoriesFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("categories")

        dbRef.get().addOnSuccessListener { snapshot ->
            val categoryList = mutableListOf<String>()
            for (categorySnap in snapshot.children) {
                val categoryName = categorySnap.child("name").getValue(String::class.java)
                categoryName?.let { categoryList.add(it) }
            }

            if (categoryList.isNotEmpty()) {
                val categoryOptions = listOf("Select") + categoryList
                val categoryAdapter = object : ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categoryOptions
                ) {
                    override fun isEnabled(position: Int) = position != 0
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        (view as TextView).setTextColor(
                            if (position == 0) android.graphics.Color.GRAY else android.graphics.Color.BLACK
                        )
                        return view
                    }
                }
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spCategory.adapter = categoryAdapter
            } else {
                spCategory.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("No categories available")
                )
            }

        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTransaction() {
        val title = etTitle.text.toString().trim()
        val type = spType.selectedItem?.toString() ?: "Select type"
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val category = spCategory.selectedItem?.toString() ?: ""
        val payment = spPayment.selectedItem?.toString() ?: ""
        val date = etDate.text.toString()
        val start = etStartTime.text.toString()
        val end = etEndTime.text.toString()
        val desc = etDescription.text.toString().trim()

        if (title.isEmpty() || amount <= 0.0 || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill Title, Amount & Date", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Expense(
            title = title,
            type = type,
            amount = amount,
            category = category,
            paymentMethod = payment,
            date = date,
            startTime = start,
            endTime = end,
            description = desc,
            imagePath = imageUri?.toString()
        )

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        // 🔍 Check for budget if type is Expense
        if (type == "Expense") {
            userRef.child("budgets").orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(budgetSnapshot: DataSnapshot) {
                        if (budgetSnapshot.exists()) {
                            val budgetAmount = budgetSnapshot.children.first()
                                .child("amount").getValue(Int::class.java) ?: 0

                            // Get total spent in this category
                            userRef.child("transactions").orderByChild("category").equalTo(category)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(transactionSnapshot: DataSnapshot) {
                                        var totalSpent = 0.0
                                        for (txn in transactionSnapshot.children) {
                                            val txnType = txn.child("type").getValue(String::class.java)
                                            val txnAmount = txn.child("amount").getValue(Double::class.java) ?: 0.0
                                            if (txnType == "Expense") {
                                                totalSpent += txnAmount
                                            }
                                        }

                                        val newTotal = totalSpent + amount
                                        if (newTotal > budgetAmount) {
                                            Toast.makeText(requireContext(),
                                                "⚠️ This transaction will exceed your budget of R$budgetAmount for '$category'. Total after this: R$newTotal",
                                                Toast.LENGTH_LONG).show()
                                            // Optionally return to block save:
                                            // return
                                        }

                                        // Proceed to save transaction
                                        saveTransactionToFirebase(userRef, transaction)
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(requireContext(), "Error checking transactions", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            // No budget set — just save the transaction
                            saveTransactionToFirebase(userRef, transaction)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error checking budget", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // Not an expense — just save it
            saveTransactionToFirebase(userRef, transaction)
        }
    }

    private fun saveTransactionToFirebase(userRef: DatabaseReference, transaction: Expense) {
        val newTransactionId = userRef.child("transactions").push().key
        if (newTransactionId != null) {
            userRef.child("transactions").child(newTransactionId).setValue(transaction)
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
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                target.setText(format.format(cal.time))
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
            { _, hour, minute ->
                target.setText(String.format("%02d:%02d", hour, minute))
            },
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
                    0 -> takePhotoFromCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun takePhotoFromCamera() {
        val photoFile = createImageFile() ?: return
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(imageUri)
    }

    private fun createImageFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "JPEG_$timestamp"
            val storageDir = requireContext().cacheDir
            File.createTempFile(fileName, ".jpg", storageDir)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to create image file", Toast.LENGTH_SHORT).show()
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

    
    override fun onStart() {
        super.onStart()
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCalcTut = prefs.getBoolean("hasSeenCalcTut", false)
        
        if (!hasSeenCalcTut) {
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.butterfly, // Replace with a valid drawable in your project
                "This is the transaction screen!/n" +
                        "Add the transaction details here./n" +
                        "Press 'Next' to continue and if you missed a spot you can always press 'Back' to go back ./n" +
                        "• Tap OK to begin!"
            )
            
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay) // ensure this ID matches your layout
                .commit()
            
            prefs.edit().putBoolean("hasSeenCalcTut", true).apply()
        }
    }
}

}

