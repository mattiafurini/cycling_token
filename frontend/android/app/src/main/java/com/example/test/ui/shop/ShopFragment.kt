package com.example.test.ui.shop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test.R

import com.example.test.databinding.FragmentShopBinding
import com.example.test.utils.Constants
import com.example.test.wallet.WalletManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    
    private val TAG = "ShopFragment"
    
    // Shop URL - backend serves the shop page
    private val SHOP_URL = Constants.BASE_URL.trimEnd('/') + "/shop.html"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        loadBalance()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh balance when returning from browser
        loadBalance()
    }
    
    private fun setupUI() {
        // Each buy button opens its specific product page in MetaMask
        binding.btnBuyItem1.setOnClickListener { openProductPage("cappuccio_pro") }
        binding.btnBuyItem2.setOnClickListener { openProductPage("borraccia_pro") }
        binding.btnBuyItem3.setOnClickListener { openProductPage("maglia_pro") }
    }
    
    private fun loadBalance() {
        val address = WalletManager.getCurrentAddress()
        if (address == null) {
            binding.tvBalance.text = "Wallet non connesso"
            return
        }
        
        binding.progressBalance.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val balance = withContext(Dispatchers.IO) {
                    WalletManager.getTokenBalance(address)
                }
                
                if (isAdded && _binding != null) {
                    binding.progressBalance.visibility = View.GONE
                    binding.tvBalance.text = String.format("%.2f CYCL", balance)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading balance", e)
                if (isAdded && _binding != null) {
                    binding.progressBalance.visibility = View.GONE
                    binding.tvBalance.text = "Errore"
                }
            }
        }
    }
    
    /**
     * Open a specific product page in MetaMask browser
     * @param productId: cappuccio_pro, borraccia_pro, or maglia_pro
     */
    private fun openProductPage(productId: String) {
        val address = WalletManager.getCurrentAddress()
        
        if (address == null) {
            Toast.makeText(requireContext(), getString(R.string.connect_wallet_first), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Build product-specific URL
        val baseUrl = Constants.BASE_URL.trimEnd('/')
        val productUrl = when (productId) {
            "cappuccio_pro" -> "$baseUrl/shop_item_1.html"
            "borraccia_pro" -> "$baseUrl/shop_item_2.html"
            "maglia_pro" -> "$baseUrl/shop_item_3.html"
            else -> "$baseUrl/shop.html" // Fallback to general shop
        }
        
        // Use MetaMask deep link to open dApp browser directly
        val metamaskDeepLink = "https://metamask.app.link/dapp/$productUrl"
        
        try {
            // Open MetaMask dApp browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(metamaskDeepLink))
            startActivity(intent)
            
            // Open MetaMask dApp browser silently
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MetaMask browser", e)
            
            // Fallback: show instructions
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("🛒 Shop")
                .setMessage("""
Per acquistare:

1. Apri l'app MetaMask
2. Vai nel Browser (icona 🌐 in basso)
3. Incolla questo URL:

$productUrl

L'URL è stato copiato negli appunti!
                """.trimIndent())
                .setPositiveButton("Ho capito") { _, _ -> }
                .show()
            
            // Copy URL to clipboard as fallback
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Shop URL", productUrl)
            clipboard.setPrimaryClip(clip)
            
            // URL copied silently
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}