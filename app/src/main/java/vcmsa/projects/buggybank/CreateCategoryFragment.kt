package vcmsa.projects.buggybank

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

private const val TAG = "CreateCategoryFragment"

class CreateCategoryFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryNameInput: EditText
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var expenseRadioButton: RadioButton
    private lateinit var incomeRadioButton: RadioButton
    private lateinit var addCategoryButton: Button
    private val categoryList = mutableListOf(
        "Clothing", "Entertainment", "Food", "Fuel",
        "Groceries", "Health", "Housing", "Internet", "Insurance"
    )
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val view = inflater.inflate(R.layout.fragment_create_category, container, false)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)
        categoryNameInput = view.findViewById(R.id.categoryNameInput)
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup)
        expenseRadioButton = view.findViewById(R.id.expenseRadioButton)
        incomeRadioButton = view.findViewById(R.id.incomeRadioButton)
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance()
            .getReference("users").child(uid).child("categories")

        categoryAdapter = CategoryAdapter(categoryList)
        categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryRecyclerView.adapter = categoryAdapter

        addCategoryButton.setOnClickListener {
            addCategory()
            typeRadioGroup.clearCheck() //clears selection of radio buttons
        }

        // Ensure only one radio button is selected at a time
        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.expenseRadioButton -> Log.d(TAG, "Expense selected")
                R.id.incomeRadioButton -> Log.d(TAG, "Income selected")
                else -> Log.d(TAG, "No selection")
            }


        }

        
        fun addCategory() {
            Log.d(TAG, "addCategory")
            val name = categoryNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name.", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            val type = when {
                expenseRadioButton.isChecked -> "Expense"
                incomeRadioButton.isChecked -> "Income"
                else -> {
                    Toast.makeText(
                        requireContext(),
                        "Please select Expense or Income.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }

            val user = FirebaseAuth.getInstance().currentUser ?: run {
                Log.e(TAG, "No user logged in")
                return
            }

            val categoryData = mapOf("name" to name, "type" to type)

            database.child("categories").push().setValue(categoryData)
                .addOnSuccessListener {
                    Log.d(TAG, "Saved under Users/${user.uid}/Category: $categoryData")
                    Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()

                    val display = "$name ($type)"
                    categoryList.add(display)
                    categoryAdapter.notifyItemInserted(categoryList.size - 1)
                    categoryNameInput.text.clear()
                    typeRadioGroup.clearCheck()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error adding category", exception)
                    Toast.makeText(requireContext(), "Failed to add category", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun addCategory() {
        Log.d(TAG, "addCategory")
        val name = categoryNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val type = when {
            expenseRadioButton.isChecked -> "Expense"
            incomeRadioButton.isChecked -> "Income"
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Please select Expense or Income.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Log.e(TAG, "No user logged in")
            return
        }

        val categoryData = mapOf("name" to name, "type" to type)
        database.child("categories").push().setValue(categoryData)
            .addOnSuccessListener {
                Log.d(TAG, "Saved under Users/${user.uid}/Category: $categoryData")
                Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()

                val display = "$name ($type)"
                categoryList.add(display)
                categoryAdapter.notifyItemInserted(categoryList.size - 1)
                categoryNameInput.text.clear()
                typeRadioGroup.clearCheck()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error adding category", exception)
                Toast.makeText(requireContext(), "Failed to add category", Toast.LENGTH_SHORT)
                    .show()
            }
    }
    override fun onStart() {
        super.onStart()
        
        // Show this fragment's tutorial overlay once per install/user
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCreateCategoryTut = prefs.getBoolean("hasSeenCreateCategoryTut", false)
        
        if (!hasSeenCreateCategoryTut) {
            Log.d(TAG, "onStart: Launching Create Category tutorial overlay")
            
            // Build the tutorial page explaining this screen's controls
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.lacalm1,  // Replace with your appropriate drawable
                // Explanatory text for creating a category:
                "This is the Create Category screen./n" +
                        "1. Type a name into 'Category Name'./n" +
                        "2. Select whether it’s an Expense or Income using the radio buttons./n" +
                        "3. Tap 'Add Category' to save it to your account and see it appear below./n" +
                        "4. Your new category will now be listed, so you can use it when logging transactions."
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
}