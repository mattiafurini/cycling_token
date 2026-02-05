package com.example.test.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class RideSession(
    val id: Long = System.currentTimeMillis(),
    val date: Long,
    val distanceKm: Float,
    val averageSpeed: Float,
    val durationSeconds: Long,
    val tokensEarned: Float,
    val path: List<LocationPoint> = emptyList()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(date))
    }
}

object RideRepository {
    private val _rides = mutableListOf<RideSession>()
    private const val PREFS_NAME = "cycling_app_prefs"
    private const val KEY_RIDES = "saved_rides"
    
    fun getRides(context: Context): List<RideSession> {
        if (_rides.isEmpty()) {
            loadRides(context)
        }
        return _rides.toList().sortedByDescending { it.date }
    }
    
    fun getRide(context: Context, id: Long): RideSession? {
        if (_rides.isEmpty()) {
            loadRides(context)
        }
        return _rides.find { it.id == id }
    }

    fun addRide(context: Context, ride: RideSession) {
        if (_rides.isEmpty()) {
            loadRides(context)
        }
        _rides.add(ride)
        saveRides(context)
    }
    
    private fun saveRides(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        
        _rides.forEach { ride ->
            val jsonObj = JSONObject().apply {
                put("id", ride.id)
                put("date", ride.date)
                put("distanceKm", ride.distanceKm.toDouble())
                put("averageSpeed", ride.averageSpeed.toDouble())
                put("durationSeconds", ride.durationSeconds)
                put("tokensEarned", ride.tokensEarned)
                
                // Save path
                val pathArray = JSONArray()
                ride.path.forEach { point ->
                    val pointObj = JSONObject().apply {
                        put("lat", point.latitude)
                        put("lon", point.longitude)
                        put("time", point.timestamp)
                    }
                    pathArray.put(pointObj)
                }
                put("path", pathArray)
            }
            jsonArray.put(jsonObj)
        }
        
        prefs.edit().putString(KEY_RIDES, jsonArray.toString()).apply()
    }
    
    private fun loadRides(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RIDES, null) ?: return
        
        _rides.clear()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val pathList = mutableListOf<LocationPoint>()
                val pathArray = obj.optJSONArray("path")
                if (pathArray != null) {
                    for (j in 0 until pathArray.length()) {
                        val pObj = pathArray.getJSONObject(j)
                        pathList.add(LocationPoint(
                            latitude = pObj.getDouble("lat"),
                            longitude = pObj.getDouble("lon"),
                            timestamp = pObj.getLong("time")
                        ))
                    }
                }
                
                val ride = RideSession(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    date = obj.getLong("date"),
                    distanceKm = obj.getDouble("distanceKm").toFloat(),
                    averageSpeed = obj.getDouble("averageSpeed").toFloat(),
                    durationSeconds = obj.getLong("durationSeconds"),
                    tokensEarned = obj.getDouble("tokensEarned").toFloat(),
                    path = pathList
                )
                _rides.add(ride)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}