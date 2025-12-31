# CyclingToken (CYCL) - Project 2.0

Welcome to the CyclingToken repository! This project is a "Bike-to-Earn" application that rewards users with ERC-20 tokens for cycling kilometers.

The project has been updated to support rapid local development and cost-effective deployment on Polygon Amoy.

---

## 📂 Project Structure

Here are the main files and folders that have been created or modified:

### Smart Contracts & Backend (Hardhat)
- **`contracts/CyclingToken.sol`**: The ERC-20 Smart Contract for the token.
- **`hardhat.config.js`**: Updated configuration to support **Localhost** and **Polygon Amoy**.
- **`test/CyclingToken.js`** (New): Automated test suite to verify contract functionality without spending gas.
- **`scripts/deploy_amoy.js`** (New): Specific script for deployment on the Polygon Amoy testnet.

### Frontend (React + Vite)
- **`frontend/`** (New): Folder containing the web application.
  - **`src/App.jsx`**: Main UI logic and Wallet connection.
  - **`src/index.css`**: Global styles with premium "Dark Mode" design.
  - **`package.json`**: Dependency management (Vite, Ethers, Framer Motion).

### Backend (Node.js + Express)
- **`backend/`** (New): API server for data and database management.
  - **`server.js`**: Express server entry point.
  - **`package.json`**: Backend dependencies (Express, CORS, Dotenv).

---

## 🌟 New Features (v2.1 - Server-Side Minting)

### 1. Gasless Claiming (Server-Side Minting)
Users no longer have to pay gas fees to claim tokens.
- **Frontend**: Sends a claim request to the server.
- **Server**: Verifies data and "mints" tokens directly to the user's wallet, paying for gas.
- **Advantage**: Smooth user experience (no signature popups) and zero cost for the end user.

### 2. IPFS Data Verification (Security) 🔒
Before minting tokens, the server performs a strict security check:
1.  **Downloads** raw ride data from IPFS (Pinata).
2.  **Recalculates** independently the sum of expected rewards.
3.  **Compares** the calculated total with the pending balance in the database.
4.  If the two values do not match, the transaction is blocked.
This guarantees that **every minted token is backed by real, immutable data on IPFS**.

### 3. Hybrid Architecture (IPFS + Blockchain)
To reduce gas costs and keep data decentralized:
- **Off-Chain Data (IPFS)**: Ride details are saved on IPFS via the Backend.
- **On-Chain Data (Polygon)**: The Smart Contract saves the CID (Content Identifier) as proof of work.

---

## 🚀 Quick Start Guide: How to Run Everything

### 1. Prerequisites
Make sure you have installed:
- Node.js (v18 or higher)
- A Wallet (Rabby or MetaMask) installed in the browser.
- PostgreSQL installed and active.

### 2. Database Setup (PostgreSQL)
1.  **Create User and Database**:
    ```bash
    sudo -u postgres psql -c "CREATE USER cycling_user WITH PASSWORD 'secure_password';"
    sudo -u postgres psql -c "CREATE DATABASE cycling_token_db OWNER cycling_user;"
    ```
2.  **Create Tables**:
    ```bash
    sudo -u postgres psql -d cycling_token_db -c "CREATE TABLE users (wallet_address VARCHAR(42) PRIMARY KEY, pending_balance NUMERIC DEFAULT 0, total_km NUMERIC DEFAULT 0, is_pro BOOLEAN DEFAULT FALSE, pro_expiry TIMESTAMP, pending_cids TEXT[] DEFAULT '{}');"
    sudo -u postgres psql -d cycling_token_db -c "GRANT ALL PRIVILEGES ON TABLE users TO cycling_user;"
    ```

### 3. Backend Setup (API Server)
Move to the backend folder:
```bash
cd backend
npm install
```

#### `.env` Configuration
Create a `.env` file in the `backend/` folder with the following data (CRITICAL):
```env
# Database
DB_USER=cycling_user
DB_HOST=localhost
DB_NAME=cycling_token_db
DB_PASSWORD=secure_password
DB_PORT=5432

# Blockchain (The Server pays Gas!)
RPC_URL=https://rpc-amoy.polygon.technology/
PRIVATE_KEY=your_wallet_private_key
CONTRACT_ADDRESS=0x2AAd40100641dBd6336eDC60832fc237bFe39C95

# IPFS
PINATA_JWT=your_long_jwt_key
```

Start the server:
```bash
node server.js
```
The server will be active at `http://localhost:3000`.

### 4. Frontend Setup (Web Interface)
Move to the frontend folder:
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173`, connect wallet, and start riding!

---

## 🛠 Technologies Used
- **Blockchain**: Solidity, Hardhat, Ethers.js
- **Server**: Node.js, Express, pg (PostgreSQL)
- **Frontend**: React 19, Vite, Reown AppKit (WalletConnect)
- **Storage**: Pinata (IPFS)
- **Network**: Polygon Amoy (Testnet)

---

## 📝 Development Notes
- **Design**: The interface uses pure CSS with variables for a modern and easy-to-modify look.
- **Compatibility**: The project is configured to run with Node.js v18 (LTS).

## ☁️ Deployment on Azure (Optional - Production)

To move the project from your local PC to a live server:

### 1. Create Azure VM
- **OS**: Ubuntu Server 22.04 LTS
- **Networking**: Allow ports 22 (SSH), 80 (HTTP), 443 (HTTPS), and 3000 (Node.js).

### 2. Install Dependencies on VM
Connect via SSH and install Node.js and PostgreSQL:
```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs git postgresql postgresql-contrib nginx
```

### 3. Deploy Code
Clone the repo and install dependencies:
```bash
git clone https://github.com/mattiafurini/cycling_token.git
cd cycling_token/backend
npm install
```
**Important**: Configure the `.env` file on the VM with the production values (Database user, Private Key, etc.).

### 4. Run Forever (PM2)
Use PM2 to keep the server running even if you disconnect:
```bash
sudo npm install -g pm2
pm2 start server.js --name "cycling-backend"
pm2 save
pm2 startup
```

### 5. Mobile App Connection (Android)
Android requires HTTPS or cleartext permission.
- **Option A (Easy)**: Open port 3000 on Azure Firewall and use `http://YOUR_VM_IP:3000`. Enable `usesCleartextTraffic` in `AndroidManifest.xml`.
- **Option B (Secure)**: Use Nginx as a Reverse Proxy to serve the API on port 80/443.

---

## 🔮 Roadmap
- **Disaster Recovery**: Automatic DB reconstruction starting from on-chain and IPFS data.
- **Mobile App**: Deployment of the Android version already configured in `frontend/android`.
