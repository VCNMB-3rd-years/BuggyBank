package vcmsa.projects.buggybank

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class CurrencyConverterFragment : Fragment() {

    private lateinit var etAmount: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var btnConvert: Button
    private lateinit var tvResult: TextView
    private lateinit var btnClear: Button

    private val currencies = listOf("Select","USD", "EUR", "ZAR", "GBP", "JPY")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_currency_convertor, container, false)

        etAmount = view.findViewById(R.id.etAmount)
        spinnerFrom = view.findViewById(R.id.spinnerFrom)
        spinnerTo = view.findViewById(R.id.spinnerTo)
        btnConvert = view.findViewById(R.id.btnConvert)
        tvResult = view.findViewById(R.id.tvResult)
        btnClear = view.findViewById(R.id.btnClear)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter



        btnConvert.setOnClickListener {
            val from = spinnerFrom.selectedItem.toString()
            val to = spinnerTo.selectedItem.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val apiKey = "bAfl2F3hdBkBMd0jUt6kQGpFTaGTAZLy"

            if (from == "Select" || to == "Select") {
                Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount == null) {
                Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.getLiveRates(apiKey, from, to)
                    if (response.success) {
                        val rate = response.quotes["$from$to"]
                        if (rate != null) {
                            val amount = etAmount.text.toString().toDouble()
                            val converted = amount * rate
                            tvResult.text = "Result: %.2f $to".format(converted)
                        } else {
                            tvResult.text = "Rate not found"
                        }
                    } else {
                        tvResult.text = "Conversion failed"
                    }
                } catch (e: Exception) {
                    tvResult.text = "Error: ${e.localizedMessage}"
                }
            }

        }

//resets to default
        btnClear.setOnClickListener{
            tvResult.text = ""
            spinnerTo.setSelection(0)
            spinnerFrom.setSelection(0)
            etAmount.text.clear()

        }

        return view
    }
}
