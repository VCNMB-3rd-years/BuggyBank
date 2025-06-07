package vcmsa.projects.buggybank

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

private val FragBudgetBuddyWalletsList = BudgetBuddyWalletsListFragment()

class BudgetBuddyProfileFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val btnAddWallets = view?.findViewById<Button>(R.id.btnAddWallets)

        btnAddWallets?.setOnClickListener{
        parentFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, FragBudgetBuddyWalletsList)
        .addToBackStack(null)
        .commit()
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_budget_buddy_profile, container, false)
    }


}