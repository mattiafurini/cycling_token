# CyclingToken (CYCL) - Proof of Physical Work

Welcome to **CyclingToken**, a mobile-first "Bike-to-Earn" application that rewards users with ERC-20 tokens for verified physical activity.

**Core Concept**: This is a **Proof of Physical Work** protocol. The blockchain serves as an immutable registry where tokens are minted only when physical effort (cycling) is cryptographically verified via GPS data and IPFS storage.

---

## 📱 Project Focus: Android Application

This project is designed as a native mobile experience. The web interface exists primarily for testing and admin visualization.

### Key Features
*   **Ride Tracking**: Real-time GPS tracking of rides using the Android device.
*   **Proof of Ride**: Ride data (GPX/JSON) is uploaded to **IPFS** to create a permanent, tamper-proof record of the physical activity.
*   **Gasless Minting**: Users receive rewards without paying gas fees. The backend verifies the IPFS data and handles the blockchain transaction.
*   **Wallet Connect**: Seamless login with MetaMask, Rabby, or other Web3 wallets directly on mobile.

---

## 🛠 Technology Stack

### Mobile App (Android)
*   **Platform**: Native Android (Kotlin/Java)
*   **Web3 Integration**: WalletConnect / Reown AppKit
*   **Location**: Native GPS Services
*   **Networking**: Retrofit / OkHttp for communicating with the Backend Node.js

### Backend (The "Oracle")
*   **Runtime**: Node.js + Express
*   **Database**: PostgreSQL (Stores user profiles and pending balances)
*   **Storage**: IPFS (Pinata) for decentralized storage of ride data.
*   **Security**: Validates GPS telemetry before authorizing Minting on-chain.

### Blockchain (Polygon Amoy / Mainnet)
*   **Contract**: Solidity ERC-20 (Custom `RideMinted` logic)
*   **Framework**: Hardhat
*   **Network**: Polygon POS (Fast & Low Cost)

---

## 📂 Project Structure

*   **`android/`**: The main Android Studio project source code.
*   **`backend/`**: The Node.js API server that acts as the bridge between the App, IPFS, and the Blockchain.
*   **`contracts/`**: Solidity Smart Contracts.
*   **`frontend/`** *(Legacy/Test)*: A React web dashboard used for initial testing and contract interactions.

---

## 🚀 How It Works (The Flow)

1.  **Ride**: The user starts a ride on the Android App.
2.  **Upload**: When finished, the app uploads the GPS data to the Backend.
3.  **Verify**: The Backend calculates the reward based on distance/elevation and uploads the proof to **IPFS**.
4.  **Mint**: The Backend calls the `mint()` function on the Smart Contract, passing the user's address and the IPFS CID.
5.  **Reward**: The Smart Contract mints **CYCL** tokens to the user and logs the IPFS CID on-chain as proof.

---

## ⚙️ Setup & Installation

### 1. Backend Setup
The backend is required for the app to function (Minting & Database).

```bash
# Navigate to backend
cd backend

# Install dependencies
npm install

# Configure .env (See example below)
cp .env.example .env

# Start Server
node server.js
```

### 2. Smart Contract (Deploy)
If you need to deploy a new version of the contract:

```bash
npx hardhat run scripts/deploy_amoy.js --network amoy
```

### 3. Running the Android App
1.  Open the `android/` folder in **Android Studio**.
2.  Sync Gradle files.
3.  Connect a physical device via USB or use an Emulator.
4.  **Important**: Ensure your phone is on the same Wi-Fi as your PC if using a local backend, or update the `BASE_URL` in the Android code to point to your Azure/Cloud instance.
5.  Build & Run.

---

## 🔐 Security & "Proof of Physical Work"
To prevent cheating (GPS spoofing), the system implements a hybrid verification model:
*   **Off-Chain**: The backend analyzes speed, elevation changes, and consistency of GPS points.
*   **On-Chain**: The blockchain stores the *Result* (Tokens) and the *Reference* (IPFS CID), allowing anyone to audit the physical work that generated the tokens.

---

## 📝 License
MIT
