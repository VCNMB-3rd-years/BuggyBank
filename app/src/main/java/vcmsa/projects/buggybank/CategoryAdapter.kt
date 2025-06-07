package vcmsa.projects.buggybank


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: List<Category>,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    fun getItemAt(position: Int): Category = categories[position]

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.categoryNameTextView1)
        val editButton: View = view.findViewById(R.id.editButton1)
        val deleteButton: View = view.findViewById(R.id.deleteButton1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.nameTextView.text = "${category.name} (${category.type})"
        holder.editButton.setOnClickListener { onEdit(category) }
        holder.deleteButton.setOnClickListener { onDelete(category) }
    }

    override fun getItemCount(): Int = categories.size
}
