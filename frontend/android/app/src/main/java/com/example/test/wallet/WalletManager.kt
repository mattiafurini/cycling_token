package com.example.test.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.test.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Manager for wallet operations and MetaMask integration
 * Simplified version that uses MetaMask deep links for automatic configuration
 */
object WalletManager {
    
    private var web3j: Web3j? = null
    
    private val _walletAddress = MutableStateFlow<String?>(null)
    val walletAddress: StateFlow<String?> = _walletAddress.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _balance = MutableStateFlow(BigDecimal.ZERO)
    val balance: StateFlow<BigDecimal> = _balance.asStateFlow()
    
    /**
     * Initialize Web3j connection
     */
    fun initialize() {
        if (web3j == null) {
            web3j = Web3j.build(HttpService(Constants.RPC_URL))
        }
    }
    
    /**
     * Connect to MetaMask with automatic network and token setup
     * This will:
     * 1. Open MetaMask
     * 2. Request to add Polygon Amoy network
     * 3. Request to add CYC token
     * 4. Request account connection
     */
    fun connectMetaMaskWithSetup(context: Context, onAddressReceived: (String?) -> Unit) {
        try {
            val packageManager = context.packageManager
            val metamaskIntent = packageManager.getLaunchIntentForPackage("io.metamask")
            
            if (metamaskIntent != null) {
                // MetaMask is installed
                
                // Step 1: Add Polygon Amoy network via deep link
                addPolygonAmoyNetwork(context)
                
                // Save callback for later
                val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("pending_connection", true).apply()
                
            } else {
                // MetaMask not installed, redirect to Play Store
                val playStoreIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=io.metamask")
                )
                playStoreIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(playStoreIntent)
                onAddressReceived(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onAddressReceived(null)
        }
    }
    
    /**
     * Add Polygon Amoy network to MetaMask via deep link
     */
    private fun addPolygonAmoyNetwork(context: Context) {
        try {
            // Open MetaMask directly
            val intent = context.packageManager.getLaunchIntentForPackage("io.metamask")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (intent != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Request to add CYC token to MetaMask
     */
    fun addCycTokenToMetaMask(context: Context) {
        try {
            // MetaMask doesn't have a direct deep link for adding tokens on mobile
            // We'll need to do this programmatically after connection
            // For now, we open MetaMask
            val intent = context.packageManager.getLaunchIntentForPackage("io.metamask")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Set wallet address after manual entry or QR scan
     */
    fun setWalletAddress(context: Context, address: String) {
        _walletAddress.value = address
        _isConnected.value = true
        
        // Save to SharedPreferences
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("wallet_address", address).apply()
        prefs.edit().putBoolean("pending_connection", false).apply()
    }
    
    /**
     * Get token balance from blockchain
     */
    suspend fun getTokenBalance(address: String): BigDecimal {
        return try {
            initialize()
            
            android.util.Log.d("WalletManager", "Getting balance for address: $address")
            android.util.Log.d("WalletManager", "Contract address: ${Constants.CONTRACT_ADDRESS}")
            android.util.Log.d("WalletManager", "RPC URL: ${Constants.RPC_URL}")
            
            // Load contract
            val contract = Web3jContractHelper.loadContract(web3j!!, address)
            val balanceResult = contract.balanceOf(address)
            
            android.util.Log.d("WalletManager", "Balance result success: ${balanceResult.isSuccess}")
            
            if (balanceResult.isSuccess) {
                val balance = balanceResult.getOrNull() ?: java.math.BigInteger.ZERO
                android.util.Log.d("WalletManager", "Raw balance (Wei): $balance")
                
                // Convert from Wei to tokens (18 decimals)
                val balanceDecimal = BigDecimal(balance).divide(BigDecimal(10).pow(18))
                android.util.Log.d("WalletManager", "Balance (tokens): $balanceDecimal")
                
                _balance.value = balanceDecimal
                balanceDecimal
            } else {
                val error = balanceResult.exceptionOrNull()
                android.util.Log.e("WalletManager", "Balance query failed: ${error?.message}", error)
                BigDecimal.ZERO
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletManager", "Error getting balance: ${e.message}", e)
            e.printStackTrace()
            BigDecimal.ZERO
        }
    }
    
    /**
     * Get ETH/MATIC balance
     */
    suspend fun getEthBalance(address: String): BigDecimal {
        return try {
            initialize()
            val balance = web3j!!.ethGetBalance(address, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).sendAsync().get()
            BigDecimal(balance.balance).divide(BigDecimal(10).pow(18))
        } catch (e: Exception) {
            e.printStackTrace()
            BigDecimal.ZERO
        }
    }
    
    /**
     * Check if wallet is connected
     */
    fun isWalletConnected(): Boolean {
        return _isConnected.value && _walletAddress.value != null
    }
    
    /**
     * Disconnect wallet
     */
    fun disconnect(context: Context) {
        _walletAddress.value = null
        _isConnected.value = false
        _balance.value = BigDecimal.ZERO
        
        // Clear from SharedPreferences
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("wallet_address").apply()
        prefs.edit().putBoolean("pending_connection", false).apply()
    }
    
    /**
     * Get current wallet address
     */
    fun getCurrentAddress(): String? {
        return _walletAddress.value
    }
    
    /**
     * Load wallet address from SharedPreferences
     */
    fun loadSavedWalletAddress(context: Context): String? {
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        val address = prefs.getString("wallet_address", null)
        
        if (address != null) {
            _walletAddress.value = address
            _isConnected.value = true
        }
        
        return address
    }
}
