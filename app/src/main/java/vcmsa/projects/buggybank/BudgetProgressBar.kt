package vcmsa.projects.buggybank

import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BudgetProgressBar : Fragment() {

    private lateinit var layoutBudgetItems: LinearLayout
    private lateinit var dbRef: DatabaseReference
    private val userId: String by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_budget_progress_bar, container, false)
        layoutBudgetItems = view.findViewById(R.id.layoutBudgetItems)

        loadBudgetProgress()

        return view
    }

    private fun loadBudgetProgress() {
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val budgetsRef = dbRef.child("budgets")
        val transactionsRef = dbRef.child("transactions")

        budgetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(budgetSnapshot: DataSnapshot) {
                transactionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(transactionSnapshot: DataSnapshot) {
                        layoutBudgetItems.removeAllViews()

                        for (budget in budgetSnapshot.children) {
                            val category = budget.child("category").getValue(String::class.java) ?: continue
                            val budgetAmount = budget.child("amount").getValue(Int::class.java) ?: 0

                            // Calculate total spent
                            var totalSpent = 0
                            for (txn in transactionSnapshot.children) {
                                val txnCategory = txn.child("category").getValue(String::class.java)
                                val txnType = txn.child("type").getValue(String::class.java)
                                val txnAmount = txn.child("amount").getValue(Int::class.java) ?: 0

                                if (txnCategory == category && txnType == "Expense") {
                                    totalSpent += txnAmount
                                }
                            }

                            addBudgetItemToLayout(category, totalSpent, budgetAmount)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load budgets", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addBudgetItemToLayout(category: String, spent: Int, budget: Int) {
        val itemView = layoutInflater.inflate(R.layout.item_budget_progress, layoutBudgetItems, false)

        val txtCategoryName = itemView.findViewById<TextView>(R.id.txtCategoryName)
        val txtAmountInfo = itemView.findViewById<TextView>(R.id.txtAmountInfo)
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progressBar)

        txtCategoryName.text = category
        txtAmountInfo.text = "Spent R$spent of R$budget"

        val percent = if (budget > 0) (spent * 100 / budget) else 0
        progressBar.progress = percent.coerceAtMost(100)

        if (spent > budget) {
            progressBar.progressDrawable = requireContext().getDrawable(R.drawable.progress_bar_red)
        } else {
            progressBar.progressDrawable = requireContext().getDrawable(R.drawable.progress_bar_green)
        }

        layoutBudgetItems.addView(itemView)
    }

}
