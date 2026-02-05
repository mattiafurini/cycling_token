package com.example.test.wallet

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Uint
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import com.example.test.utils.Constants
import java.math.BigInteger

/**
 * Helper class for Web3j contract interactions
 */
object Web3jContractHelper {
    
    /**
     * Simple contract wrapper for balance queries
     */
    class SimpleContract(private val web3j: Web3j, private val contractAddress: String) {
        
        /**
         * Get token balance of an address
         */
        fun balanceOf(address: String): Result<BigInteger> {
            return try {
                android.util.Log.d("Web3jHelper", "Creating balanceOf function for address: $address")
                
                val function = Function(
                    "balanceOf",
                    listOf(Address(address)),
                    listOf(object : TypeReference<Uint256>() {})
                )
                
                val encodedFunction = FunctionEncoder.encode(function)
                android.util.Log.d("Web3jHelper", "Encoded function: $encodedFunction")
                android.util.Log.d("Web3jHelper", "Contract address: $contractAddress")
                
                val response: EthCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        address,
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).sendAsync().get()
                
                android.util.Log.d("Web3jHelper", "Response value: ${response.value}")
                android.util.Log.d("Web3jHelper", "Response error: ${response.error}")
                
                if (response.hasError()) {
                    android.util.Log.e("Web3jHelper", "RPC Error: ${response.error.message}")
                    return Result.failure(Exception("RPC Error: ${response.error.message}"))
                }
                
                val result = FunctionReturnDecoder.decode(
                    response.value,
                    function.outputParameters
                )
                
                android.util.Log.d("Web3jHelper", "Decoded result size: ${result.size}")
                
                if (result.isNotEmpty()) {
                    val balance = (result[0] as Uint256).value
                    android.util.Log.d("Web3jHelper", "Balance value: $balance")
                    Result.success(balance)
                } else {
                    android.util.Log.e("Web3jHelper", "No balance returned from decoder")
                    Result.failure(Exception("No balance returned"))
                }
            } catch (e: Exception) {
                android.util.Log.e("Web3jHelper", "Exception in balanceOf: ${e.message}", e)
                Result.failure(e)
            }
        }
        
        /**
         * Get total supply
         */
        fun totalSupply(): Result<BigInteger> {
            return try {
                val function = Function(
                    "totalSupply",
                    emptyList(),
                    listOf(object : TypeReference<Uint256>() {})
                )
                
                val encodedFunction = FunctionEncoder.encode(function)
                
                val response: EthCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).send()
                
                val result = FunctionReturnDecoder.decode(
                    response.value,
                    function.outputParameters
                )
                
                if (result.isNotEmpty()) {
                    Result.success((result[0] as Uint256).value)
                } else {
                    Result.failure(Exception("No supply returned"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        /**
         * Get nonce for EIP-2612 permit
         */
        fun nonces(address: String): Result<BigInteger> {
            return try {
                android.util.Log.d("Web3jHelper", "Getting nonce for address: $address")
                
                val function = Function(
                    "nonces",
                    listOf(Address(address)),
                    listOf(object : TypeReference<Uint256>() {})
                )
                
                val encodedFunction = FunctionEncoder.encode(function)
                
                val response: EthCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        address,
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).sendAsync().get()
                
                if (response.hasError()) {
                    android.util.Log.e("Web3jHelper", "RPC Error: ${response.error.message}")
                    return Result.failure(Exception("RPC Error: ${response.error.message}"))
                }
                
                val result = FunctionReturnDecoder.decode(
                    response.value,
                    function.outputParameters
                )
                
                if (result.isNotEmpty()) {
                    val nonce = (result[0] as Uint256).value
                    android.util.Log.d("Web3jHelper", "Nonce value: $nonce")
                    Result.success(nonce)
                } else {
                    Result.failure(Exception("No nonce returned"))
                }
            } catch (e: Exception) {
                android.util.Log.e("Web3jHelper", "Exception in nonces: ${e.message}", e)
                Result.failure(e)
            }
        }
        
        /**
         * Get token name for EIP-712 domain
         */
        fun name(): Result<String> {
            return try {
                val function = Function(
                    "name",
                    emptyList(),
                    listOf(object : TypeReference<org.web3j.abi.datatypes.Utf8String>() {})
                )
                
                val encodedFunction = FunctionEncoder.encode(function)
                
                val response: EthCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).sendAsync().get()
                
                if (response.hasError()) {
                    return Result.failure(Exception("RPC Error: ${response.error.message}"))
                }
                
                val result = FunctionReturnDecoder.decode(
                    response.value,
                    function.outputParameters
                )
                
                if (result.isNotEmpty()) {
                    val name = (result[0] as org.web3j.abi.datatypes.Utf8String).value
                    android.util.Log.d("Web3jHelper", "Token name: $name")
                    Result.success(name)
                } else {
                    Result.failure(Exception("No name returned"))
                }
            } catch (e: Exception) {
                android.util.Log.e("Web3jHelper", "Exception in name: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Load contract instance
     */
    fun loadContract(web3j: Web3j, address: String): SimpleContract {
        return SimpleContract(web3j, Constants.CONTRACT_ADDRESS)
    }
}
