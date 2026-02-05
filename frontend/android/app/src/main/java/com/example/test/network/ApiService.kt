package com.example.test.network

import com.example.test.network.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    /**
     * Health check endpoint
     */
    @GET("api/health")
    suspend fun checkHealth(): Response<HealthResponse>
    
    /**
     * Health check endpoint with full details (includes server wallet address)
     */
    @GET("api/health")
    suspend fun checkHealthFull(): Response<HealthResponseFull>
    
    /**
     * Get user profile and statistics
     * @param address Wallet address (0x...)
     */
    @GET("api/user/{address}")
    suspend fun getUserProfile(
        @Path("address") address: String
    ): Response<UserResponse>
    
    /**
     * Save a completed ride
     * @param rideRequest Ride data including GPS points
     */
    @POST("api/ride")
    suspend fun saveRide(
        @Body rideRequest: RideRequest
    ): Response<RideResponse>
    
    /**
     * Claim pending tokens (mints tokens on blockchain)
     * @param claimRequest User address
     */
    @POST("api/claim")
    suspend fun claimTokens(
        @Body claimRequest: ClaimRequest
    ): Response<ClaimResponse>
    
    /**
     * Get ride history for a user
     * @param address Wallet address
     */
    @GET("api/rides/{address}")
    suspend fun getRideHistory(
        @Path("address") address: String
    ): Response<RidesHistoryResponse>
    
    /**
     * Test endpoint to verify connectivity
     */
    @GET("api/test")
    suspend fun testConnection(): Response<Map<String, Any>>
    
    /**
     * Buy item from shop (gasless burn via permit)
     * @param buyRequest Contains address, item_id, price, and permit signature
     */
    @POST("api/buy")
    suspend fun buyItem(
        @Body buyRequest: BuyRequest
    ): Response<BuyResponse>
}
