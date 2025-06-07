package vcmsa.projects.buggybank

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import vcmsa.projects.buggybank.databinding.FragmentMainPageBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val TAG = "MainPageFragment"

class MainPageFragment : Fragment() {
    
    // Fragment arguments (if ever used)
    private var param1: String? = null
    private var param2: String? = null
    
    // ViewBinding for fragment_main_page.xml
    private var _binding: FragmentMainPageBinding? = null
    private val binding get() = _binding!!
    
    // Chart reference
    private lateinit var chart: BarChart
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        Log.d(TAG, "onCreate: PARAM1: $param1, PARAM2: $param2")
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentMainPageBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        
        // Retrieve the current Firebase user and database
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseDatabase.getInstance()
        
        // TextView references
        val textViewUserName = binding.mainText
        val vAmount = binding.walletAmount
        
        // Initialize chart reference
        chart = binding.statusBar
        
        // Load chart data from Firebase
        loadChartData()
        
        // If user is logged in, fetch their username and wallet total
        user?.let {
            val userId = it.uid
            
            // Fetch and display username
            val userRef = db.getReference("users")
                .child(userId)
                .child("details")
                .child("username")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "onDataChange (username): $snapshot")
                    val userName = snapshot.getValue<String>()
                    textViewUserName.text = "Welcome ${userName ?: "No name"}"
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "onCancelled (username): $error")
                    textViewUserName.text = "Error fetching user data"
                }
            })
            
            // Fetch and display wallet total
            val walletRef = db.getReference("users")
                .child(userId)
                .child("transactions")
            walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "onDataChange (wallet): $snapshot")
                    var totalAmount = 0.0
                    for (txn in snapshot.children) {
                        val amount = txn.child("amount").getValue<Double>()
                        amount?.let { totalAmount += it }
                    }
                    vAmount.text = "Wallet: $totalAmount"
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "onCancelled (wallet): $error")
                    vAmount.text = "Error fetching wallet"
                }
            })
        }
        
        // Button click animation (fade)
        val buttonClickAnimation = AlphaAnimation(1f, 0.5f).apply {
            duration = 200
            repeatMode = Animation.REVERSE
            repeatCount = 1
        }
        
        // Navigate to CreateTransactionFragment
        binding.addTransaction.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            Log.d(TAG, "onClick: Going to create transaction page")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, CreateTransactionFragment())
                .addToBackStack(null)
                .commit()
        }
        
        // Navigate to TransactionRecords fragment
        binding.viewTransition.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            Log.d(TAG, "onClick: Going to view transaction page")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, TransactionRecords())
                .addToBackStack(null)
                .commit()
        }
        
        // Navigate to ReportFragment
        binding.viewReport.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            Log.d(TAG, "onClick: Going to view report page")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, ReportFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        // Show tutorial overlay once per user for this page
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenMainPageTut = prefs.getBoolean("hasSeenMainPageTut", false)
        
        if (!hasSeenMainPageTut) {
            Log.d(TAG, "onStart: Launching Main Page tutorial overlay")
            
            // Example image and text for the main page tutorial
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.anti,                            // your drawable resource
                "Hey there! Welcome to BuggyBank. /n" +
                        "This is your Dashboard! Here you can see your recent transactions and wallet balance./n" +
                        " You can Tap “Add Transaction” to log a new expense, or use “View Report” to see charts./n"+
                        "Tap “+” to get started."
            )
            
            // Add on top of the current fragmentContainerView
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay)
                .commit()
            
            // Mark as seen so it doesn’t reappear next time
            prefs.edit()
                .putBoolean("hasSeenMainPageTut", true)
                .apply()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
    
    // Load transaction data from Firebase into the bar chart
    private fun loadChartData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("Firebase", "User not logged in.")
            return
        }
        
        val userId = currentUser.uid
        val transactionsRef = FirebaseDatabase
            .getInstance()
            .getReference("users/$userId/transactions")
        
        transactionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionData = mutableMapOf<String, Float>()
                for (txnSnap in snapshot.children) {
                    val title = txnSnap.child("title").getValue(String::class.java)
                    val amount = txnSnap.child("amount").getValue(Float::class.java)
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
    
    // Render bar chart with labels and amounts
    private fun showChart(transactions: Map<String, Float>) {
        val entries = ArrayList<BarEntry>()
        val labels = transactions.keys.toList()
        
        for ((index, title) in labels.withIndex()) {
            entries.add(BarEntry(index.toFloat(), transactions[title] ?: 0f))
        }
        
        val dataSet = BarDataSet(entries, "Transactions").apply {
            color = Color.rgb(114, 191, 120)
        }
        val data = BarData(dataSet).apply {
            barWidth = 0.9f
        }
        
        chart.data = data
        chart.setFitBars(true)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.invalidate()
    }
    
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
