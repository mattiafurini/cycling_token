package com.example.test.utils

import android.util.Log
import com.example.test.network.models.GpsPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object IpfsHelper {
    
    private const val TAG = "IpfsHelper"
    private const val PINATA_API_URL = "https://api.pinata.cloud/pinning/pinJSONToIPFS"
    
    /**
     * Upload ride data to IPFS via Pinata
     * @param distance Total distance in km
     * @param reward Token reward for this ride
     * @param userAddress User wallet address
     * @return IPFS CID or null on failure
     */
    suspend fun uploadRideData(
        distance: Float,
        reward: Float,
        userAddress: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Prepare JSON payload - ONLY distance, reward, timestamp, user
            // DO NOT include GPS data (server handles verification differently)
            val jsonData = JSONObject().apply {
                put("user", userAddress)
                put("km", distance)
                put("reward", reward)
                put("timestamp", System.currentTimeMillis())
                put("type", "Cycling Session")
                put("app", "CyclingToken Android")
            }
            
            val requestBody = JSONObject().apply {
                put("pinataContent", jsonData)
                put("pinataMetadata", JSONObject().apply {
                    put("name", "ride_${System.currentTimeMillis()}")
                })
            }
            
            // Make HTTP request
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(PINATA_API_URL)
                .addHeader("Authorization", "Bearer ${Constants.PINATA_JWT}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                val cid = jsonResponse.optString("IpfsHash")
                
                Log.d(TAG, "IPFS Upload Success: $cid")
                return@withContext cid
            } else {
                Log.e(TAG, "IPFS Upload Failed: ${response.code} ${response.message}")
                return@withContext null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "IPFS Upload Error (Network): ${e.message}")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "IPFS Upload Error: ${e.message}")
            return@withContext null
        }
    }
    
    /**
     * Fetch ride data from IPFS
     * @param cid IPFS CID
     * @return JSON string or null
     */
    suspend fun fetchRideData(cid: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "${Constants.PINATA_GATEWAY}$cid"
            val request = Request.Builder().url(url).build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                return@withContext response.body?.string()
            } else {
                Log.e(TAG, "IPFS Fetch Failed: ${response.code}")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "IPFS Fetch Error: ${e.message}")
            return@withContext null
        }
    }
}
