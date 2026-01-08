import { useState, useEffect } from 'react';
import { ethers } from 'ethers';
import { Wallet, Bike, ArrowRight, Timer, Trophy } from 'lucide-react';
import { motion } from 'framer-motion';
import { contractAddress, contractABI, API_URL } from './config';
import { RideService } from './services/RideService';
import Shop from './components/Shop';


// AppKit Imports
import { createAppKit } from '@reown/appkit/react'
import { WagmiAdapter } from '@reown/appkit-adapter-wagmi'
import { useAppKit, useAppKitAccount, useAppKitProvider } from "@reown/appkit/react";
import { polygonAmoy } from '@reown/appkit/networks'
import { WagmiProvider } from 'wagmi'

// 1. Get projectId from https://cloud.reown.com
const projectId = '806973e58b8cc7a0ea58361bb80e8028'

// 2. Create a metadata object
const metadata = {
  name: 'CyclingToken',
  description: 'Bike-to-Earn App',
  url: 'https://cyclingtoken.app', // origin must match your domain & subdomain
  icons: ['https://avatars.mywebsite.com/'],
  redirect: {
    native: 'cyclingtoken://app',
    universal: 'https://cyclingtoken.app'
  }
}

// 3. Create the AppKit instance
// 3. Create the AppKit instance (Wagmi Adapter)
const wagmiAdapter = new WagmiAdapter({
  projectId,
  networks: [polygonAmoy],
  metadata
})

createAppKit({
  adapters: [wagmiAdapter],
  metadata,
  networks: [polygonAmoy],
  projectId,
  features: {
    analytics: true
  },
  featuredWalletIds: [
    'c57ca95b47569778a828d19178114f4db188b89b763c899ba0be274e97267d96', // MetaMask
    '1ae92b26df02f0abca6304df07debccd18262fdf5fe82daa81593582dac9a369'  // Rainbow
  ]
})

function MainApp() {
  // AppKit Hooks
  const { address, isConnected } = useAppKitAccount()
  const { walletProvider } = useAppKitProvider('eip155')
  const { open } = useAppKit()

  const [account, setAccount] = useState(null);
  const [owner, setOwner] = useState(null);
  const [balance, setBalance] = useState('0');
  const [pendingReward, setPendingReward] = useState(0);
  const [isPro, setIsPro] = useState(false);
  const [loading, setLoading] = useState(false);
  const [view, setView] = useState('dashboard');

  // Sync AppKit address with local state
  useEffect(() => {
    if (isConnected && address) {
      setAccount(address);
      connectWallet(address); // Reuse existing logic but pass address
    } else {
      setAccount(null);
    }
  }, [isConnected, address]);

  const connectWallet = async (userAddress) => {
    try {
      let provider;
      if (walletProvider) {
        provider = new ethers.BrowserProvider(walletProvider, 'any')
      } else {
        // Fallback or return if no provider
        console.warn("No wallet provider found, skipping connection");
        return;
      }

      setLoading(true);

      const signer = await provider.getSigner();
      const contract = new ethers.Contract(contractAddress, contractABI, signer);

      // Check Owner
      try {
        const contractOwner = await contract.owner();
        setOwner(contractOwner);
      } catch (e) { console.warn("Owner check failed", e); }

      // Check Balance
      const bal = await contract.balanceOf(userAddress);
      setBalance(ethers.formatUnits(bal, 18));

      // Fetch user data from backend
      try {
        const response = await fetch(`${API_URL}/api/user/${userAddress}`);
        const userData = await response.json();

        let totalPending = parseFloat(userData.pending_balance);
        const userIsPro = userData.is_pro;
        setIsPro(userIsPro);

        // Calculate offline rewards
        const localRides = await RideService.getLocalRides();
        // Filter rides for this address only
        const myLocalRides = localRides.filter(r => r.address === userAddress);

        let localReward = 0;
        for (const ride of myLocalRides) {
          // Use the same logic as simulateRide: 1.2 for Pro, 1.0 for others
          // Ideally we should have stored the reward in the ride object, but recalculating is fine for now
          const multiplier = userIsPro ? 1.2 : 1.0;
          localReward += (ride.km * multiplier);
        }

        if (localReward > 0) {
          console.log(`Adding ${localReward} CYCL from offline rides to display.`);
          totalPending += localReward;
        }

        setPendingReward(totalPending);
      } catch (err) {
        console.error("Error fetching user data:", err);
        // If server is offline, we still want to show local rewards!
        const localRides = await RideService.getLocalRides();
        const myLocalRides = localRides.filter(r => r.address === userAddress);
        // We might not know if user is PRO if offline, assume standard rate or check local storage if we cached it (not implemented yet)
        // For safety, assume standard rate 1.0 if offline
        let localReward = 0;
        for (const ride of myLocalRides) {
          localReward += ride.km;
        }
        if (localReward > 0) {
          setPendingReward(localReward);
          alert("Server offline. Showing local rewards only.");
        }
      }

      setLoading(false);
    } catch (error) {
      console.error("Error connecting wallet:", error);
      setLoading(false);
    }
  };

  // Sync pending rides on mount
  useEffect(() => {
    const syncRides = async () => {
      const localRides = await RideService.getLocalRides();
      if (localRides.length > 0) {
        console.log(`Found ${localRides.length} pending rides. Syncing...`);
      }

      for (const ride of localRides) {
        try {
          // Notify backend using the standard endpoint
          await fetch(`${API_URL}/api/ride`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              address: ride.address,
              km: ride.km
            })
          });

          // Remove from local storage on success
          await RideService.removeLocalRide(ride.timestamp);
          console.log(`Synced ride from ${ride.timestamp}`);
        } catch (err) {
          console.error(`Failed to sync ride ${ride.timestamp}:`, err);
          // If it's a network error, we just keep it for next time
        }
      }
    };

    // Run sync only if connected
    if (isConnected) {
      syncRides();
    }
  }, [isConnected]);

  const simulateRide = async () => {
    if (!account) {
      alert("Please connect your wallet first!");
      return;
    }
    setLoading(true);

    // Generate Random Ride Data
    const { km, avg_speed, gps_data } = RideService.simulateRideData();
    console.log(`Simulating Ride: ${km}km at ${avg_speed}km/h`);

    const rideData = {
      address: account,
      km: km,
      avg_speed: avg_speed,
      gps_data: gps_data,
      timestamp: Date.now(),
      device: 'android_sim'
    };

    // 1. Calculate Reward Locally (Offline Feedback)
    // Base rate: 1 CYCL/km. Pro rate: 1.2 CYCL/km.
    const multiplier = isPro ? 1.2 : 1.0;
    const estimatedReward = km * multiplier;

    try {
      // 2. Save locally immediately (Source of Truth for Offline)
      await RideService.saveRideLocal(rideData);

      // 3. Update UI Immediately
      setPendingReward(prev => prev + estimatedReward);

      // 4. Try Pinata Upload (OFF - Server handles this now)
      let cid = null;
      let pinataSuccess = false;
      // try {
      //   cid = await RideService.uploadToPinata(rideData);
      //   pinataSuccess = true;
      // } catch (uploadError) {
      //   console.warn("Pinata upload failed (Offline):", uploadError);
      // }

      // 5. Backend Sync (Server will upload to Pinata)
      try {
        const response = await fetch(`${API_URL}/api/ride`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ ...rideData, cid: null }) // We don't send CID, server generates it
        });

        if (response.ok) {
          const resData = await response.json();
          // Update with server truth if available
          setPendingReward(parseFloat(resData.user.pending_balance));

          // Remove local copy since server has it
          await RideService.removeLocalRide(rideData.timestamp);
          alert("Ride saved and synced to Server!");
        } else {
          throw new Error("Server returned " + response.status);
        }
      } catch (backendError) {
        console.warn("Backend sync failed (Offline):", backendError);
        // Do NOT alert user with error. Show success message for offline save.
        if (pinataSuccess) {
          alert("Ride saved to IPFS! (Server offline, will sync later)");
        } else {
          alert("Ride saved locally! (Offline mode)");
        }
      }

    } catch (err) {
      console.error("Critical error saving ride:", err);
      alert("Error recording ride: " + err.message);
    }

    setLoading(false);
  };

  const upgradeToPro = async () => {
    if (!account) return;
    if (!walletProvider) {
      alert("Provider not found. Please connect wallet.");
      return;
    }

    const currentBalance = parseFloat(balance);
    if (currentBalance < 100) {
      if (currentBalance + pendingReward >= 100) {
        alert(`You need 100 CYCL. You have ${currentBalance} in wallet and ${pendingReward} pending. Claim your rewards first!`);
      } else {
        alert("You need 100 CYCL to upgrade to PRO!");
      }
      return;
    }

    try {
      setLoading(true);
      const provider = new ethers.BrowserProvider(walletProvider, 'any');
      const signer = await provider.getSigner();
      const contract = new ethers.Contract(contractAddress, contractABI, signer);

      // Burn 100 tokens
      const tx = await contract.burn(ethers.parseUnits("100", 18));
      await tx.wait();

      // Notify backend
      const response = await fetch(`${API_URL}/api/upgrade-pro`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: account })
      });

      const userData = await response.json();
      setIsPro(userData.is_pro);

      // Refresh balance
      const bal = await contract.balanceOf(account);
      setBalance(ethers.formatUnits(bal, 18));

      setLoading(false);
      alert("Upgraded to PRO successfully!");
    } catch (error) {
      console.error("Error upgrading:", error);
      setLoading(false);
      alert("Error upgrading: " + (error.reason || error.message));
    }
  };

  const claimReward = async () => {
    if (!account) return;

    // Server-Side Minting: We don't need a signer, just the address.

    if (pendingReward < 20) {
      alert("You need at least 20 CYCL to claim rewards!");
      return;
    }

    try {
      setLoading(true);

      // Call the backend via RideService
      // The backend handles data fetching, minting, and DB updates.
      const result = await RideService.claimRewards(account);

      console.log("Claim Success:", result);

      // Refresh balance
      if (walletProvider) {
        try {
          const provider = new ethers.BrowserProvider(walletProvider, 'any');
          const contract = new ethers.Contract(contractAddress, contractABI, provider); // Read-only is fine
          const bal = await contract.balanceOf(account);
          setBalance(ethers.formatUnits(bal, 18));
        } catch (e) {
          console.warn("Could not refresh balance:", e);
        }
      }

      setPendingReward(0);
      setLoading(false);
      alert(`Reward claimed successfully! Tx: ${result.txHash}`);
    } catch (error) {
      console.error("Error claiming reward:", error);
      setLoading(false);
      // Nice error message handling
      const msg = error.response?.data?.error || error.message;
      alert("Error claiming reward: " + msg);
    }
  };

  return (
    <div className="app-container">
      <nav className="container nav">
        {/* ... logo ... */}
        <div className="logo" onClick={() => setView('dashboard')} style={{ cursor: 'pointer' }}>
          <Bike size={32} color="var(--accent-primary)" />
          <span>CyclingToken</span>
        </div>
        {/* ... */}
      </nav>

      <main className="container hero">
        {view === 'dashboard' ? (
          <>
            <motion.div
              initial={{ opacity: 0, x: -50 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.8 }}
            >
              <h1 className="hero-title">
                Ride Your Bike. <br />
                <span className="text-accent">Earn Crypto.</span>
              </h1>
              <p className="hero-text">
                Join the revolution of sustainable transport. Track your rides, reduce your carbon footprint, and get rewarded with Cycling Tokens.
              </p>
              <div className="btn-group">
                <button className="btn-primary" onClick={simulateRide} disabled={loading}>
                  Start Riding <ArrowRight size={20} />
                </button>
                <button className="btn-secondary" onClick={() => setView('shop')}>
                  Go to Shop
                </button>
              </div>
            </motion.div>

            {/* ... Card Container ... */}
            <motion.div
              // ... props ...
              className="card-container"
            >
              {/* ... Existing Card Content ... */}
              <div className="card-glow"></div>
              <div className="card">
                {/* ... header ... */}
                <div className="card-header">
                  <div>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Pending Rewards</p>
                    <h3 className="balance-amount">{pendingReward} CYCL</h3>
                  </div>
                  <button
                    className="icon-box"
                    onClick={!isPro ? upgradeToPro : null}
                    disabled={loading}
                    style={{
                      cursor: isPro ? 'default' : 'pointer',
                      background: isPro ? 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)' : 'rgba(255, 255, 255, 0.1)',
                      color: isPro ? '#000' : '#fff',
                      border: isPro ? 'none' : '1px solid rgba(255, 255, 255, 0.1)',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      padding: '0.5rem',
                      minWidth: '80px',
                      height: 'auto',
                      gap: '4px'
                    }}
                  >
                    <Trophy size={24} />
                    <span style={{ fontSize: '0.75rem', fontWeight: 'bold' }}>
                      {isPro ? 'PRO' : 'GET PRO'}
                    </span>
                  </button>
                </div>

                <div className="btn-group" style={{ marginTop: '1.5rem' }}>
                  <button
                    className="btn-primary"
                    onClick={claimReward}
                    disabled={loading || pendingReward < 20}
                    style={{ width: '100%', justifyContent: 'center', opacity: pendingReward < 20 ? 0.5 : 1 }}
                  >
                    {loading ? 'Processing...' : (pendingReward < 20 ? `Min 20 CYCL to Claim` : 'Claim Reward')}
                  </button>
                </div>

                <div className="card-divider"></div>

                <div>
                  <div className="status-row">
                    <div className="status-label">
                      <div className="dot dot-green"></div>
                      <span>Status</span>
                    </div>
                    <span className="status-value-active">Active</span>
                  </div>
                  <div className="status-row">
                    <div className="status-label">
                      <div className="dot dot-cyan"></div>
                      <span>Network</span>
                    </div>
                    <span className="status-value-network">Polygon Amoy</span>
                  </div>
                </div>
              </div>
            </motion.div>
          </>
        ) : (
          <Shop onBack={() => setView('dashboard')} />
        )}
      </main>
    </div >
  );
}

export default function App() {
  return (
    <WagmiProvider config={wagmiAdapter.wagmiConfig}>
      <MainApp />
    </WagmiProvider>
  );
}
