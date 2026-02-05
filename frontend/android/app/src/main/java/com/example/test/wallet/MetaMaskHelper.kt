package com.example.test.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * Helper class for MetaMask integration instructions and setup
 */
object MetaMaskHelper {
    
    /**
     * Show setup instructions dialog
     */
    fun showSetupInstructions(context: Context, onProceed: () -> Unit) {
        val message = """
            Per connettere MetaMask:
            
            1. Apri MetaMask sul telefono
            2. Aggiungi la rete Polygon Amoy:
               • Nome: Polygon Amoy
               • RPC: polygon-amoy.g.alchemy.com...
               • Chain ID: 80002
            3. Aggiungi il token CYC:
               • Indirizzo: 0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991
            4. Copia il tuo indirizzo wallet
            5. Torna all'app e incollalo
            
            Vuoi procedere?
        """.trimIndent()
        
        AlertDialog.Builder(context)
            .setTitle("Setup MetaMask")
            .setMessage(message)
            .setPositiveButton("Sì, procedi") { _, _ ->
                onProceed()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    /**
     * Show network setup dialog with copy buttons
     */
    fun showNetworkSetupDialog(context: Context, onComplete: () -> Unit) {
        val view = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        
        val message = android.widget.TextView(context).apply {
            text = """
                Configura Polygon Amoy in MetaMask:
                
                1. Apri MetaMask
                2. Tap su menu (☰) > Impostazioni > Reti
                3. Tap su "Aggiungi Rete"
                4. Usa questi dati:
            """.trimIndent()
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        view.addView(message)
        
        // RPC URL
        addCopyableField(context, view, "RPC URL", 
            "https://polygon-amoy.g.alchemy.com/v2/VdS_PBkq5kFNVYrolRkh4")
        
        // Chain ID
        addCopyableField(context, view, "Chain ID", "80002")
        
        // Symbol
        addCopyableField(context, view, "Simbolo", "MATIC")
        
        // Explorer
        addCopyableField(context, view, "Explorer", 
            "https://amoy.polygonscan.com")
        
        AlertDialog.Builder(context)
            .setTitle("Aggiungi Rete")
            .setView(view)
            .setPositiveButton("Fatto") { _, _ ->
                onComplete()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    /**
     * Show token setup dialog with copy button
     */
    fun showTokenSetupDialog(context: Context, onComplete: () -> Unit) {
        val view = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        
        val message = android.widget.TextView(context).apply {
            text = """
                Aggiungi token CYC in MetaMask:
                
                1. Apri MetaMask
                2. Assicurati di essere su Polygon Amoy
                3. Tap su "Importa Token"
                4. Incolla l'indirizzo:
            """.trimIndent()
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        view.addView(message)
        
        // Contract address
        addCopyableField(context, view, "Indirizzo Contratto", 
            "0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991")
        
        // Symbol
        val symbolLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val symbolLabel = android.widget.TextView(context).apply {
            text = "Simbolo: CYC"
            textSize = 14f
        }
        symbolLayout.addView(symbolLabel)
        view.addView(symbolLayout)
        
        val decimalsLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val decimalsLabel = android.widget.TextView(context).apply {
            text = "Decimali: 18"
            textSize = 14f
        }
        decimalsLayout.addView(decimalsLabel)
        view.addView(decimalsLayout)
        
        AlertDialog.Builder(context)
            .setTitle("Aggiungi Token CYC")
            .setView(view)
            .setPositiveButton("Fatto") { _, _ ->
                onComplete()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    /**
     * Add a copyable field to a view
     */
    private fun addCopyableField(context: Context, parent: android.widget.LinearLayout, 
                                  label: String, value: String) {
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val textView = android.widget.TextView(context).apply {
            text = "$label:\n$value"
            textSize = 12f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        val copyButton = android.widget.Button(context).apply {
            text = "Copia"
            setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "$label copiato!", Toast.LENGTH_SHORT).show()
            }
        }
        
        layout.addView(textView)
        layout.addView(copyButton)
        parent.addView(layout)
    }
    
    /**
     * Validate Ethereum address
     */
    fun isValidAddress(address: String): Boolean {
        return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
    }
    
    /**
     * Shorten address for display
     */
    fun shortenAddress(address: String): String {
        if (address.length < 10) return address
        return "${address.substring(0, 6)}...${address.substring(address.length - 4)}"
    }
}
