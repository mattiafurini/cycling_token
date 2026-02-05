package com.example.test.wallet

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.utils.Constants

/**
 * WebView Activity for EIP-712 signing via MetaMask browser
 * This opens a local HTML page that connects to MetaMask and signs the permit
 */
class SigningActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_TYPED_DATA = "typed_data"
        const val EXTRA_OWNER = "owner"
        const val EXTRA_SPENDER = "spender"
        const val EXTRA_VALUE = "value"
        const val EXTRA_NONCE = "nonce"
        const val EXTRA_DEADLINE = "deadline"
        const val EXTRA_CHAIN_ID = "chain_id"
        const val EXTRA_CONTRACT = "contract"
        const val EXTRA_TOKEN_NAME = "token_name"
        
        const val RESULT_SIGNATURE = "signature"
        const val REQUEST_CODE = 1001
    }
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    
    private val TAG = "SigningActivity"
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple layout with WebView
        webView = WebView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        progressBar = ProgressBar(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        
        val container = android.widget.FrameLayout(this).apply {
            addView(webView)
            addView(progressBar)
        }
        setContentView(container)
        
        // Get permit params from intent
        val owner = intent.getStringExtra(EXTRA_OWNER) ?: ""
        val spender = intent.getStringExtra(EXTRA_SPENDER) ?: ""
        val value = intent.getStringExtra(EXTRA_VALUE) ?: "0"
        val nonce = intent.getStringExtra(EXTRA_NONCE) ?: "0"
        val deadline = intent.getLongExtra(EXTRA_DEADLINE, 0)
        val chainId = intent.getLongExtra(EXTRA_CHAIN_ID, Constants.CHAIN_ID)
        val contract = intent.getStringExtra(EXTRA_CONTRACT) ?: Constants.CONTRACT_ADDRESS
        val tokenName = intent.getStringExtra(EXTRA_TOKEN_NAME) ?: "CyclingToken"
        
        setupWebView()
        loadSigningPage(owner, spender, value, nonce, deadline, chainId, contract, tokenName)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        // Add JavaScript interface to receive signature
        webView.addJavascriptInterface(SigningInterface(), "Android")
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Handle MetaMask deep links
                if (url.startsWith("metamask://") || url.startsWith("https://metamask.app.link")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Cannot open MetaMask: ${e.message}")
                    }
                }
                return false
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "WebView Console: ${consoleMessage?.message()}")
                return true
            }
        }
    }
    
    private fun loadSigningPage(
        owner: String,
        spender: String,
        value: String,
        nonce: String,
        deadline: Long,
        chainId: Long,
        contract: String,
        tokenName: String
    ) {
        val html = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Firma Acquisto</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #00C853 0%, #00A843 100%);
            min-height: 100vh;
            padding: 20px;
            color: #fff;
        }
        .container {
            max-width: 400px;
            margin: 0 auto;
            background: rgba(255,255,255,0.95);
            border-radius: 24px;
            padding: 24px;
            color: #333;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        h1 {
            font-size: 24px;
            margin-bottom: 8px;
            color: #00C853;
        }
        .subtitle {
            color: #666;
            margin-bottom: 24px;
        }
        .info-box {
            background: #f5f5f5;
            border-radius: 12px;
            padding: 16px;
            margin-bottom: 16px;
        }
        .info-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #e0e0e0;
        }
        .info-row:last-child { border: none; }
        .info-label { color: #888; }
        .info-value { 
            font-weight: 600; 
            word-break: break-all;
            text-align: right;
            max-width: 60%;
        }
        .btn {
            width: 100%;
            padding: 16px;
            border: none;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            margin-top: 12px;
            transition: all 0.2s;
        }
        .btn-primary {
            background: #00C853;
            color: white;
        }
        .btn-primary:hover { background: #00A843; }
        .btn-primary:disabled {
            background: #ccc;
            cursor: not-allowed;
        }
        .btn-secondary {
            background: #f5f5f5;
            color: #333;
        }
        .status {
            text-align: center;
            padding: 16px;
            border-radius: 12px;
            margin-top: 16px;
        }
        .status.loading { background: #fff3cd; color: #856404; }
        .status.success { background: #d4edda; color: #155724; }
        .status.error { background: #f8d7da; color: #721c24; }
        .spinner {
            display: inline-block;
            width: 20px;
            height: 20px;
            border: 3px solid #f3f3f3;
            border-top: 3px solid #00C853;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-right: 8px;
            vertical-align: middle;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .hidden { display: none; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔐 Firma Acquisto</h1>
        <p class="subtitle">Autorizza il pagamento con MetaMask</p>
        
        <div class="info-box">
            <div class="info-row">
                <span class="info-label">Da:</span>
                <span class="info-value" id="owner">${owner.take(10)}...${owner.takeLast(8)}</span>
            </div>
            <div class="info-row">
                <span class="info-label">A:</span>
                <span class="info-value" id="spender">${spender.take(10)}...${spender.takeLast(8)}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Importo:</span>
                <span class="info-value" id="value">${value} CYCL</span>
            </div>
        </div>
        
        <button id="connectBtn" class="btn btn-primary" onclick="connectAndSign()">
            🦊 Connetti MetaMask e Firma
        </button>
        
        <button id="cancelBtn" class="btn btn-secondary" onclick="cancel()">
            Annulla
        </button>
        
        <div id="status" class="status hidden"></div>
    </div>

    <script>
        const permitData = {
            owner: "$owner",
            spender: "$spender",
            value: "$value",
            nonce: "$nonce",
            deadline: $deadline,
            chainId: $chainId,
            contract: "$contract",
            tokenName: "$tokenName"
        };
        
        function showStatus(message, type) {
            const status = document.getElementById('status');
            status.className = 'status ' + type;
            status.innerHTML = type === 'loading' 
                ? '<div class="spinner"></div>' + message 
                : message;
            status.classList.remove('hidden');
        }
        
        function hideStatus() {
            document.getElementById('status').classList.add('hidden');
        }
        
        async function connectAndSign() {
            const btn = document.getElementById('connectBtn');
            btn.disabled = true;
            
            // Check if we're in MetaMask browser or have window.ethereum
            if (typeof window.ethereum === 'undefined') {
                showStatus('⚠️ MetaMask non rilevato. Apri questa pagina nel browser di MetaMask.', 'error');
                
                // Try to open in MetaMask browser
                setTimeout(() => {
                    const currentUrl = window.location.href;
                    const metamaskUrl = 'https://metamask.app.link/dapp/' + currentUrl.replace('https://', '').replace('http://', '');
                    window.location.href = metamaskUrl;
                }, 2000);
                
                btn.disabled = false;
                return;
            }
            
            try {
                showStatus('Connessione a MetaMask...', 'loading');
                
                // Request accounts
                const accounts = await window.ethereum.request({ 
                    method: 'eth_requestAccounts' 
                });
                
                if (!accounts || accounts.length === 0) {
                    throw new Error('Nessun account connesso');
                }
                
                const account = accounts[0];
                console.log('Connected account:', account);
                
                // Verify it's the correct account
                if (account.toLowerCase() !== permitData.owner.toLowerCase()) {
                    showStatus('⚠️ Account sbagliato! Connetti: ' + permitData.owner.substring(0,10) + '...', 'error');
                    btn.disabled = false;
                    return;
                }
                
                showStatus('Firma in corso...', 'loading');
                
                // Prepare EIP-712 typed data
                const domain = {
                    name: permitData.tokenName,
                    version: "1",
                    chainId: permitData.chainId,
                    verifyingContract: permitData.contract
                };
                
                const types = {
                    Permit: [
                        { name: "owner", type: "address" },
                        { name: "spender", type: "address" },
                        { name: "value", type: "uint256" },
                        { name: "nonce", type: "uint256" },
                        { name: "deadline", type: "uint256" }
                    ]
                };
                
                const message = {
                    owner: permitData.owner,
                    spender: permitData.spender,
                    value: permitData.value,
                    nonce: permitData.nonce,
                    deadline: permitData.deadline.toString()
                };
                
                // Build the full typed data
                const typedData = JSON.stringify({
                    types: {
                        EIP712Domain: [
                            { name: "name", type: "string" },
                            { name: "version", type: "string" },
                            { name: "chainId", type: "uint256" },
                            { name: "verifyingContract", type: "address" }
                        ],
                        Permit: types.Permit
                    },
                    primaryType: "Permit",
                    domain: domain,
                    message: message
                });
                
                console.log('Signing typed data:', typedData);
                
                // Sign with eth_signTypedData_v4
                const signature = await window.ethereum.request({
                    method: 'eth_signTypedData_v4',
                    params: [account, typedData]
                });
                
                console.log('Signature:', signature);
                
                showStatus('✅ Firma completata!', 'success');
                
                // Send signature back to Android
                setTimeout(() => {
                    if (window.Android) {
                        window.Android.onSignatureReceived(signature);
                    }
                }, 1000);
                
            } catch (error) {
                console.error('Signing error:', error);
                showStatus('❌ Errore: ' + (error.message || 'Firma rifiutata'), 'error');
                btn.disabled = false;
            }
        }
        
        function cancel() {
            if (window.Android) {
                window.Android.onCancel();
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(
            "https://cycling-token.app/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }
    
    /**
     * JavaScript interface for communication between WebView and Android
     */
    inner class SigningInterface {
        
        @JavascriptInterface
        fun onSignatureReceived(signature: String) {
            Log.d(TAG, "Signature received: $signature")
            
            runOnUiThread {
                val resultIntent = Intent().apply {
                    putExtra(RESULT_SIGNATURE, signature)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
        
        @JavascriptInterface
        fun onCancel() {
            Log.d(TAG, "User cancelled signing")
            
            runOnUiThread {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        
        @JavascriptInterface
        fun onError(message: String) {
            Log.e(TAG, "Signing error: $message")
            
            runOnUiThread {
                Toast.makeText(this@SigningActivity, "Errore: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            setResult(Activity.RESULT_CANCELED)
            super.onBackPressed()
        }
    }
}
