package com.example.test.network

import com.example.test.utils.Constants
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    
    /**
     * Get configured Retrofit instance
     */
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            // Logging Interceptor for debugging
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            // OkHttp Client with timeouts
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()
            
            // Gson converter
            val gson = GsonBuilder()
                .setLenient()
                .create()
            
            // Build Retrofit
            retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        
        return retrofit!!
    }
    
    /**
     * Get ApiService instance (Singleton)
     */
    fun getApiService(): ApiService {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService::class.java)
        }
        return apiService!!
    }
    
    /**
     * Reset client (useful for testing or changing base URL)
     */
    fun reset() {
        retrofit = null
        apiService = null
    }
}
