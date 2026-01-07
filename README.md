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
*   **Platform**: Native Android (Kotlin)
*   **Web3 Integration**: Web3j v4.9.8 (Blockchain queries)
*   **Location**: GPS Simulation (no real GPS tracking required)
*   **Networking**: Retrofit 2.9.0 + OkHttp for Backend API
*   **Storage**: IPFS/Pinata (Client-side upload of ride metadata)

### Backend (The "Oracle")
*   **Runtime**: Node.js + Express
*   **Database**: PostgreSQL (Stores user profiles and pending balances)
*   **Storage**: IPFS (Pinata) for decentralized storage of ride data.
*   **Security**: Validates ride data before authorizing Minting on-chain.

### Blockchain (Polygon Amoy / Mainnet)
*   **Contract**: Solidity ERC-20 (Custom `RideMinted` logic)
*   **Framework**: Hardhat
*   **Network**: Polygon POS (Fast & Low Cost)

---

## 📂 Project Structure

*   **`/AndroidStudioProjects/test/`**: Native Android app (Kotlin) - production mobile client
*   **`backend/`**: Node.js API server that acts as the bridge between the App, IPFS, and the Blockchain
*   **`contracts/`**: Solidity Smart Contracts (CyclingToken ERC-20)
*   **`frontend/`** *(Test)*: React web dashboard for testing and contract interactions

---

## 🚀 How It Works (The Flow)

1.  **Ride**: User starts a ride on the Android App (GPS simulation)
2.  **Upload**: App uploads ride metadata (distance, reward) to **IPFS** via Pinata
3.  **Save**: App sends ride data to Backend `/api/ride` (includes IPFS CID)
4.  **Wait**: 4-second delay for Pinata synchronization
5.  **Claim**: App requests token minting via Backend `/api/claim`
6.  **Verify**: Backend validates IPFS data against database records
7.  **Mint**: Backend calls Smart Contract `mint()` function (gasless for user)
8.  **Reward**: User receives **CYCL** tokens, viewable in MetaMask and app Profile

**Reward Rate**: 1 CYCL token per kilometer

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
### 3. Running the Android App

**Quick Install (Recommended)**:
Download the latest APK from [GitHub Releases](https://github.com/mattiafurini/cycling_token/releases) and install on your Android device. The app is pre-configured to connect to the production server.

**Build from Source** (Developers only):
1.  Open `/AndroidStudioProjects/test/` in **Android Studio**
2.  Sync Gradle files
3.  Connect physical device via USB or use Emulator
4.  Build & Run: `./gradlew installDebug`

**First Use**: Go to Wallet tab → Follow guided setup → Add Polygon Amoy network + Token in MetaMask → Enter wallet addresshe Android App
1.  Open the `android/` folder in **Android Studio**.
2.  Sync Gradle files.
3.  Connect a physical device via USB or use an Emulator.
4.  **Important**: Ensure your phone is on the same Wi-Fi as your PC if using a local backend, or update the `BASE_URL` in the Android code to point to your Azure/Cloud instance.
5.  Build & Run.

---

## 🔐 Security & "Proof of Physical Work"
To prevent cheating (GPS spoofing), the system implements a hybrid verification model:
*   **On-Chain**: The blockchain stores the *Result* (Tokens) and the *Reference* (IPFS CID), allowing anyone to audit the physical work that generated the tokens.

---

## 📝 License
MIT
