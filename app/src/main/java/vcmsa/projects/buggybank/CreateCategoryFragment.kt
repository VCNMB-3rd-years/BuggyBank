package vcmsa.projects.buggybank

import android.app.AlertDialog
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Fragment for creating a new category.
 *
 * This fragment will allow the user to input a name for the category and select whether it is an expense or income.
 * The category will be saved to the Firebase Realtime Database under the current user's UID.
 */
private const val TAG = "CreateCategoryFragment"

class CreateCategoryFragment : Fragment() {
    
    /**
     * The database reference for the categories.
     */
    private lateinit var database: DatabaseReference
    
    /**
     * The RecyclerView for displaying the list of categories.
     */
    private lateinit var categoryRecyclerView: RecyclerView
    
    /**
     * The EditText for inputting the name of the category.
     */
    private lateinit var categoryNameInput: EditText
    
    /**
     * The RadioGroup for selecting whether the category is an expense or income.
     */
    private lateinit var typeRadioGroup: RadioGroup
    
    /**
     * The RadioButton for selecting expense.
     */
    private lateinit var expenseRadioButton: RadioButton
    
    /**
     * The RadioButton for selecting income.
     */
    private lateinit var incomeRadioButton: RadioButton
    
    /**
     * The Button for adding the category.
     */
    private lateinit var addCategoryButton: Button
    
    /**
     * The list of categories.
     */
    private val categoryList = mutableListOf<Category>()
    
    
    /**
     * The adapter for the RecyclerView.
     */
    private lateinit var categoryAdapter: CategoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val view = inflater.inflate(R.layout.fragment_create_category, container, false)
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.categoryNameInput)
        categoryNameInput = inputLayout.editText as EditText
        
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup)
        expenseRadioButton = view.findViewById(R.id.expenseRadioButton)
        incomeRadioButton = view.findViewById(R.id.incomeRadioButton)
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        return view
    }
    override fun onStart() {
        super.onStart()
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCalcTut = prefs.getBoolean("hasSeenCalcTut", false)
        
        if (!hasSeenCalcTut) {
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.lacalm1, // Replace with a valid drawable in your project
                "Movies, TV Shows, and Music/n" +
                        "Add new categories to help organize your transactions./n" +
                        "selected income or expense before hitting done./n" +
                        "• Tap OK to begin! PS you watch Dune and done to add that to BuggyBank"
            )
            
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay) // ensure this ID matches your layout
                .commit()
            
            prefs.edit().putBoolean("hasSeenCalcTut", true).apply()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        
        // Initialize user-scoped database reference
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance()
            .getReference("users").child(uid).child("categories")
        
        // Setup RecyclerView and Adapter
        categoryAdapter = CategoryAdapter(categoryList,
            onEdit = { category -> showEditDialog(category) },
            onDelete = { category -> deleteCategory(category) }
        )
        
        categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryRecyclerView.adapter = categoryAdapter
        
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val category = categoryAdapter.getItemAt(position)
                
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    categoryAdapter.notifyItemChanged(position)
                    return
                }
                
                val transactionsRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(uid).child("transactions")
                
                // Check if any transaction uses this category
                transactionsRef.orderByChild("category").equalTo(category.name)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // Category is in use
                                Toast.makeText(requireContext(), "Cannot delete: Category is used in transactions", Toast.LENGTH_LONG).show()
                                categoryAdapter.notifyItemChanged(position)
                            } else {
                                // Show confirmation dialog
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Category")
                                    .setMessage("Are you sure you want to delete \"${category.name}\"?")
                                    .setPositiveButton("Delete") { _, _ ->
                                        deleteCategory(category)
                                    }
                                    .setNegativeButton("Cancel") { _, _ ->
                                        categoryAdapter.notifyItemChanged(position)
                                    }
                                    .show()
                            }
                        }
                        
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error checking transactions", Toast.LENGTH_SHORT).show()
                            categoryAdapter.notifyItemChanged(position)
                        }
                    })
            }
        })
        itemTouchHelper.attachToRecyclerView(categoryRecyclerView)
        
        
        // Load categories from Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val type = child.child("type").getValue(String::class.java) ?: continue
                    categoryList.add(Category(id, name, type))
                }
                
                // 🔽 Sort alphabetically by name (case-insensitive)
                categoryList.sortBy { it.name.lowercase() }
                
                categoryAdapter.notifyDataSetChanged()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
        
        
        // Add category on button click
        addCategoryButton.setOnClickListener { addCategory() }
    }
    
    
    
    /**
     * Adds a new category to the database.
     */
    private fun addCategory() {
        Log.d(TAG, "addCategory")
        val name = categoryNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val type = when {
            expenseRadioButton.isChecked -> "Expense"
            incomeRadioButton.isChecked -> "Income"
            else -> {
                Toast.makeText(requireContext(), "Please select Expense or Income.", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Log.e(TAG, "No user logged in")
            return
        }
        
        // Check for duplicates
        database.orderByChild("name").equalTo(name).get().addOnSuccessListener { snapshot ->
            var duplicateFound = false
            
            snapshot.children.forEach { child ->
                val existingType = child.child("type").getValue(String::class.java)
                if (existingType == type) {
                    duplicateFound = true
                    return@forEach
                }
            }
            
            if (duplicateFound) {
                Toast.makeText(requireContext(), "This category already exists.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            
            // No duplicate, proceed to add
            val categoryData = mapOf("name" to name, "type" to type)
            database.push().setValue(categoryData)
                .addOnSuccessListener {
                    Log.d(TAG, "Saved: $categoryData")
                    Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()
                    
                    // No need to manually add to categoryList — Firebase listener will do it
                    categoryNameInput.text.clear()
                    typeRadioGroup.clearCheck()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error adding category", exception)
                    Toast.makeText(requireContext(), "Failed to add category", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to check duplicates", exception)
            Toast.makeText(requireContext(), "Error checking for duplicates", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEditDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editCategoryNameInput)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.editTypeRadioGroup)
        val incomeRadio = dialogView.findViewById<RadioButton>(R.id.editIncomeRadioButton)
        val expenseRadio = dialogView.findViewById<RadioButton>(R.id.editExpenseRadioButton)
        
        nameInput.setText(category.name)
        if (category.type == "Income") incomeRadio.isChecked = true else expenseRadio.isChecked = true
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newType = when {
                    incomeRadio.isChecked -> "Income"
                    expenseRadio.isChecked -> "Expense"
                    else -> ""
                }
                
                if (newName.isEmpty() || newType.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter valid data", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val updatedCategory = mapOf("name" to newName, "type" to newType)
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                val categoryRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(uid).child("categories").child(category.id)
                
                categoryRef.updateChildren(updatedCategory)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Category updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    
    private fun deleteCategory(category: Category) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        val transactionsRef = FirebaseDatabase.getInstance()
            .getReference("users").child(uid).child("transactions")
        
        // Check if category is in use
        transactionsRef.orderByChild("category").equalTo(category.name)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Category is in use
                        Toast.makeText(requireContext(), "Cannot delete category. It is used in existing transactions.", Toast.LENGTH_LONG).show()
                    } else {
                        // Not in use, proceed to confirm delete
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Category")
                            .setMessage("Are you sure you want to delete the category \"${category.name}\"?")
                            .setPositiveButton("Delete") { _, _ ->
                                val categoryRef = FirebaseDatabase.getInstance()
                                    .getReference("users").child(uid).child("categories").child(category.id)
                                
                                categoryRef.removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Failed to delete category", Toast.LENGTH_SHORT).show()
                                        Log.e(TAG, "Delete failed", e)
                                    }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error checking transactions", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Database error", error.toException())
                }
            })
    }
}