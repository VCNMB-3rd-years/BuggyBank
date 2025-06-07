package vcmsa.projects.buggybank

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReportAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tvTransactionItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.textView.text =
            "${transaction.date} - ${transaction.description} - R${transaction.amount}"
    }

    override fun getItemCount() = transactions.size
}
