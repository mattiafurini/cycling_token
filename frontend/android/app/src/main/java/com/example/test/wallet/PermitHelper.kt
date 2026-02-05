package com.example.test.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.test.utils.Constants
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Helper for EIP-712 typed data signing for ERC20 Permit
 * 
 * Since we can't sign directly in the app (no private key access),
 * we'll generate the typed data and open MetaMask to sign.
 */
object PermitHelper {
    
    // Type hashes (keccak256 of the type strings)
    private val PERMIT_TYPEHASH = Hash.sha3String(
        "Permit(address owner,address spender,uint256 value,uint256 nonce,uint256 deadline)"
    )
    
    private val EIP712_DOMAIN_TYPEHASH = Hash.sha3String(
        "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
    )
    
    /**
     * Data class for Permit parameters
     */
    data class PermitParams(
        val owner: String,
        val spender: String,
        val value: BigInteger,
        val nonce: BigInteger,
        val deadline: Long,
        val chainId: Long,
        val tokenName: String,
        val contractAddress: String
    )
    
    /**
     * Build the domain separator for EIP-712
     */
    fun buildDomainSeparator(
        tokenName: String,
        version: String,
        chainId: Long,
        contractAddress: String
    ): ByteArray {
        val nameHash = Hash.sha3(tokenName.toByteArray(StandardCharsets.UTF_8))
        val versionHash = Hash.sha3(version.toByteArray(StandardCharsets.UTF_8))
        
        val encodedDomain = encodePackedForDomain(
            Numeric.hexStringToByteArray(EIP712_DOMAIN_TYPEHASH),
            Numeric.hexStringToByteArray(Numeric.toHexString(nameHash)),
            Numeric.hexStringToByteArray(Numeric.toHexString(versionHash)),
            padTo32Bytes(BigInteger.valueOf(chainId)),
            padAddress(contractAddress)
        )
        
        return Hash.sha3(encodedDomain)
    }
    
    /**
     * Build the struct hash for Permit
     */
    fun buildPermitStructHash(
        owner: String,
        spender: String,
        value: BigInteger,
        nonce: BigInteger,
        deadline: Long
    ): ByteArray {
        val encodedPermit = encodePackedForPermit(
            Numeric.hexStringToByteArray(PERMIT_TYPEHASH),
            padAddress(owner),
            padAddress(spender),
            padTo32Bytes(value),
            padTo32Bytes(nonce),
            padTo32Bytes(BigInteger.valueOf(deadline))
        )
        
        return Hash.sha3(encodedPermit)
    }
    
    /**
     * Build the full digest to be signed (EIP-712)
     */
    fun buildDigest(params: PermitParams): ByteArray {
        val domainSeparator = buildDomainSeparator(
            params.tokenName,
            "1",
            params.chainId,
            params.contractAddress
        )
        
        val structHash = buildPermitStructHash(
            params.owner,
            params.spender,
            params.value,
            params.nonce,
            params.deadline
        )
        
        // \x19\x01 ++ domainSeparator ++ structHash
        val prefix = byteArrayOf(0x19.toByte(), 0x01.toByte())
        val data = prefix + domainSeparator + structHash
        
        return Hash.sha3(data)
    }
    
    /**
     * Get the digest as hex string for display/verification
     */
    fun getDigestHex(params: PermitParams): String {
        return Numeric.toHexString(buildDigest(params))
    }
    
    /**
     * Open MetaMask with a message signing request
     * Unfortunately, MetaMask mobile doesn't support eth_signTypedData via deep link,
     * so we need to use a dApp browser approach or manual copy/paste
     */
    fun openMetaMaskForSigning(context: Context) {
        try {
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
     * Generate EIP-712 JSON for display/copy
     */
    fun generateTypedDataJson(params: PermitParams): String {
        return """
{
    "types": {
        "EIP712Domain": [
            {"name": "name", "type": "string"},
            {"name": "version", "type": "string"},
            {"name": "chainId", "type": "uint256"},
            {"name": "verifyingContract", "type": "address"}
        ],
        "Permit": [
            {"name": "owner", "type": "address"},
            {"name": "spender", "type": "address"},
            {"name": "value", "type": "uint256"},
            {"name": "nonce", "type": "uint256"},
            {"name": "deadline", "type": "uint256"}
        ]
    },
    "primaryType": "Permit",
    "domain": {
        "name": "${params.tokenName}",
        "version": "1",
        "chainId": ${params.chainId},
        "verifyingContract": "${params.contractAddress}"
    },
    "message": {
        "owner": "${params.owner}",
        "spender": "${params.spender}",
        "value": "${params.value}",
        "nonce": "${params.nonce}",
        "deadline": "${params.deadline}"
    }
}
        """.trimIndent()
    }
    
    // Helper functions
    
    private fun padTo32Bytes(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        val result = ByteArray(32)
        
        if (bytes.size <= 32) {
            // Copy to the right (big-endian)
            System.arraycopy(bytes, 0, result, 32 - bytes.size, bytes.size)
        } else {
            // Take rightmost 32 bytes
            System.arraycopy(bytes, bytes.size - 32, result, 0, 32)
        }
        
        // Handle negative numbers (leading 0x00 for positive, or 0xFF stripped)
        if (bytes.isNotEmpty() && bytes[0] == 0.toByte() && bytes.size > 1) {
            val trimmed = bytes.copyOfRange(1, bytes.size)
            val r = ByteArray(32)
            System.arraycopy(trimmed, 0, r, 32 - trimmed.size, trimmed.size)
            return r
        }
        
        return result
    }
    
    private fun padAddress(address: String): ByteArray {
        val cleanAddress = if (address.startsWith("0x")) address.substring(2) else address
        val addressBytes = Numeric.hexStringToByteArray(cleanAddress)
        val result = ByteArray(32)
        System.arraycopy(addressBytes, 0, result, 32 - addressBytes.size, addressBytes.size)
        return result
    }
    
    private fun encodePackedForDomain(vararg parts: ByteArray): ByteArray {
        val totalSize = parts.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (part in parts) {
            System.arraycopy(part, 0, result, offset, part.size)
            offset += part.size
        }
        return result
    }
    
    private fun encodePackedForPermit(vararg parts: ByteArray): ByteArray {
        val totalSize = parts.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (part in parts) {
            System.arraycopy(part, 0, result, offset, part.size)
            offset += part.size
        }
        return result
    }
}
