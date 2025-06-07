package vcmsa.projects.buggybank

data class Transaction(
        val title: String = "",
        val category: String = "",
        val paymentMethod: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val type: String = "",
        val description: String = "",
        val startTime: String = "",
        val endTime: String = "",
        var isExpanded: Boolean = false
)