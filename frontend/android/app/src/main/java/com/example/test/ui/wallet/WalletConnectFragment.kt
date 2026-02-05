package com.example.test.ui.wallet

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.test.R
import com.example.test.databinding.FragmentWalletConnectBinding
import com.example.test.wallet.MetaMaskHelper
import com.example.test.wallet.WalletManager
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.Result
import io.metamask.androidsdk.SDKOptions
import kotlinx.coroutines.launch

class WalletConnectFragment : Fragment() {

    private var _binding: FragmentWalletConnectBinding? = null
    private val binding get() = _binding!!
    
    private var ethereum: Ethereum? = null
    private val TAG = "WalletConnect"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletConnectBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize WalletManager
        WalletManager.initialize()
        
        // Check if wallet is already connected
        checkExistingConnection()

        binding.btnConnectWallet.setOnClickListener {
            connectWithMetaMask()
        }

        binding.btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_wallet_to_dashboard)
        }

        return root
    }
    
    /**
     * Called when fragment resumes - check if wallet was connected while away
     * This handles the case where MetaMask callback doesn't fire properly
     */
    override fun onResume() {
        super.onResume()
        
        // Small delay to allow SDK callback to complete
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_binding == null) return@postDelayed
            
            // First check saved wallet
            val savedAddress = WalletManager.loadSavedWalletAddress(requireContext())
            if (savedAddress != null) {
                Log.d(TAG, "onResume: Found saved wallet, navigating to dashboard")
                navigateToDashboard()
                return@postDelayed
            }
            
            // Also check if SDK has an address (callback completed but not saved yet)
            val sdkAddress = ethereum?.selectedAddress
            if (!sdkAddress.isNullOrEmpty()) {
                Log.d(TAG, "onResume: SDK has address: $sdkAddress, saving and navigating")
                onWalletConnected(sdkAddress)
                return@postDelayed
            }
            
            // No connection found, reset button
            resetConnectButton()
        }, 500) // 500ms delay to let SDK callback complete
    }
    
    /**
     * Navigate to dashboard safely
     */
    private fun navigateToDashboard() {
        try {
            findNavController().navigate(R.id.action_wallet_to_dashboard)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error", e)
        }
    }
    
    /**
     * Initialize MetaMask SDK
     */
    private fun initializeMetaMaskSDK() {
        try {
            val dappMetadata = DappMetadata(
                name = "CyclingToken",
                url = "https://cyclingtoken.app",
                iconUrl = "https://cyclingtoken.app/icon.png"
            )
            
            // Create Ethereum provider with callback API
            ethereum = Ethereum(
                context = requireContext(),
                dappMetadata = dappMetadata,
                sdkOptions = SDKOptions(
                    infuraAPIKey = null,
                    readonlyRPCMap = mapOf("0x13882" to "https://polygon-amoy.g.alchemy.com/v2/VdS_PBkq5kFNVYrolRkh4")
                )
            )
            
            Log.d(TAG, "MetaMask SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MetaMask SDK", e)
        }
    }
    
    /**
     * Connect using MetaMask SDK with callbacks
     */
    private fun connectWithMetaMask() {
        binding.btnConnectWallet.isEnabled = false
        binding.btnConnectWallet.text = getString(R.string.connecting)
        
        // Check if MetaMask is installed
        if (!isMetaMaskInstalled()) {
            showInstallMetaMaskDialog()
            return
        }
        
        try {
            // Initialize SDK if needed
            if (ethereum == null) {
                initializeMetaMaskSDK()
            }
            
            // Connect using callback API
            ethereum?.connect { result ->
                activity?.runOnUiThread {
                    when (result) {
                        is Result.Error -> {
                            Log.e(TAG, "Connection error: ${result.error.message}")
                            Toast.makeText(
                                context,
                                getString(R.string.connection_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                            resetConnectButton()
                        }
                        is Result.Success.Item -> {
                            val address = result.value
                            Log.d(TAG, "Connection successful: $address")
                            if (address.isNotEmpty()) {
                                // After connecting, switch to Polygon Amoy network
                                switchToPolygonAmoy(address)
                            } else {
                                resetConnectButton()
                            }
                        }
                        is Result.Success.Items -> {
                            val accounts = result.value
                            Log.d(TAG, "Got accounts: $accounts")
                            if (accounts.isNotEmpty()) {
                                // After connecting, switch to Polygon Amoy network
                                switchToPolygonAmoy(accounts.first())
                            } else {
                                resetConnectButton()
                            }
                        }
                        else -> {
                            Log.d(TAG, "Unknown result type")
                            resetConnectButton()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to MetaMask", e)
            // Fallback to manual connection
            showManualConnectDialog()
        }
    }
    
    /**
     * Switch MetaMask to Polygon Amoy network
     */
    private fun switchToPolygonAmoy(address: String) {
        val chainId = "0x13882" // Polygon Amoy = 80002 decimal
        
        // First try to switch to the chain
        val switchRequest = io.metamask.androidsdk.EthereumRequest(
            method = "wallet_switchEthereumChain",
            params = listOf(mapOf("chainId" to chainId))
        )
        
        ethereum?.sendRequest(switchRequest) { result ->
            activity?.runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Switched to Polygon Amoy")
                        // Now add the CYC token
                        addCyclingToken(address)
                    }
                    is Result.Error -> {
                        // Error 4902 means chain not added, need to add it
                        if (result.error.code == 4902 || result.error.message.contains("Unrecognized chain")) {
                            addPolygonAmoyNetwork(address)
                        } else {
                            Log.e(TAG, "Switch chain error: ${result.error.message}")
                            // Still add token and save wallet
                            addCyclingToken(address)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Add Polygon Amoy network to MetaMask if not exists
     */
    private fun addPolygonAmoyNetwork(address: String) {
        val addChainRequest = io.metamask.androidsdk.EthereumRequest(
            method = "wallet_addEthereumChain",
            params = listOf(mapOf(
                "chainId" to "0x13882",
                "chainName" to "Polygon Amoy Testnet",
                "nativeCurrency" to mapOf(
                    "name" to "POL",
                    "symbol" to "POL",
                    "decimals" to 18
                ),
                "rpcUrls" to listOf("https://rpc-amoy.polygon.technology"),
                "blockExplorerUrls" to listOf("https://amoy.polygonscan.com")
            ))
        )
        
        ethereum?.sendRequest(addChainRequest) { result ->
            activity?.runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Polygon Amoy network added")
                        // Now add the CYC token
                        addCyclingToken(address)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Add chain error: ${result.error.message}")
                        // Still try to add token
                        addCyclingToken(address)
                    }
                }
            }
        }
    }
    
    /**
     * Add CYC token to MetaMask using wallet_watchAsset
     */
    private fun addCyclingToken(address: String) {
        val tokenRequest = io.metamask.androidsdk.EthereumRequest(
            method = "wallet_watchAsset",
            params = listOf(mapOf(
                "type" to "ERC20",
                "options" to mapOf(
                    "address" to "0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991",
                    "symbol" to "CYCL",
                    "decimals" to 18
                )
            ))
        )
        
        ethereum?.sendRequest(tokenRequest) { result ->
            activity?.runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "CYCL token added to MetaMask")
                        onWalletConnected(address)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Add token error: ${result.error.message}")
                        // Still save wallet even if token add fails
                        onWalletConnected(address)
                    }
                }
            }
        }
    }
    
    /**
     * Check if MetaMask app is installed
     */
    private fun isMetaMaskInstalled(): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo("io.metamask", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Show dialog to install MetaMask
     */
    private fun showInstallMetaMaskDialog() {
        resetConnectButton()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.metamask_not_found))
            .setMessage(getString(R.string.install_metamask_message))
            .setPositiveButton(getString(R.string.install)) { _, _ ->
                // Open Play Store
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=io.metamask")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=io.metamask")
                    )
                    startActivity(intent)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Fallback: Show manual connection dialog
     */
    private fun showManualConnectDialog() {
        resetConnectButton()
        
        val input = android.widget.EditText(requireContext())
        input.hint = "0x..."
        
        // Try to auto-paste from clipboard
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text.toString()
                if (MetaMaskHelper.isValidAddress(text)) {
                    input.setText(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard access error", e)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.connect_wallet))
            .setMessage(getString(R.string.paste_wallet_address))
            .setView(input)
            .setPositiveButton(getString(R.string.connect)) { _, _ ->
                val address = input.text.toString().trim()
                if (MetaMaskHelper.isValidAddress(address)) {
                    onWalletConnected(address)
                } else {
                    Toast.makeText(context, getString(R.string.invalid_address), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Check if there's an existing wallet connection
     */
    private fun checkExistingConnection() {
        val savedAddress = WalletManager.loadSavedWalletAddress(requireContext())
        if (savedAddress != null) {
            Log.d(TAG, "Found saved wallet: $savedAddress")
            // Auto-navigate to dashboard
            findNavController().navigate(R.id.action_wallet_to_dashboard)
        }
    }
    
    /**
     * Reset connect button to default state
     */
    private fun resetConnectButton() {
        binding.btnConnectWallet.isEnabled = true
        binding.btnConnectWallet.text = getString(R.string.connect_metamask)
    }
    
    /**
     * Handle successful wallet connection
     */
    private fun onWalletConnected(address: String) {
        lifecycleScope.launch {
            try {
                // Save wallet address
                WalletManager.setWalletAddress(requireContext(), address)
                
                Toast.makeText(
                    context, 
                    getString(R.string.wallet_connected, MetaMaskHelper.shortenAddress(address)), 
                    Toast.LENGTH_SHORT
                ).show()
                
                // Navigate to dashboard
                findNavController().navigate(R.id.action_wallet_to_dashboard)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving wallet", e)
                Toast.makeText(context, getString(R.string.error_saving_wallet), Toast.LENGTH_SHORT).show()
                resetConnectButton()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
