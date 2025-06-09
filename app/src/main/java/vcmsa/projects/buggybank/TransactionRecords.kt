package vcmsa.projects.buggybank

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.transactionrecords.TransactionRecordsAdapter
import com.google.android.material.search.SearchBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class TransactionRecords : Fragment() {
    private lateinit var rootNode: FirebaseDatabase
    private lateinit var userReference: DatabaseReference
    private lateinit var adapter: TransactionRecordsAdapter
    private lateinit var transactionsList: RecyclerView
    private val transactions = ArrayList<Transaction>()
    private lateinit var noTransactions: TextView
    private lateinit var timePeriod:TextView
    private lateinit var btnAll: TextView
    private lateinit var btnIncome: TextView
    private lateinit var btnExpense: TextView

    private lateinit var searchBar : SearchView

    private val TAG = "TransactionRecords"
    private lateinit var sortCategory: TextView
    private val filteredTransactions = ArrayList<Transaction>()
    private val itemRecords = ArrayList <Transaction>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val layout = inflater.inflate(R.layout.fragment_transaction_records, container, false)

        transactionsList = layout.findViewById(R.id.rvTransactions)
        noTransactions = layout.findViewById<TextView>(R.id.tvNoTransactions)
        transactionsList.layoutManager = LinearLayoutManager(requireContext())
        sortCategory = layout.findViewById(R.id.SortCategory)
        timePeriod = layout.findViewById<TextView>(R.id.txtTimePeriod)
        btnAll = layout.findViewById(R.id.btnAll)
        btnIncome = layout.findViewById(R.id.btnIncomes)
        btnExpense = layout.findViewById(R.id.btnExpenses)

        searchBar = layout.findViewById(R.id.searchBar)
        searchBar.isIconified = false
        searchBar.queryHint = "Search..."
        searchBar.setQuery("", false)

        searchBar.clearFocus() //this removes the cursor from the search bar when fragment loads


        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRecords(newText ?: "")
                return true
            }
        })


        adapter = TransactionRecordsAdapter(transactions)
        transactionsList.adapter = adapter

        btnAll.setOnClickListener { updateDisplayedTransactions(filteredTransactions) }

        btnIncome.setOnClickListener {
            //updateFilterButtons(btnIncome)
            val income = filteredTransactions.filter { it.type.equals("Income", true) }
            updateDisplayedTransactions(income)
        }

        btnExpense.setOnClickListener {
            val expense = filteredTransactions.filter { it.type.equals("Expense", true) }
            updateDisplayedTransactions(expense)
        }
        timePeriod.setOnClickListener {
            showDateRangeDialog()
        }

        // Initialize Firebase
        rootNode = FirebaseDatabase.getInstance()

        // Use FirebaseAuth to get the current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid



        Log.e(TAG, userId.toString())

        if (userId == null) {
            Log.e(TAG, "User not logged in")
            noTransactions.visibility = View.VISIBLE
            return layout
        }

        Log.e(TAG, "User: ${FirebaseAuth.getInstance().currentUser}")
        userReference = rootNode.getReference("users").child(userId).child("transactions")
        Log.e(TAG, "$userReference")

        fetchTransactionsFromFirebase()

        sortCategory.setOnClickListener {
            showCategoryPopup(it)
        }

        return layout
    }

    private fun fetchTransactionsFromFirebase() {

        lifecycleScope.launch {
            try {

                val snapshot = withContext(Dispatchers.IO) {
                    val dataSnapshot = suspendCoroutine<DataSnapshot> { continuation ->
                        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                Log.e(TAG, "$dataSnapshot ")
                                continuation.resume(dataSnapshot)
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                continuation.resumeWithException(databaseError.toException())
                            }
                        })
                    }
                    dataSnapshot
                }

                // Update the UI on the main thread
                transactions.clear()
                filteredTransactions.clear()

                for (snapshot1 in snapshot.children) {
                    Log.e(TAG, "$snapshot1")
                    val transaction = snapshot1.getValue(Transaction::class.java)
                    if (transaction != null) {
                        transactions.add(transaction)
                        filteredTransactions.add(transaction)
                    }
                }
                adapter.notifyDataSetChanged()

                if (transactions.isEmpty()) {
                    noTransactions.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                // Handle errors, e.g. network failures
//                context?.let {
//                    Toast.makeText(
//                        it,
//                        "You have no transactions",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }

                Log.e(TAG, "Failed to Fetch Transactions ", e)
                noTransactions.visibility = View.VISIBLE;

            }
        }

    }
//    private fun updateFilterButtons(selected: TextView) {
//        val allButtons = listOf(btnAll, btnIncome, btnExpense)
//        allButtons.forEach { it.setBackgroundResource(R.drawable.unselected_button_bg) }
//        selected.setBackgroundResource(R.drawable.selected_button_bg)
//    }


    private fun showDateRangeDialog() {
        val calendar = Calendar.getInstance()
        val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())

        // Start Date Picker
        DatePickerDialog(requireContext(), { _, startYear, startMonth, startDay ->
            val startCal = Calendar.getInstance()
            startCal.set(startYear, startMonth, startDay, 0, 0, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            // End Date Picker
            DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                val endCal = Calendar.getInstance()
                endCal.set(endYear, endMonth, endDay, 23, 59, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val startMillis = startCal.timeInMillis
                val endMillis = endCal.timeInMillis

                // Update the label
                timePeriod.text = "${fmt.format(startCal.time)} - ${fmt.format(endCal.time)}"

                // Filter transactions
                filterTransactionsByDateRange(startMillis, endMillis)

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            ).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

//    private fun showDateRangePicker(selectedTimePeriod: TextView) {
//        val cal = Calendar.getInstance()
//        DatePickerDialog(
//            requireContext(),
//            { _, y, m, d ->
//                cal.set(y, m, d)
//                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                selectedTimePeriod.setText(fmt.format(cal.time))
//            },
//            cal.get(Calendar.YEAR),
//            cal.get(Calendar.MONTH),
//            cal.get(Calendar.DAY_OF_MONTH)
//        ).show()
//    }


    private fun updateDisplayedTransactions(filteredList: List<Transaction>) {
        transactions.clear()
        transactions.addAll(filteredList)
        adapter.notifyDataSetChanged()

        noTransactions.visibility = if (transactions.isEmpty()) View.VISIBLE else View.INVISIBLE
    }
    private fun filterTransactionsByDateRange(startMillis: Long, endMillis: Long) {

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val filtered = filteredTransactions.filter { transaction ->
            try {
                val date = dateFormat.parse(transaction.date)
                val millis = date?.time ?: 0L
                millis in startMillis..endMillis
            } catch (e: Exception) {
                false // skip if there's a parsing error
            }
        }
        updateDisplayedTransactions(filtered)
    }


    private fun showCategoryPopup(anchor: View) {
        val context = anchor.context
        val categories = filteredTransactions.map { it.category }.distinct()

        if (categories.isEmpty()) {
            Toast.makeText(context, "No categories to filter", Toast.LENGTH_SHORT).show()
            return
        }

        val popupMenu = PopupMenu(context, anchor)
        categories.forEachIndexed { index, category ->
            popupMenu.menu.add(Menu.NONE, index, Menu.NONE, category)
        }

        popupMenu.setOnMenuItemClickListener { item ->
            val selectedCategory = categories[item.itemId]
            val filtered = filteredTransactions.filter { it.category == selectedCategory }
            updateDisplayedTransactions(filtered)
            true
        }

        popupMenu.show()
    }
    private fun filterRecords(text: String) {
        val searchText = text.lowercase()
        val filteredRecords = mutableListOf<Transaction>()

        for (item in filteredTransactions) {
            val matches = item.title.lowercase().contains(searchText) ||
                    item.category.lowercase().contains(searchText) ||
                    item.paymentMethod.lowercase().contains(searchText) ||
                    item.amount.toString().lowercase().contains(searchText) ||
                    item.date.lowercase().contains(searchText) ||
                    item.type.lowercase().contains(searchText)

            if (matches) {
                filteredRecords.add(item)
            }
        }

        if (filteredRecords.isEmpty()) {
            Toast.makeText(requireContext(), "No data found", Toast.LENGTH_SHORT).show()
        } else {
            updateDisplayedTransactions(filteredRecords)
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCalcTut = prefs.getBoolean("hasSeenCalcTut", false)
        
        if (!hasSeenCalcTut) {
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.wap, // Replace with a valid drawable in your project
                "Check out your transactions./n" + "You can filter by date range and category./n" +
                        "Also you can search for specific records./n" +
                        "You can edit and delete transactions./n" +
                        "• Tap OK to begin!"
            )
            
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay) // ensure this ID matches your layout
                .commit()
            
            prefs.edit().putBoolean("hasSeenCalcTut", true).apply()
        }
    }


}