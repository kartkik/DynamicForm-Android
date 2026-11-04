package com.dev.dynamicform.data.repository

import android.util.Log
import com.dev.dynamicform.data.model.FormField
import com.dev.dynamicform.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FormRepository {

    private val tag = "DynamicForm:Repo"

    suspend fun fetchFormFields(): Result<List<FormField>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching form fields from API...")
            val response = RetrofitClient.apiService.getFormConfig()

            if (response.isSuccessful) {
                val fields = response.body()?.data
                if (!fields.isNullOrEmpty()) {
                    Log.d(tag, "Successfully loaded ${fields.size} fields from remote API")
                    Result.success(fields)
                } else {
                    val errorMsg = "API returned an empty data list"
                    Log.e(tag, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "API error ${response.code()}: ${response.message()}"
                Log.e(tag, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "Network request failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}
