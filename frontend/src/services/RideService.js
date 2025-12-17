import { Preferences } from '@capacitor/preferences';
import { PINATA_JWT, API_URL } from '../config';

const RIDE_STORAGE_KEY = 'pending_rides';

export const RideService = {
    /**
     * Save a ride locally
     * @param {Object} rideData 
     */
    async saveRideLocal(rideData) {
        try {
            const { value } = await Preferences.get({ key: RIDE_STORAGE_KEY });
            const rides = value ? JSON.parse(value) : [];
            rides.push(rideData);
            await Preferences.set({
                key: RIDE_STORAGE_KEY,
                value: JSON.stringify(rides),
            });
            console.log('Ride saved locally:', rideData);
        } catch (error) {
            console.error('Error saving ride locally:', error);
        }
    },

    /**
     * Get all local rides
     */
    async getLocalRides() {
        try {
            const { value } = await Preferences.get({ key: RIDE_STORAGE_KEY });
            return value ? JSON.parse(value) : [];
        } catch (error) {
            console.error('Error getting local rides:', error);
            return [];
        }
    },

    /**
     * Remove a ride from local storage by timestamp (or unique ID)
     * @param {number} timestamp 
     */
    async removeLocalRide(timestamp) {
        try {
            const rides = await this.getLocalRides();
            const updatedRides = rides.filter(r => r.timestamp !== timestamp);
            await Preferences.set({
                key: RIDE_STORAGE_KEY,
                value: JSON.stringify(updatedRides),
            });
        } catch (error) {
            console.error('Error removing local ride:', error);
        }
    },

    /**
     * Upload JSON data to Pinata
     * @param {Object} data 
     * @returns {Promise<string>} IPFS CID
     */
    async uploadToPinata(data) {
        try {
            const body = JSON.stringify({
                pinataOptions: {
                    cidVersion: 1
                },
                pinataMetadata: {
                    name: `ride_${data.timestamp}.json`,
                },
                pinataContent: data
            });

            const response = await fetch('https://api.pinata.cloud/pinning/pinJSONToIPFS', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${PINATA_JWT}`
                },
                body: body
            });

            if (!response.ok) {
                throw new Error(`Pinata upload failed: ${response.statusText}`);
            }

            const result = await response.json();
            return result.IpfsHash;
        } catch (error) {
            console.error('Error uploading to Pinata:', error);
            throw error;
        }
    },

    /**
     * Claim Rewards (Server-Side)
     * @param {string} userAddress 
     */
    async claimRewards(userAddress) {
        try {
            console.log("Requesting server-side claim for:", userAddress);
            // We need to import API_URL, let's assume it's available or import it
            // Wait, this file imports PINATA_JWT from ../config, so we should import API_URL too
            // I'll fix imports in another step if needed, but for now I'll use the variable assuming it was imported
            const response = await fetch(`${API_URL}/api/claim`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ address: userAddress })
            });

            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error || "Claim failed");
            }

            return await response.json();
        } catch (error) {
            console.error("Claim Error:", error);
            throw error;
        }
    }
};
