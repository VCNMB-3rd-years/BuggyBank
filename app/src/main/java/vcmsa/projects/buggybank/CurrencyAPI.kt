package vcmsa.projects.buggybank

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CurrencyAPI {
    @GET("live")
    suspend fun getLiveRates(
        @Header("apikey") apiKey: String,
        @Query("source") source: String,
        @Query("currencies") currencies: String
    ): CurrencyResponse
}
