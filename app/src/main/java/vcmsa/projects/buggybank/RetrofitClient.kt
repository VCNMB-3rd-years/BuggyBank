package vcmsa.projects.buggybank

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.apilayer.com/currency_data/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CurrencyAPI = retrofit.create(CurrencyAPI::class.java)
}



