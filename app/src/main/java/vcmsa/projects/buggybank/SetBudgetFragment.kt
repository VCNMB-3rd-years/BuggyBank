package vcmsa.projects.buggybank

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SetBudgetFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var txtSeekValue: TextView
    private lateinit var txtSelectedCategory: TextView
    private lateinit var btnSet: Button
    private lateinit var layoutCategoryButtons: LinearLayout

    private var selectedCategoryId: String? = null
    private var selectedCategoryName: String? = null
    private var selectedCategoryType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_set_budget, container, false)

        seekBar = view.findViewById(R.id.seekBarMin)
        txtSeekValue = view.findViewById(R.id.txtSeekValue)
        txtSelectedCategory = view.findViewById(R.id.txtHeadingChange)
        btnSet = view.findViewById(R.id.btnSet)
        layoutCategoryButtons = view.findViewById(R.id.layoutCategoryButtons)

        val maxValueEditText = view.findViewById<TextInputEditText>(R.id.etMaxValueInput)
        val value = maxValueEditText.text.toString()
        val btnSetMax: Button = view.findViewById(R.id.btnSetMax)

        btnSetMax.setOnClickListener {
            val input = maxValueEditText.text.toString()
            val max = input.toIntOrNull()
            if (max != null && max > 0) {
                seekBar.max = max
                Toast.makeText(context, "Max budget set to R$max", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                txtSeekValue.text = "R$${progress}"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnSet.setOnClickListener {
            saveBudgetToFirebase()
            maxValueEditText.text?.clear()
            seekBar.progress = 0
        }

        loadCategoriesFromFirebase()

        return view
    }

    private fun loadCategoriesFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("categories")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                layoutCategoryButtons.removeAllViews()
                val categories = mutableListOf<Triple<String, String, String>>()

                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: continue
                    val categoryName = categorySnapshot.child("name").getValue(String::class.java) ?: continue
                    val categoryType = categorySnapshot.child("type").getValue(String::class.java) ?: "Expense"
                    categories.add(Triple(categoryId, categoryName, categoryType))
                }

                categories.sortBy { it.second.lowercase() }

                for ((categoryId, categoryName, categoryType) in categories) {
                    val button = Button(requireContext())
                    button.text = categoryName
                    button.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(8, 8, 8, 8)
                    }

                    button.setOnClickListener {
                        selectedCategoryId = categoryId
                        selectedCategoryName = categoryName
                        selectedCategoryType = categoryType
                        txtSelectedCategory.text = categoryName

                        // Highlight selected
                        for (i in 0 until layoutCategoryButtons.childCount) {
                            val otherBtn = layoutCategoryButtons.getChildAt(i) as Button
                            otherBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                        }
                        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                    }

                    layoutCategoryButtons.addView(button)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveBudgetToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val categoryName = selectedCategoryName ?: run {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        val categoryType = selectedCategoryType
        val amount = seekBar.progress

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budgets")

        dbRef.orderByChild("category").equalTo(categoryName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(context, "Budget for this category already exists.", Toast.LENGTH_SHORT).show()
                    } else {
                        val budgetId = dbRef.push().key ?: return
                        val budgetData = mapOf(
                            "id" to budgetId,
                            "category" to categoryName,
                            "categoryType" to categoryType,
                            "amount" to amount
                        )

                        dbRef.child(budgetId).setValue(budgetData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Budget set successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to save budget", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show()
                }
            })
    }
    override fun onStart() {
        super.onStart()
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCalcTut = prefs.getBoolean("hasSeenCalcTut", false)
        
        if (!hasSeenCalcTut) {
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.anti, // Replace with a valid drawable in your project
                "i remember my mother telling me 'Their is Mac Donald's at the Colony' haa good times\n" +
                        "You can set your budgets here.\n" +
                        "Select a category/n" + "Set an amount\n" +
                        "You define the exact amount to allocate by dragging the slider and clicking 'Define'\n" +
                        "• Tap OK to begin!"
            )
            
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay) // ensure this ID matches your layout
                .commit()
            
            prefs.edit().putBoolean("hasSeenCalcTut", true).apply()
        }
    }

}
