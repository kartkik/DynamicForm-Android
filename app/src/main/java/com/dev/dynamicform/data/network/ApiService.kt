package com.dev.dynamicform.data.network

import android.util.Log
import com.dev.dynamicform.data.model.ApiResponse
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("2de9f482-e308-474e-82df-aa36f5eb4ada")
    suspend fun getFormConfig(): Response<ApiResponse>
}

object RetrofitClient {
    private const val TAG = "ApiService>>>"
    private const val BASE_URL = "https://webhook.site/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d(TAG, "--> ${request.method} ${request.url}")

                val response = chain.proceed(request)
                val bodyString = response.body?.string().orEmpty()

                Log.d(TAG, "<-- HTTP ${response.code} (${request.url})\nResponse: $bodyString")

                val newResponseBody = bodyString.toResponseBody(response.body?.contentType())
                response.newBuilder().body(newResponseBody).build()
            }
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

