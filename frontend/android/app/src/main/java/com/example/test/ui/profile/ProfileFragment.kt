package com.example.test.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test.R
import com.example.test.databinding.FragmentProfileBinding
import com.example.test.utils.LanguageManager
import com.example.test.wallet.WalletManager
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val TAG = "ProfileFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        
        // Load wallet data
        loadWalletInfo()
        
        // Setup language selector
        setupLanguageSelector()
        
        return binding.root
    }
    
    private fun loadWalletInfo() {
        // Check if wallet is connected
        val walletAddress = WalletManager.loadSavedWalletAddress(requireContext())
        
        if (walletAddress != null) {
            // Wallet is connected
            binding.chipWalletStatus.text = getString(R.string.active)
            binding.chipWalletStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            
            // Display shortened address
            val shortAddress = "${walletAddress.substring(0, 6)}...${walletAddress.substring(walletAddress.length - 4)}"
            binding.textWalletAddress.text = shortAddress
            
            Log.d(TAG, "Loading balance for wallet: $walletAddress")
            
            // Load token balance
            loadTokenBalance(walletAddress)
        } else {
            // No wallet connected
            binding.chipWalletStatus.text = getString(R.string.inactive)
            binding.chipWalletStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            binding.textWalletAddress.text = getString(R.string.no_wallet_connected)
            binding.textTokenBalance.text = "0"
        }
    }
    
    private fun loadTokenBalance(address: String) {
        lifecycleScope.launch {
            try {
                val balance = WalletManager.getTokenBalance(address)
                Log.d(TAG, "Token balance loaded: $balance CYC")
                
                // Display balance with 2 decimal places
                val balanceFormatted = String.format("%.2f", balance)
                binding.textTokenBalance.text = balanceFormatted
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading token balance: ${e.message}", e)
                binding.textTokenBalance.text = "0"
            }
        }
    }
    
    private fun setupLanguageSelector() {
        // Get available languages
        val languages = LanguageManager.getAvailableLanguages()
        val languageNames = languages.map { it.second }
        
        // Create adapter
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languageNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter
        
        // Set current selection
        val currentLanguage = LanguageManager.getSavedLanguage(requireContext())
        val currentIndex = languages.indexOfFirst { it.first == currentLanguage }
        if (currentIndex >= 0) {
            binding.spinnerLanguage.setSelection(currentIndex)
        }
        
        // Handle selection changes
        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position].first
                val currentSaved = LanguageManager.getSavedLanguage(requireContext())
                
                // Only change if different from current
                if (selectedLanguage != currentSaved) {
                    LanguageManager.saveLanguage(requireContext(), selectedLanguage)
                    
                    // Recreate activity to apply new language
                    requireActivity().recreate()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}