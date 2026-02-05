package com.example.test.network.models

import com.google.gson.annotations.SerializedName

// Request Models

data class RideRequest(
    @SerializedName("user_address")
    val userAddress: String,
    
    @SerializedName("distance")
    val distance: Float,
    
    @SerializedName("avg_speed")
    val avgSpeed: Float,
    
    @SerializedName("gps_data")
    val gpsData: List<GpsPoint>,
    
    @SerializedName("ipfs_cid")
    val ipfsCid: String? = null
)

data class GpsPoint(
    @SerializedName("lat")
    val lat: Double,
    
    @SerializedName("lon")
    val lon: Double,
    
    @SerializedName("time")
    val time: Long
)

data class ClaimRequest(
    @SerializedName("address")
    val address: String
)

// Response Models

data class UserResponse(
    @SerializedName("wallet_address")
    val walletAddress: String,
    
    @SerializedName("pending_balance")
    val pendingBalance: Float,
    
    @SerializedName("total_km")
    val totalKm: Float,
    
    @SerializedName("pending_cids")
    val pendingCids: List<String>,
    
    @SerializedName("is_pro")
    val isPro: Boolean,
    
    @SerializedName("pro_expiry")
    val proExpiry: String? = null
)

data class RideResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("ride_id")
    val rideId: Int? = null,
    
    @SerializedName("tokens_earned")
    val tokensEarned: Float? = null,
    
    @SerializedName("ipfs_cid")
    val ipfsCid: String? = null,
    
    @SerializedName("user")
    val user: UserResponse? = null,
    
    @SerializedName("error")
    val error: String? = null
)

data class ClaimResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("txHash")
    val txHash: String? = null,
    
    @SerializedName("amount")
    val amount: Float? = null,
    
    @SerializedName("user")
    val user: UserResponse? = null,
    
    @SerializedName("error")
    val error: String? = null
)

data class HealthResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("timestamp")
    val timestamp: String
)

data class RidesHistoryResponse(
    @SerializedName("rides")
    val rides: List<RideHistoryItem>
)

data class RideHistoryItem(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_address")
    val userAddress: String,
    
    @SerializedName("distance")
    val distance: Float,
    
    @SerializedName("avg_speed")
    val avgSpeed: Float,
    
    @SerializedName("ipfs_cid")
    val ipfsCid: String?,
    
    @SerializedName("timestamp")
    val timestamp: String
)

// Shop Models

data class BuyRequest(
    @SerializedName("address")
    val address: String,
    
    @SerializedName("item_id")
    val itemId: String,
    
    @SerializedName("price")
    val price: Int,
    
    @SerializedName("permit")
    val permit: PermitData
)

data class PermitData(
    @SerializedName("deadline")
    val deadline: Long,
    
    @SerializedName("v")
    val v: Int,
    
    @SerializedName("r")
    val r: String,
    
    @SerializedName("s")
    val s: String
)

data class BuyResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("txHash")
    val txHash: String? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("error")
    val error: String? = null
)

data class HealthResponseFull(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("database")
    val database: String? = null,
    
    @SerializedName("blockchain")
    val blockchain: String? = null
)
