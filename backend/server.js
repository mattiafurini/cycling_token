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

// Record Ride (Simulated)
app.post('/api/ride', async (req, res) => {
    const { address, km } = req.body;

    try {
        // Check if user is PRO
        const userResult = await pool.query('SELECT is_pro, pro_expiry FROM users WHERE wallet_address = $1', [address]);
        const user = userResult.rows[0];

        let multiplier = 1.0;
        if (user && user.is_pro) {
            // Check expiry (optional for prototype, but good practice)
            // For now, assume permanent or check date
            multiplier = 1.2; // 20% bonus (12 tokens for 10km)
        }

        const reward = km * multiplier;

        const result = await pool.query(
            'UPDATE users SET pending_balance = pending_balance + $1, total_km = total_km + $2 WHERE wallet_address = $3 RETURNING *',
            [reward, km, address]
        );
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Database error' });
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

// Claim Success (Reset Pending Balance)
app.post('/api/claim-success', async (req, res) => {
    const { address } = req.body;
    try {
        const result = await pool.query(
            'UPDATE users SET pending_balance = 0 WHERE wallet_address = $1 RETURNING *',
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
