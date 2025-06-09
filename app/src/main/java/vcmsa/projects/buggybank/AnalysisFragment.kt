package vcmsa.projects.buggybank

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import vcmsa.projects.buggybank.databinding.FragmentAnalysisBinding
import vcmsa.projects.buggybank.databinding.FragmentMainPageBinding

private const val TAG = "AnalysisFragment"

class AnalysisFragment : Fragment() {
    
    private lateinit var chart: BarChart
    private lateinit var database: DatabaseReference
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analysis, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize chart and database
        chart = view.findViewById(R.id.statusBar)
       // chart = binding.statusBar
        database = FirebaseDatabase.getInstance().reference
        
        // Analyze and load chart data and get maximum goal for each category methods called
        analyzeData()
        loadChartData()
        populateBudgetTable()
    }
    
    override fun onStart() {
        super.onStart()
        
        // Check SharedPreferences to see if we've already shown this tutorial
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenAnalysisTut = prefs.getBoolean("hasSeenAnalysisTut", false)
        
        if (!hasSeenAnalysisTut) {
            Log.d(TAG, "onStart: Launching Analysis tutorial overlay")
            
            // Create a TutorialFragment explaining the Analysis page
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.butterfly, // replace with an appropriate drawable for analysis
                // Explanatory text for Analysis page:
                "This is the Analysis screen:\n\n" +
                        "• The bar chart above shows your transactions grouped by title.\n" +
                        "• Below, you’ll see total Expenses, total Income, and your highest-spend category.\n" +
                        "• Use this overview to track where most of your money is going.\n" +
                        "• Tap OK to continue."
            )
            
            // Overlay it on top of the current fragmentContainerView
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay)
                .commit()
            
            // Mark as seen so it won’t appear next time
            prefs.edit()
                .putBoolean("hasSeenAnalysisTut", true)
                .apply()
        }
    }
    
    private fun loadChartData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("Firebase", "User not logged in.")
            return
        }
        
        val userId = currentUser.uid
        val transactionsRef =
            FirebaseDatabase.getInstance().getReference("users/$userId/transactions")
        
        transactionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionData = mutableMapOf<String, Float>()
                for (transactionSnapshot in snapshot.children) {
                    val title = transactionSnapshot.child("title").getValue(String::class.java)
                    val amount = transactionSnapshot.child("amount").getValue(Float::class.java)
                    if (!title.isNullOrBlank() && amount != null) {
                        transactionData[title] = amount
                    }
                }
                
                if (transactionData.isEmpty()) {
                    Log.w("Firebase", "No transactions found.")
                } else {
                    showChart(transactionData)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Database error: ${error.message}")
            }
        })
    }

    private fun showChart(transactions: Map<String, Float>) {
        val entries = ArrayList<BarEntry>()
        val labels = transactions.keys.toList()

        labels.forEachIndexed { index, title ->
            entries.add(BarEntry(index.toFloat(), transactions[title] ?: 0f))
        }

        val dataSet = BarDataSet(entries, "Transactions").apply {
            color = Color.rgb(114, 191, 120)
        }

        chart.data = BarData(dataSet).apply {
            barWidth = 0.9f
        }

        chart.apply {
            setFitBars(true)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            description.isEnabled = false
            invalidate()
        }
    }

    private fun analyzeData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("Firebase", "User not logged in.")
            return
        }

        val userId = currentUser.uid
        val transactionsRef = FirebaseDatabase.getInstance().getReference("users/$userId/transactions")

        val expensesByCategory = mutableMapOf<String, Double>()
        var totalExpenses = 0.0
        var totalIncome = 0.0

        // For new calculations
        var lowestExpenseAmount = Double.MAX_VALUE
        var lowestExpenseCategory: String? = null

        var highestIncomeAmount = Double.MIN_VALUE
        var lowestIncomeAmount = Double.MAX_VALUE

        transactionsRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.d("Analysis", "No transaction data found.")
                return@addOnSuccessListener
            }

            for (transactionSnap in snapshot.children) {
                val type = transactionSnap.child("type").getValue(String::class.java)
                val category = transactionSnap.child("category").getValue(String::class.java)
                val amount = transactionSnap.child("amount").getValue(Double::class.java) ?: 0.0

                when (type) {
                    "Expense" -> {
                        val cat = category ?: "Unknown"
                        expensesByCategory[cat] = expensesByCategory.getOrDefault(cat, 0.0) + amount
                        totalExpenses += amount

                        // Track lowest single expense
                        if (amount < lowestExpenseAmount && amount > 0) {
                            lowestExpenseAmount = amount
                            lowestExpenseCategory = cat
                        }
                    }
                    "Income" -> {
                        totalIncome += amount

                        // Track highest and lowest single income
                        if (amount > highestIncomeAmount) highestIncomeAmount = amount
                        if (amount < lowestIncomeAmount && amount > 0) lowestIncomeAmount = amount
                    }
                }
            }

            val highestExpenseCategory = expensesByCategory.maxByOrNull { it.value }?.key ?: "N/A"
            val lowestExpenseDisplay = if (lowestExpenseCategory != null)
                "R %.2f (%s)".format(lowestExpenseAmount, lowestExpenseCategory)
            else
                "N/A"

            val highestIncomeDisplay = if (highestIncomeAmount != Double.MIN_VALUE)
                "R %.2f".format(highestIncomeAmount)
            else
                "N/A"

            val lowestIncomeDisplay = if (lowestIncomeAmount != Double.MAX_VALUE)
                "R %.2f".format(lowestIncomeAmount)
            else
                "N/A"

            // Display results
            view?.findViewById<TextView>(R.id.txtTotalExpensesData)?.text = "R %.2f".format(totalExpenses)
            view?.findViewById<TextView>(R.id.txtTotalIncomeData)?.text = "R %.2f".format(totalIncome)
            view?.findViewById<TextView>(R.id.txtHighestExpenseData)?.text = highestExpenseCategory
            view?.findViewById<TextView>(R.id.txtLowestExpenseData)?.text = lowestExpenseDisplay
            view?.findViewById<TextView>(R.id.txtHighestIncomeData)?.text = highestIncomeDisplay
            view?.findViewById<TextView>(R.id.txtLowestIncomeData)?.text = lowestIncomeDisplay

            Log.d("Analysis", "Total Expenses: R $totalExpenses")
            Log.d("Analysis", "Total Income: R $totalIncome")
            Log.d("Analysis", "Highest Expense Category: $highestExpenseCategory")
            Log.d("Analysis", "Lowest Single Expense: $lowestExpenseDisplay")
            Log.d("Analysis", "Highest Single Income: $highestIncomeDisplay")
            Log.d("Analysis", "Lowest Single Income: $lowestIncomeDisplay")

        }.addOnFailureListener {
            Log.e("Analysis", "Failed to load transactions: ${it.message}")
        }
    }
    private fun populateBudgetTable() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val budgetsRef = FirebaseDatabase.getInstance().getReference("users/$userId/budgets")

        budgetsRef.get().addOnSuccessListener { snapshot ->
            val tableLayout = view?.findViewById<TableLayout>(R.id.tableLayoutBudget)
            if (tableLayout == null || !snapshot.exists()) return@addOnSuccessListener

            for (budgetSnap in snapshot.children) {
                val category = budgetSnap.child("category").getValue(String::class.java) ?: "Unknown"
                val amount = budgetSnap.child("amount").getValue(Double::class.java) ?: 0.0

                val row = TableRow(requireContext())
                row.layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )

                val categoryTextView = TextView(requireContext()).apply {
                    text = category
                    setPadding(8, 8, 8, 8)
                }

                val amountTextView = TextView(requireContext()).apply {
                    text = "R %.2f".format(amount)
                    setPadding(8, 8, 8, 8)
                }

                row.addView(categoryTextView)
                row.addView(amountTextView)

                tableLayout.addView(row)
            }

        }.addOnFailureListener {
            Log.e("Firebase", "Failed to fetch budget data: ${it.message}")
        }
    }

}
