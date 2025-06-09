package vcmsa.projects.buggybank

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.buggybank.databinding.FragmentReportBinding
import com.example.transactionrecords.TransactionRecordsAdapter
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ReportFragment : Fragment() {

    private lateinit var binding: FragmentReportBinding
    private lateinit var adapter: TransactionRecordsAdapter
    private lateinit var userReference: DatabaseReference
    private val transactions = ArrayList<Transaction>()
    private val TAG = "TransactionRecords"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportBinding.inflate(inflater, container, false)

        adapter = TransactionRecordsAdapter(transactions)
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            binding.tvNoTransactions.visibility = View.VISIBLE
            return binding.root
        }

        userReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("transactions")

        binding.btnDownloadPDF.setOnClickListener {
            createPDF(transactions)
        }

        fetchTransactionsFromFirebase()

        return binding.root
    }

    private fun fetchTransactionsFromFirebase() {
        lifecycleScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    suspendCoroutine<DataSnapshot> { continuation ->
                        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                continuation.resume(dataSnapshot)
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                continuation.resumeWithException(databaseError.toException())
                            }
                        })
                    }
                }

                transactions.clear()
                for (snapshot1 in snapshot.children) {
                    val transaction = snapshot1.getValue(Transaction::class.java)
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }

                adapter.notifyDataSetChanged()
                if (transactions.isEmpty()) {
                    binding.tvNoTransactions.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to Fetch Transactions", e)
                binding.tvNoTransactions.visibility = View.VISIBLE
            }
        }
    }

    private fun createPDF(transactions: List<Transaction>) {
        val pdfDocument = PdfDocument()

        // making page A4 landscape (landscape width is 842 and height is 595)
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
        }

        var y = 40f
        val xStart = 20f

        //main heading
        canvas.drawText("BuggyBank - Transaction Report", xStart, y, paint)
        y += 30

        // columns
        val headers = listOf("Title", "Category", "Payment", "Amount", "Date", "Type", "Start", "End", "Desc")
        val columnWidths = listOf(80, 80, 80, 70, 80, 70, 70, 70, 100) // Adjusted column widths for landscape
        var x = xStart
        headers.forEachIndexed { index, title ->
            canvas.drawText(title, x, y, paint)
            x += columnWidths[index]
        }

        y += 20
        paint.isFakeBoldText = false // normal text for rows

        // rows
        transactions.forEach {
            x = xStart
            val rowValues = listOf(
                it.title,
                it.category,
                it.paymentMethod,
                "R${"%.2f".format(it.amount)}",
                it.date,
                it.type,
                it.startTime,
                it.endTime,
                it.description
            )
            rowValues.forEachIndexed { index, value ->
                val text = if (value.length > 10) value.take(10) + "…" else value
                canvas.drawText(text, x, y, paint)
                x += columnWidths[index]
            }
            y += 20
        }

        pdfDocument.finishPage(page)

        // saving and opening file
        val docsFolder = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (docsFolder != null && !docsFolder.exists()) {
            docsFolder.mkdirs()
        }

        val file = File(docsFolder, "Report.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()

            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val openIntent = Intent(Intent.ACTION_VIEW)
            openIntent.setDataAndType(uri, "application/pdf")
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(openIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    override fun onStart() {
        super.onStart()
        
        // Show this fragment's tutorial overlay once per install/user
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenCreateCategoryTut = prefs.getBoolean("hasSeenCreateCategoryTut", false)
        
        if (!hasSeenCreateCategoryTut) {
            Log.d(ContentValues.TAG, "onStart: Launching tutorial overlay")
            
            // Build the tutorial page explaining this screen's controls
            val tutorialOverlay = TutorialFragment.newInstance(
                R.drawable.wap,  // Replace with your appropriate drawable
                // Explanatory text for creating a category:
                "This the reports screen.../n" +
                        "You can check and download your reports here./n" +
                        "You also filter your reports here/n"
            )
            
            // Overlay the tutorial on top of the existing fragmentContainerView
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerView, tutorialOverlay)
                .commit()
            
            // Mark as seen so it won't show next time
            prefs.edit()
                .putBoolean("hasSeenCreateCategoryTut", true)
                .apply()
        }
    }

}
