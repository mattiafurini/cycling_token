package com.example.test.utils

object Constants {
    // Backend Configuration
    // Server with nip.io domain
    const val BASE_URL = "https://cycling.98.66.138.159.nip.io/"
    
    // Previous addresses (backup reference)
    // const val BASE_URL = "http://98.66.138.159:3000/"
    // const val BASE_URL = "http://10.81.159.180:3000/"
    
    // Blockchain Configuration
    // IMPORTANTE: Deve corrispondere al backend server.js
    const val CONTRACT_ADDRESS = "0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991"
    const val RPC_URL = "https://polygon-amoy.g.alchemy.com/v2/VdS_PBkq5kFNVYrolRkh4"
    const val CHAIN_ID = 80002L // Polygon Amoy Testnet
    
    // IPFS/Pinata Configuration
    const val PINATA_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySW5mb3JtYXRpb24iOnsiaWQiOiIxOThkNzBjYy01ZDkyLTQyNmMtYTJkZS02NjQyMjdkZjE4YTMiLCJlbWFpbCI6Im1hdHRpYS5mdXJpbmlAc3R1ZGlvLnVuaWJvLml0IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInBpbl9wb2xpY3kiOnsicmVnaW9ucyI6W3siZGVzaXJlZFJlcGxpY2F0aW9uQ291bnQiOjEsImlkIjoiRlJBMSJ9LHsiZGVzaXJlZFJlcGxpY2F0aW9uQ291bnQiOjEsImlkIjoiTllDMSJ9XSwidmVyc2lvbiI6MX0sIm1mYV9lbmFibGVkIjpmYWxzZSwic3RhdHVzIjoiQUNUSVZFIn0sImF1dGhlbnRpY2F0aW9uVHlwZSI6InNjb3BlZEtleSIsInNjb3BlZEtleUtleSI6ImM5YTIzZmQ4NDA3NThkYjZmNjRhIiwic2NvcGVkS2V5U2VjcmV0IjoiMmJjODAwOTQwOTBmZmEyOGMyMWQ0N2VkMDY5ZjYxYWZhZTBjNTMxNDI1ZTQ2NDMyZmEyNDcxMTk4MTVlMWUzNSIsImV4cCI6MTc5NjM4NDc1NX0.Y_vUv-RgVWISVcQsaKpq9v09mfWhQBl6qfaT3_nHLEM"
    const val PINATA_GATEWAY = "https://gateway.pinata.cloud/ipfs/"
    
    // App Configuration
    const val TOKENS_PER_KM = 1
    const val MIN_SPEED_KMH = 5.0f
    const val MAX_SPEED_KMH = 50.0f
    
    // Timeout Configuration
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L // seconds
    const val WRITE_TIMEOUT = 30L // seconds
}
