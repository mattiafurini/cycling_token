package com.example.test.repository

import android.content.Context
import android.util.Log
import com.example.test.data.LocationPoint
import com.example.test.data.RideSession
import com.example.test.network.RetrofitClient
import com.example.test.network.models.*
import com.example.test.utils.Constants
import com.example.test.utils.IpfsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkRepository {
    
    private const val TAG = "NetworkRepository"
    private val apiService = RetrofitClient.getApiService()
    
    /**
     * Check backend health
     */
    suspend fun checkBackendHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkHealth()
            if (response.isSuccessful && response.body()?.status == "healthy") {
                Log.d(TAG, "Backend health check: OK")
                return@withContext Result.success(true)
            } else {
                Log.e(TAG, "Backend health check failed: ${response.code()}")
                return@withContext Result.failure(Exception("Backend not healthy"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend health check error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get user profile from backend
     * @param walletAddress User wallet address
     */
    suspend fun getUserProfile(walletAddress: String): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserProfile(walletAddress)
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    Log.d(TAG, "User profile fetched: ${user.walletAddress}")
                    return@withContext Result.success(user)
                }
            }
            Log.e(TAG, "Failed to fetch user profile: ${response.code()}")
            return@withContext Result.failure(Exception("Failed to fetch user: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Save ride to backend with IPFS upload
     * @param walletAddress User wallet address
     * @param ride Completed ride session
     */
    suspend fun saveRide(
        walletAddress: String,
        ride: RideSession
    ): Result<RideResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Upload to IPFS first (without GPS data, like frontend)
            Log.d(TAG, "Uploading ride data to IPFS...")
            val ipfsCid = IpfsHelper.uploadRideData(
                distance = ride.distanceKm,
                reward = ride.tokensEarned,
                userAddress = walletAddress
            )
            
            if (ipfsCid == null) {
                Log.w(TAG, "IPFS upload failed, continuing without CID")
            } else {
                Log.d(TAG, "IPFS CID: $ipfsCid")
            }
            
            // 2. Convert path to GPS points (for backend storage only, not IPFS)
            val gpsPoints = ride.path.map { point ->
                GpsPoint(
                    lat = point.latitude,
                    lon = point.longitude,
                    time = point.timestamp
                )
            }
            
            // 3. Save to backend
            val rideRequest = RideRequest(
                userAddress = walletAddress,
                distance = ride.distanceKm,
                avgSpeed = ride.averageSpeed,
                gpsData = gpsPoints,
                ipfsCid = ipfsCid
            )
            
            val response = apiService.saveRide(rideRequest)
            
            if (response.isSuccessful) {
                val rideResponse = response.body()
                if (rideResponse?.success == true) {
                    Log.d(TAG, "Ride saved successfully: ${rideResponse.rideId}")
                    return@withContext Result.success(rideResponse)
                }
            }
            
            Log.e(TAG, "Failed to save ride: ${response.code()}")
            return@withContext Result.failure(Exception("Failed to save ride: ${response.code()}"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ride: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Claim pending tokens
     * @param walletAddress User wallet address
     */
    suspend fun claimTokens(walletAddress: String): Result<ClaimResponse> = withContext(Dispatchers.IO) {
        try {
            val claimRequest = ClaimRequest(address = walletAddress)
            val response = apiService.claimTokens(claimRequest)
            
            if (response.isSuccessful) {
                val claimResponse = response.body()
                if (claimResponse?.success == true) {
                    Log.d(TAG, "Tokens claimed: ${claimResponse.amount} CYCL, TX: ${claimResponse.txHash}")
                    return@withContext Result.success(claimResponse)
                } else {
                    Log.e(TAG, "Claim failed: ${claimResponse?.error}")
                    return@withContext Result.failure(Exception(claimResponse?.error ?: "Claim failed"))
                }
            }
            
            Log.e(TAG, "Claim request failed: ${response.code()}")
            return@withContext Result.failure(Exception("Claim failed: ${response.code()}"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming tokens: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get ride history from backend
     */
    suspend fun getRideHistory(walletAddress: String): Result<List<RideHistoryItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRideHistory(walletAddress)
            if (response.isSuccessful) {
                val history = response.body()?.rides ?: emptyList()
                Log.d(TAG, "Fetched ${history.size} rides")
                return@withContext Result.success(history)
            }
            Log.e(TAG, "Failed to fetch ride history: ${response.code()}")
            return@withContext Result.failure(Exception("Failed to fetch history"))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ride history: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}
