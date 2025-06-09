package vcmsa.projects.buggybank

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import vcmsa.projects.buggybank.databinding.FragmentMainPageBinding

private const val TAG = "MainPageFragment"

class MainPageFragment : Fragment() {

    private var _binding: FragmentMainPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var chart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMainPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase.getInstance()
        val userId = user.uid

        chart = binding.statusBar

        // Greet user
        db.getReference("users/$userId/details/username")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.getValue<String>()
                    binding.mainText.text = "Welcome ${username ?: "User"}"
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.mainText.text = "Error fetching user data"
                    Log.e(TAG, "Username fetch cancelled: ${error.message}")
                }
            })

        // Load wallet and chart data
        loadWalletAndChartData(userId)

        // Click listeners with animation
        val buttonAnimation = AlphaAnimation(1f, 0.5f).apply {
            duration = 200
            repeatMode = Animation.REVERSE
            repeatCount = 1
        }

        binding.addTransaction.setOnClickListener {
            it.startAnimation(buttonAnimation)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, CreateTransactionFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.viewTransition.setOnClickListener {
            it.startAnimation(buttonAnimation)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, TransactionRecords())
                .addToBackStack(null)
                .commit()
        }

        binding.viewReport.setOnClickListener {
            it.startAnimation(buttonAnimation)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, ReportFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadWalletAndChartData(userId: String) {
        val transactionsRef = FirebaseDatabase.getInstance()
            .getReference("users/$userId/transactions")

        transactionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalIncome = 0f
                var totalExpense = 0f
                val chartData = mutableMapOf<String, Float>()

                for (transactionSnapshot in snapshot.children) {
                    val type = transactionSnapshot.child("type").getValue(String::class.java)
                    val title = transactionSnapshot.child("title").getValue(String::class.java)
                    val amount = transactionSnapshot.child("amount").getValue(Float::class.java)

                    if (!title.isNullOrBlank() && amount != null) {
                        chartData[title] = amount

                        when (type) {
                            "Income" -> totalIncome += amount
                            "Expense" -> totalExpense += amount
                        }
                    }
                }

                val walletAmount = totalIncome - totalExpense
                binding.walletTextView.text = "Wallet: R$walletAmount"

                showChart(chartData)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch transactions: ${error.message}")
                binding.walletTextView.text = "Error loading wallet"
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
    override fun onStart() {
        super.onStart()
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCalcTut = prefs.getBoolean("hasSeenCalcTut", false)
        
        if (!hasSeenCalcTut) {
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.butterfly, // Replace with a valid drawable in your project
                "WELCOME!/n" +
                        "This is BuggyBank, your personal budgeting app!.\n" +
                        "And this is the main page./n" + "Here you can view your wallet and transactions.\n" +
                        "I hope you love graphs go ahead and make your first transaction.\n" +
                        "• by tapping the + button to add a transaction and see the magic.\n" +
                        "• Tap OK to begin!"
            )
            
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay) // ensure this ID matches your layout
                .commit()
            
            prefs.edit().putBoolean("hasSeenCalcTut", true).apply()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
