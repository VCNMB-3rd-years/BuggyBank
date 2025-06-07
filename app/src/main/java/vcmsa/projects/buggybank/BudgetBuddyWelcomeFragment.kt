package vcmsa.projects.buggybank

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


class BudgetBuddyWelcomeFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        return inflater.inflate(R.layout.fragment_budget_buddy_welcome, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgAddBudgetBuddy : ImageView = view.findViewById(R.id.imgAddBudgetBuddy)

        imgAddBudgetBuddy.setOnClickListener{
            val showPopUp = AddBudgetBuddyPopUpFragment()
            showPopUp.show(parentFragmentManager, "showPopUp")

        }
    }
//private fun addBudgetBuddyPopUp (){

//    parentFragmentManager.beginTransaction()
//        .replace(R.id.fragment_container, FragAddBudgetBuddy)
//        .addToBackStack(null)
//        .commit()
//}
}