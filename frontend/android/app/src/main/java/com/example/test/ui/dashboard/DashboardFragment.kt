package com.example.test.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.test.R
import androidx.lifecycle.lifecycleScope
import com.example.test.data.LocationPoint
import com.example.test.data.RideRepository
import com.example.test.data.RideSession
import com.example.test.databinding.FragmentDashboardBinding
import com.example.test.repository.NetworkRepository
import com.example.test.utils.Constants
import com.example.test.wallet.WalletManager
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

class DashboardFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var totalDistance: Float = 0f
    private var lastLocation: Location? = null
    private var isTracking = false

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Simulation
    private var isSimulating = false
    private var isRideActive = false // Track if ride is in progress
    private var hasUnclaimedRide = false // Track if there's a completed ride to claim
    private val handler = Handler(Looper.getMainLooper())
    private var simulationRunnable: Runnable? = null
    private val pathPoints = mutableListOf<Location>()
    private var startTime: Long = 0
    
    // Backend integration
    private var currentWalletAddress = "0x0000000000000000000000000000000000000000"

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                startLocationUpdates()
            } else {
                Toast.makeText(context, getString(R.string.location_permission_needed), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Init Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        createLocationCallback()

        // Init Sensors
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Initialize WalletManager and load wallet address
        WalletManager.initialize()
        val walletAddress = WalletManager.loadSavedWalletAddress(requireContext())
        if (walletAddress != null) {
            currentWalletAddress = walletAddress
            Log.d("Dashboard", "Wallet connesso: $walletAddress")
            
            // Load token balance
            loadTokenBalance(walletAddress)
        } else {
            Log.d("Dashboard", "Nessun wallet connesso")
        }
        
        // Check Permissions and Start
        checkPermissionsAndStart()
        
        // Check backend connection
        checkBackendConnection()

        binding.btnStartRide.setOnClickListener {
            if (isSimulating) {
                stopSimulation()
            } else {
                startSimulation()
            }
        }

        binding.btnCollectTokens.setOnClickListener {
            saveRide()
            Toast.makeText(context, getString(R.string.tokens_claimed), Toast.LENGTH_SHORT).show()
            
            // Reset for next ride
            isRideActive = false
            hasUnclaimedRide = false // Clear the unclaimed ride flag
            totalDistance = 0f
            lastLocation = null
            pathPoints.clear()
            updateUI(0f, 0f)
            binding.textAccelerationValue.text = "0.0 m/s²"
            binding.btnCollectTokens.text = "Riscuoti Token"
            binding.btnCollectTokens.visibility = View.GONE
        }

        return root
    }

    private fun startSimulation() {
        isSimulating = true
        isRideActive = true // Start tracking distance
        startTime = System.currentTimeMillis()
        binding.btnStartRide.text = "Stop Riding"
        binding.btnStartRide.setIconResource(android.R.drawable.ic_media_pause)
        binding.btnCollectTokens.visibility = View.GONE
        
        // Reset data
        totalDistance = 0f
        lastLocation = null // Reset last location to avoid wrong distance calculation
        pathPoints.clear()
        
        // Initial fake location (Due Torri, Bologna)
        var currentLat = 44.494
        var currentLon = 11.343
        var currentSpeed = 15f // Start at 15 km/h
        
        simulationRunnable = object : Runnable {
             override fun run() {
                 if (!isSimulating) return
                 
                 // Update physics - Accelerate slowly to ~30
                 val targetSpeed = 30f 
                 val speedDelta = if (currentSpeed < targetSpeed) 0.5f else -0.5f
                 val fluctuation = (Math.random() - 0.5) * 0.5 
                 
                 val oldSpeedMs = currentSpeed / 3.6f
                 currentSpeed = (currentSpeed + speedDelta + fluctuation).toFloat().coerceIn(0f, 45f)
                 val newSpeedMs = currentSpeed / 3.6f
                 
                 // Distance in this second (m)
                 val distMeters = newSpeedMs 
                 
                 // Move North East approx
                 // 1 deg lat ~ 111km, 1 deg lon ~ 85km
                 val moveLat = (distMeters * 0.7) / 111000.0
                 val moveLon = (distMeters * 0.7) / 85000.0
                 
                 currentLat += moveLat
                 currentLon += moveLon
                 
                 val loc = Location("simulation")
                 loc.latitude = currentLat
                 loc.longitude = currentLon
                 loc.speed = newSpeedMs
                 loc.time = System.currentTimeMillis()
                 
                 pathPoints.add(loc)
                 totalDistance += (distMeters / 1000f).toFloat()
                 
                 // Accel m/s^2
                 val accel = (newSpeedMs - oldSpeedMs) // / 1 second
                 // Add some noise to make it look real or just absolute value
                 val displayAccel = max(0.0, accel.toDouble() + (Math.random() * 0.2))

                 updateUI(currentSpeed, totalDistance)
                 
                 // Only update acceleration if view is available
                 if (isAdded && _binding != null) {
                     binding.textAccelerationValue.text = String.format(Locale.getDefault(), "%.1f m/s²", displayAccel)
                 }

                 handler.postDelayed(this, 1000)
             }
        }
        handler.post(simulationRunnable!!)
    }

    private fun stopSimulation() {
        isSimulating = false
        isRideActive = false // Stop tracking distance
        hasUnclaimedRide = true // Mark that there's a ride to claim
        binding.btnStartRide.text = "Start Riding"
        binding.btnStartRide.setIconResource(android.R.drawable.ic_media_play)
        
        // Update button text with tokens to claim
        val tokensToEarn = String.format(Locale.getDefault(), "%.2f", totalDistance)
        binding.btnCollectTokens.text = "Riscuoti $tokensToEarn CYC"
        binding.btnCollectTokens.visibility = View.VISIBLE
        
        simulationRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun saveRide() {
        if (pathPoints.isEmpty()) return
        
        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - startTime) / 1000
        
        // Simple Average Speed calc (Total Distance / Total Time)
        // distance km, time hours
        val durationHours = durationSeconds / 3600f
        val avgSpeed = if (durationHours > 0) totalDistance / durationHours else 0f
        
        // Token calculation: 1 token per km (matching server logic)
        val tokens = totalDistance
        
        val path = pathPoints.map { 
            LocationPoint(it.latitude, it.longitude, it.time) 
        }

        val session = RideSession(
            date = startTime,
            distanceKm = totalDistance,
            averageSpeed = avgSpeed,
            durationSeconds = durationSeconds,
            tokensEarned = tokens,
            path = path
        )
        
        // Save locally
        RideRepository.addRide(requireContext(), session)
        
        // Save to backend (async)
        saveRideToBackend(session)
    }

    private fun checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isSimulating) return // Ignore real updates while simulating
                
                for (location in locationResult.locations) {
                    updateLocationData(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        isTracking = true
    }

    private fun updateLocationData(location: Location) {
        val speed = if (location.hasSpeed()) location.speed * 3.6f else 0f // m/s to km/h

        // Only accumulate distance when ride is active
        if (isRideActive) {
            if (lastLocation != null) {
                totalDistance += lastLocation!!.distanceTo(location) / 1000f // meters to km
            }
            lastLocation = location
        }

        updateUI(speed, totalDistance)
    }

    private fun updateUI(speed: Float, distance: Float) {
        // Only update UI if Fragment is still attached and view is available
        if (!isAdded || _binding == null) return
        
        binding.textSpeedValue.text = String.format(Locale.getDefault(), "%.1f", speed)
        binding.textDistanceValue.text = String.format(Locale.getDefault(), "%.2f km", distance)
    }

    override fun onResume() {
        super.onResume()
        
        // Restore button state if there's an unclaimed ride
        if (hasUnclaimedRide) {
            val tokensToEarn = String.format(Locale.getDefault(), "%.2f", totalDistance)
            binding.btnCollectTokens.text = "Riscuoti $tokensToEarn CYC"
            binding.btnCollectTokens.visibility = View.VISIBLE
        }
        
        if (!isSimulating) {
            accelerometer?.also { accel ->
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
            }
        }
        if (!isTracking && !isSimulating && (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
             startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        if (!isSimulating) {
             fusedLocationClient.removeLocationUpdates(locationCallback)
             isTracking = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isSimulating) return 

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val rawMagnitude = sqrt((x * x + y * y + z * z).toDouble())
            val acceleration = max(0.0, rawMagnitude - SensorManager.GRAVITY_EARTH)
            
            binding.textAccelerationValue.text = String.format(Locale.getDefault(), "%.1f m/s²", acceleration)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save state when Fragment is destroyed
        outState.putBoolean("hasUnclaimedRide", hasUnclaimedRide)
        outState.putFloat("totalDistance", totalDistance)
        outState.putBoolean("isSimulating", isSimulating)
        outState.putBoolean("isRideActive", isRideActive)
        outState.putLong("startTime", startTime)
    }
    
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Restore state when Fragment is recreated
        savedInstanceState?.let { bundle ->
            hasUnclaimedRide = bundle.getBoolean("hasUnclaimedRide", false)
            totalDistance = bundle.getFloat("totalDistance", 0f)
            isSimulating = bundle.getBoolean("isSimulating", false)
            isRideActive = bundle.getBoolean("isRideActive", false)
            startTime = bundle.getLong("startTime", 0)
            
            // Restore UI based on saved state
            if (hasUnclaimedRide) {
                val tokensToEarn = String.format(Locale.getDefault(), "%.2f", totalDistance)
                binding.btnCollectTokens.text = "Riscuoti $tokensToEarn CYC"
                binding.btnCollectTokens.visibility = View.VISIBLE
            }
            
            if (isSimulating) {
                binding.btnStartRide.text = "Stop Riding"
                binding.btnStartRide.setIconResource(android.R.drawable.ic_media_pause)
            }
        }
    }
    
    // Backend integration functions
    private fun checkBackendConnection() {
        lifecycleScope.launch {
            val result = NetworkRepository.checkBackendHealth()
            
            result.onSuccess {
                Log.d("Dashboard", "Backend connected")
                // Backend connected - no user notification needed
            }
            
            result.onFailure { error ->
                Log.e("Dashboard", "Backend unreachable: ${error.message}")
                // Silently handle - app works in local mode
            }
        }
    }
    
    /**
     * Load token balance from blockchain
     */
    private fun loadTokenBalance(address: String) {
        lifecycleScope.launch {
            try {
                val balance = WalletManager.getTokenBalance(address)
                Log.d("Dashboard", "Token balance: $balance CYC")
                Toast.makeText(
                    context,
                    "💰 Balance: $balance CYC tokens",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("Dashboard", "Errore caricamento balance: ${e.message}")
            }
        }
    }
    
    private fun saveRideToBackend(ride: RideSession) {
        lifecycleScope.launch {
            // Uploading to server silently
            
            // Step 0: Ensure user exists in database first
            Log.d("Dashboard", "Ensuring user exists in database...")
            val userResult = NetworkRepository.getUserProfile(currentWalletAddress)
            
            userResult.onFailure { error ->
                Log.e("Dashboard", "Failed to get/create user: ${error.message}")
                // User error - logged only
                return@launch
            }
            
            Log.d("Dashboard", "User verified in database")
            
            // Step 1: Save ride to backend (this uploads to IPFS)
            val result = NetworkRepository.saveRide(currentWalletAddress, ride)
            
            result.onSuccess { response ->
                Log.d("Dashboard", "Ride saved successfully. IPFS CID: ${response.ipfsCid}")
                
                Toast.makeText(
                    context,
                    "✅ Corsa salvata! Attendo sincronizzazione Pinata...",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Step 2: Wait for Pinata to sync (3-5 seconds)
                kotlinx.coroutines.delay(4000) // 4 seconds delay
                
                // Step 3: Claim tokens (triggers minting)
                // Requesting tokens silently
                claimTokens()
                
            }
            
            result.onFailure { error ->
                Toast.makeText(
                    context,
                    getString(R.string.unable_to_save_ride),
                    Toast.LENGTH_LONG
                ).show()
                Log.e("Dashboard", "Backend save failed", error)
            }
        }
    }
    
    /**
     * Claim pending tokens from backend (triggers minting)
     */
    private fun claimTokens() {
        lifecycleScope.launch {
            try {
                val claimResult = NetworkRepository.claimTokens(currentWalletAddress)
                
                claimResult.onSuccess { claimResponse ->
                    val tokensEarned = claimResponse.amount ?: 0f
                    
                    Toast.makeText(
                        context,
                        "🎉 Token mintati! Ricevuti $tokensEarned CYC",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    Log.d("Dashboard", "Tokens claimed: $tokensEarned, TX: ${claimResponse.txHash}")
                    
                    // Wait a bit more for blockchain confirmation
                    kotlinx.coroutines.delay(2000) // 2 seconds
                    
                    // Reload balance after token claim
                    if (WalletManager.isWalletConnected()) {
                        WalletManager.getCurrentAddress()?.let { address ->
                            loadTokenBalance(address)
                            Toast.makeText(
                                context,
                                "🔄 Balance aggiornato!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                
                claimResult.onFailure { error ->
                    Toast.makeText(
                        context,
                        "⚠️ Errore claim token: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("Dashboard", "Token claim failed", error)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "⚠️ Errore: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("Dashboard", "Exception during claim", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }
}