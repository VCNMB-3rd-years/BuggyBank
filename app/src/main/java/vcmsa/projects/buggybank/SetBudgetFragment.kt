package vcmsa.projects.buggybank

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import vcmsa.projects.buggybank.databinding.FragmentSetBudgetBinding

private const val TAG = "SetBudgetFragment"

class SetBudgetFragment : Fragment() {
    
    // ViewBinding reference
    private var _binding: FragmentSetBudgetBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSetBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtain references from ViewBinding
        val seekBar = binding.seekBarMin
        val txtSeekValue = binding.txtSeekValue
        val changingHeading = binding.txtHeadingChange
        val etMaxValue = binding.etMaxValue
        val btnSetMax = binding.btnSetMax
        val btnSetCatMinimum = binding.btnSet
        
        // Map of category buttons to their labels
        val categoryButtons = mapOf(
            binding.btnEntertainment.id to "Entertainment",
            binding.btnHealth.id to "Health",
            binding.btnHousing.id to "Housing",
            binding.btnClothing.id to "Clothing",
            binding.btnFood.id to "Food",
            binding.btnFuel.id to "Fuel",
            binding.btnGroceries.id to "Groceries",
            binding.btnInsurance.id to "Insurance",
            binding.btnInternet.id to "Internet"
        )
        
        // When a category button is clicked, update the heading text
        for ((id, category) in categoryButtons) {
            binding.root.findViewById<Button>(id).setOnClickListener {
                changingHeading.text = category
            }
        }
        
        // Snap SeekBar progress to the nearest 10 and show its value
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val snappedProgress = (progress / 10) * 10
                sb?.progress = snappedProgress
                txtSeekValue.text = "R$snappedProgress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        
        // Save category-specific minimum to Firebase
        btnSetCatMinimum.setOnClickListener {
            val selectedCategory = changingHeading.text.toString()
            val maxValue = seekBar.progress
            
            if (selectedCategory.isNotEmpty()) {
                val budget = Budget(category = selectedCategory, maximumValue = maxValue)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                    
                    dbRef.push().setValue(budget)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Budget saved!", Toast.LENGTH_SHORT).show()
                            // Clear input fields
                            etMaxValue.text.clear()
                            seekBar.progress = 0
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                            Log.e("FIREBASE_ERROR", "Save failed", e)
                        }
                } else {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Select a category first", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Adjust the SeekBar's maximum based on user input
        btnSetMax.setOnClickListener {
            val maxInput = etMaxValue.text.toString().toIntOrNull()
            if (maxInput != null && maxInput > 0) {
                seekBar.max = maxInput
                Toast.makeText(requireContext(), "SeekBar max set to $maxInput", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid number > 0", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        // Show tutorial overlay for this page only once
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenSetBudgetTut = prefs.getBoolean("hasSeenSetBudgetTut", false)
        
        if (!hasSeenSetBudgetTut) {
            Log.d(TAG, "onStart: Launching Set Budget tutorial overlay")
            
            // Create and show the tutorial fragment with the desired image and text
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.lacalm1, // replace with an appropriate drawable
                "On this screen, set your monthly budget per category.\n\n" +
                        "1. Tap a category button (e.g., ‘Food’, ‘Entertainment’).\n" +
                        "2. Adjust the slider to set a minimum spend (snap to multiples of 10).\n" +
                        "3. If you want to customize the slider’s maximum, enter a value above and tap ‘Set Max’.  \n" +
                        "4. Finally, tap ‘Set’ to save this category’s budget to your account."
            )
            
            // Add the overlay on top of this fragment's container
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay)
                .commit()
            
            // Mark tutorial as seen so it doesn’t show again
            prefs.edit()
                .putBoolean("hasSeenSetBudgetTut", true)
                .apply()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
