package vcmsa.projects.buggybank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class AddBudgetBuddyPopUpFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_budget_buddy_pop_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get references to input fields and button
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val btnAddBuddy: Button = view.findViewById(R.id.btnAddBuddy)

        btnAddBuddy.setOnClickListener {
            val emailInput = etEmail.text.toString().trim()

            // Validate inputs
            if (emailInput.isEmpty()) {
                Toast.makeText(context, "Email not entered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener //therefore wont get saved. redirect
            }

            // You can now use the variables emailInput
            Toast.makeText(context, "Saved: $emailInput", Toast.LENGTH_SHORT).show()

            // - Query Firebase to check if user with both values exists
            // - Send confirmation
            // - Sync data between users
            dismiss() // optional: close popup after success
        }
    }
}
