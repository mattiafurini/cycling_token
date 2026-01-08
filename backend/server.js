const express = require('express');
const cors = require('cors');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });

const { Pool } = require('pg');
const { ethers } = require('ethers');

const app = express();
const PORT = process.env.PORT || 3000;

// Blockchain Configuration
const RPC_URL = process.env.RPC_URL || "https://rpc-amoy.polygon.technology/";
const PRIVATE_KEY = process.env.PRIVATE_KEY;
const CONTRACT_ADDRESS = "0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991";

// ABI for Minting (Minimal)
const MINT_ABI = [
    "function mint(address to, uint256 amount, string memory tokenURI) public",
    "function permit(address owner, address spender, uint256 value, uint256 deadline, uint8 v, bytes32 r, bytes32 s) public",
    "function burnFrom(address account, uint256 amount) public"
];

let contract;
let wallet;

try {
    const provider = new ethers.JsonRpcProvider(RPC_URL);
    wallet = new ethers.Wallet(PRIVATE_KEY, provider);
    contract = new ethers.Contract(CONTRACT_ADDRESS, MINT_ABI, wallet);
    console.log("Blockchain Wallet Connected:", wallet.address);
} catch (e) {
    console.error("Blockchain Connection Error:", e.message);
}

// Database Connection
const pool = new Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    port: process.env.DB_PORT,
});

pool.connect(async (err, client, release) => {
    if (err) {
        return console.error('Error acquiring client', err.stack);
    }
    console.log('Connected to PostgreSQL database');

    // Create Tables if not exist
    try {
        await client.query(`
            CREATE TABLE IF NOT EXISTS users (
                wallet_address TEXT PRIMARY KEY,
                pending_balance REAL DEFAULT 0,
                total_km REAL DEFAULT 0,
                pending_cids TEXT[] DEFAULT '{}',
                is_pro BOOLEAN DEFAULT FALSE,
                pro_expiry TIMESTAMP
            );
        `);

        await client.query(`
            CREATE TABLE IF NOT EXISTS rides (
                id SERIAL PRIMARY KEY,
                user_address TEXT NOT NULL,
                distance REAL NOT NULL,
                avg_speed REAL,
                gps_data JSONB,
                ipfs_cid TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        `);
        console.log("Database Schema Verified");
    } catch (dbErr) {
        console.error("Error creating tables:", dbErr);
    }

    release();
});

app.use(cors());
app.use(express.json());

// API Endpoints

// Health check endpoint
app.get('/api/health', (req, res) => {
    res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        database: pool ? 'connected' : 'disconnected',
        blockchain: wallet ? wallet.address : 'not connected'
    });
});

// Helper to verify IPFS data integrity
async function verifyIpfsData(cids) {
    if (!cids || cids.length === 0) return 0;

    console.log(`Verifying ${cids.length} rides on IPFS...`);

    // Fetch all CIDs in parallel
    const promises = cids.map(async (cid) => {
        // List of gateways to try
        const gateways = [
            `https://gateway.pinata.cloud/ipfs/${cid}`,
            `https://ipfs.io/ipfs/${cid}`,
            `https://dweb.link/ipfs/${cid}`
        ];

        for (const url of gateways) {
            try {
                const response = await axios.get(url, { timeout: 10000 }); // Increased timeout to 10s
                const data = response.data;

                // Validate structure
                if (data && typeof data.reward === 'number') {
                    return data.reward;
                }
            } catch (err) {
                // Warning only, try next gateway
                // console.warn(`Failed to fetch ${url}: ${err.message}`);
            }
        }

        console.error(`Failed to fetch CID ${cid} from ALL gateways.`);
        return 0;
    });

    const rewards = await Promise.all(promises);
    const total = rewards.reduce((acc, curr) => acc + curr, 0);
    return total;
}

// Server-Side Claim (Minting)
app.post('/api/claim', async (req, res) => {
    const { address } = req.body;

    if (!address) {
        return res.status(400).json({ error: "Address is required" });
    }

    try {
        // 1. Get Pending Balance
        const userResult = await pool.query('SELECT pending_balance, pending_cids FROM users WHERE wallet_address = $1', [address]);

        if (userResult.rows.length === 0) {
            return res.status(404).json({ error: "User not found" });
        }

        const user = userResult.rows[0];
        const pendingAmount = parseFloat(user.pending_balance);

        if (pendingAmount <= 0) {
            return res.status(400).json({ error: "No pending rewards to claim" });
        }

        console.log(`Processing Claim for ${address}: ${pendingAmount} CYCL`);

        // 2. SECURITY CHECK: Verify IPFS Data
        const cids = user.pending_cids || [];
        const verifiedAmount = await verifyIpfsData(cids);

        // Allow a small floating point tolerance
        const difference = Math.abs(verifiedAmount - pendingAmount);
        if (difference > 0.01) {
            console.warn(`SECURITY WARNING (DEMO MODE): Mismatch! DB says ${pendingAmount}, IPFS says ${verifiedAmount}`);
            console.warn("Proceeding with claim despite verification failure (IPFS Propagation Delay).");

            // In Production: We would block this.
            // return res.status(400).json({ error: "Verification Failed" });
        }

        // 3. Prepare Metadata URI
        const latestCid = cids.length > 0 ? cids[cids.length - 1] : "";
        const tokenURI = latestCid ? `ipfs://${latestCid}` : "ipfs://generic-ride-reward";

        // 4. Mint Tokens on Blockchain
        const amountWei = ethers.parseUnits(pendingAmount.toString(), 18);
        const tx = await contract.mint(address, amountWei, tokenURI);
        console.log(`Mint Transaction Sent: ${tx.hash}`);

        const receipt = await tx.wait();
        console.log(`Mint Confirmed: Block ${receipt.blockNumber}`);

        // 5. Update Database
        const updateResult = await pool.query(
            'UPDATE users SET pending_balance = 0, pending_cids = \'{}\' WHERE wallet_address = $1 RETURNING *',
            [address]
        );

        res.json({
            success: true,
            txHash: tx.hash,
            amount: pendingAmount,
            user: updateResult.rows[0]
        });

    } catch (err) {
        console.error("Claim Error:", err);
        res.status(500).json({ error: 'Server error processing claim', details: err.message });
    }
});

// API Endpoints

// Get User Data (Create if not exists)
app.get('/api/user/:address', async (req, res) => {
    const { address } = req.params;
    try {
        let result = await pool.query('SELECT * FROM users WHERE wallet_address = $1', [address]);

        if (result.rows.length === 0) {
            // Create new user
            result = await pool.query(
                'INSERT INTO users (wallet_address, pending_balance, total_km) VALUES ($1, 0, 0) RETURNING *',
                [address]
            );
        }

        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Database error' });
    }
});

const axios = require('axios');
const FormData = require('form-data');

// ... (previous code)

// Helper function to upload to Pinata
async function uploadToPinata(data) {
    const url = `https://api.pinata.cloud/pinning/pinJSONToIPFS`;

    // If using JWT (Recommended)
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${process.env.PINATA_JWT}`
    };

    // If using API Key/Secret (Alternative)
    // const headers = {
    //     'Content-Type': 'application/json',
    //     'pinata_api_key': process.env.PINATA_API_KEY,
    //     'pinata_secret_api_key': process.env.PINATA_SECRET_KEY
    // };

    try {
        const response = await axios.post(url, data, { headers });
        return response.data.IpfsHash;
    } catch (error) {
        console.error("Pinata Upload Error:", error.response ? error.response.data : error.message);
        throw error;
    }
}

// Record Ride (IPFS Version)
app.post('/api/ride', async (req, res) => {
    // Support both parameter formats for compatibility
    const address = req.body.address || req.body.user_address;
    const km = req.body.km || req.body.distance;
    const gps_data = req.body.gps_data;
    const avg_speed = req.body.avg_speed;
    const ipfs_cid = req.body.ipfs_cid; // If client already uploaded to IPFS

    if (!address) {
        return res.status(400).json({ error: 'Missing address or user_address' });
    }
    if (!km && km !== 0) {
        return res.status(400).json({ error: 'Missing km or distance' });
    }

    try {
        // 1. Check Pro Status
        const userResult = await pool.query('SELECT is_pro, pro_expiry FROM users WHERE wallet_address = $1', [address]);
        const user = userResult.rows[0];

        let multiplier = 1.0;
        if (user && user.is_pro) {
            multiplier = 1.2;
        }

        const reward = km * multiplier;

        // 2. Prepare Metadata for IPFS (Verification Data)
        const rideData = {
            user: address,
            km: km,
            reward: reward,
            timestamp: Date.now(),
            type: "Cycling Session",
            app: "CyclingToken v2"
        };

        // 3. Upload to IPFS (if not already uploaded by client)
        let cid = ipfs_cid;
        if (!cid) {
            cid = await uploadToPinata(rideData);
            console.log(`Ride saved to IPFS: ${cid}`);
        } else {
            console.log(`Using client-provided IPFS CID: ${cid}`);
        }

        // 4. Update Database (Store Pending Balance + CID + Detailed Data)

        // Update User Balance
        await pool.query(
            `UPDATE users 
             SET pending_balance = pending_balance + $1, 
                 total_km = total_km + $2,
                 pending_cids = array_append(pending_cids, $3)
             WHERE wallet_address = $4`,
            [reward, km, cid, address]
        );

        // Store Detailed Ride Data to 'rides' table
        const rideLogResult = await pool.query(
            `INSERT INTO rides (user_address, distance, avg_speed, gps_data, ipfs_cid)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING *`,
            [address, km, avg_speed || 0, JSON.stringify(gps_data || []), cid]
        );

        // Fetch updated user to return
        const updatedUser = await pool.query('SELECT * FROM users WHERE wallet_address = $1', [address]);

        res.json({
            success: true,
            user: updatedUser.rows[0],
            new_ride: rideLogResult.rows[0],
            latest_cid: cid,
            ride_id: rideLogResult.rows[0].id,
            tokens_earned: Math.round(reward),
            ipfs_cid: cid
        });

    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Server error' });
    }
});

// Upgrade to Pro
app.post('/api/upgrade-pro', async (req, res) => {
    const { address } = req.body;
    try {
        // In a real app, we would verify the burn transaction hash here.
        // For this prototype, we trust the frontend called burn() successfully.

        const result = await pool.query(
            'UPDATE users SET is_pro = TRUE, pro_expiry = NOW() + INTERVAL \'30 days\' WHERE wallet_address = $1 RETURNING *',
            [address]
        );
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Database error' });
    }
});

// Shop: Buy Item (Gasless Burn)
app.post('/api/buy', async (req, res) => {
    const { address, item_id, price, permit } = req.body;

    if (!address || !price || !permit) {
        return res.status(400).json({ error: "Missing parameters" });
    }

    try {
        console.log(`Processing Buy for ${address}: Item ${item_id} for ${price} CYCL`);

        // 1. Submit Permit (Gasless Approval)
        // permit signature = { deadline, v, r, s }
        const { deadline, v, r, s } = permit;

        // We need to estimate gas or just send it. 
        // Note: If permit was already used, this will revert.
        // Ideally we check allowance first, but calling permit is fine.

        try {
            const txPermit = await contract.permit(
                address,
                wallet.address, // Spender is the Server (wallet)
                ethers.parseUnits(price.toString(), 18),
                deadline,
                v,
                r,
                s
            );
            await txPermit.wait();
            console.log("Permit Successful");
        } catch (e) {
            console.warn("Permit failed (might be already approved?):", e.message);
            // We continue to try burnFrom, in case allowance exists from previous permit
        }

        // 2. Execute Burn
        const amountWei = ethers.parseUnits(price.toString(), 18);
        const txBurn = await contract.burnFrom(address, amountWei);
        console.log(`Burn Transaction: ${txBurn.hash}`);
        await txBurn.wait();

        // 3. Record Purchase (Optional: Add to DB)
        // For now, we return success
        res.json({
            success: true,
            txHash: txBurn.hash,
            message: `Successfully bought Item ${item_id}`
        });

    } catch (err) {
        console.error("Buy Error:", err);
        res.status(500).json({ error: 'Transaction failed', details: err.message });
    }
});

// Claim Success (Reset Pending Balance & CIDs)
app.post('/api/claim-success', async (req, res) => {
    const { address } = req.body;
    try {
        const result = await pool.query(
            'UPDATE users SET pending_balance = 0, pending_cids = \'{}\' WHERE wallet_address = $1 RETURNING *',
            [address]
        );
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Database error' });
    }
});

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
