const express = require('express');
const cors = require('cors');
require('dotenv').config();

const { Pool } = require('pg');

const app = express();
const PORT = process.env.PORT || 3000;

// Database Connection
const pool = new Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    port: process.env.DB_PORT,
});

pool.connect((err, client, release) => {
    if (err) {
        return console.error('Error acquiring client', err.stack);
    }
    console.log('Connected to PostgreSQL database');
    release();
});

app.use(cors());
app.use(express.json());

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
    const { address, km } = req.body;

    try {
        // 1. Check Pro Status
        const userResult = await pool.query('SELECT is_pro, pro_expiry FROM users WHERE wallet_address = $1', [address]);
        const user = userResult.rows[0];

        let multiplier = 1.0;
        if (user && user.is_pro) {
            multiplier = 1.2;
        }

        const reward = km * multiplier;

        // 2. Prepare Metadata for IPFS
        const rideData = {
            user: address,
            km: km,
            reward: reward,
            timestamp: Date.now(),
            type: "Cycling Session",
            app: "CyclingToken v2"
        };

        // 3. Upload to IPFS
        const cid = await uploadToPinata(rideData);
        console.log(`Ride saved to IPFS: ${cid}`);

        // 4. Update Database (Store Pending Balance + CID)
        // We append the new CID to the array of pending_cids
        const result = await pool.query(
            `UPDATE users 
             SET pending_balance = pending_balance + $1, 
                 total_km = total_km + $2,
                 pending_cids = array_append(pending_cids, $3)
             WHERE wallet_address = $4 
             RETURNING *`,
            [reward, km, cid, address]
        );

        res.json({ ...result.rows[0], latest_cid: cid });
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
