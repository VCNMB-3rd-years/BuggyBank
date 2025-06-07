import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import vcmsa.projects.buggybank.Budget
import vcmsa.projects.buggybank.R
import android.widget.TextView
import android.widget.EditText

class SetBudgetFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_set_budget, container, false)

        val seekBar = view.findViewById<SeekBar>(R.id.seekBarMin)
        val txtSeekValue = view.findViewById<TextView>(R.id.txtSeekValue)
        val changingHeading = view.findViewById<TextView>(R.id.txtHeadingChange)
        val etMaxValue = view.findViewById<EditText>(R.id.etMaxValue)
        val btnSetMax = view.findViewById<Button>(R.id.btnSetMax)
        val btnSetCatMinimum = view.findViewById<Button>(R.id.btnSet)

        // Category buttons
        val categoryButtons = mapOf(
            R.id.btnEntertainment to "Entertainment",
            R.id.btnHealth to "Health",
            R.id.btnHousing to "Housing",
            R.id.btnClothing to "Clothing",
            R.id.btnFood to "Food",
            R.id.btnFuel to "Fuel",
            R.id.btnGroceries to "Groceries",
            R.id.btnInsurance to "Insurance",
            R.id.btnInternet to "Internet"
        )

        for ((id, category) in categoryButtons) {
            view.findViewById<Button>(id).setOnClickListener {
                changingHeading.text = category
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Snap to nearest 10
                val snappedProgress = (progress / 10) * 10
                seekBar?.progress = snappedProgress
                txtSeekValue.text = "R$snappedProgress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


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
                            etMaxValue.text.clear()
                            seekBar.progress =0
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

        btnSetMax.setOnClickListener {
            val maxInput = etMaxValue.text.toString().toIntOrNull()
            if (maxInput != null && maxInput > 0) {
                seekBar.max = maxInput
                Toast.makeText(requireContext(), "SeekBar max set to $maxInput", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid number > 0", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
