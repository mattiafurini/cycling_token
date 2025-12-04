import { useState, useEffect } from 'react';
import { ethers } from 'ethers';
import { Wallet, Bike, ArrowRight, Timer, Trophy } from 'lucide-react';
import { motion } from 'framer-motion';
import { contractAddress, contractABI, API_URL } from './config';

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
  icons: ['https://avatars.mywebsite.com/']
}

// 3. Create the AppKit instance
// 3. Create the AppKit instance (Wagmi Adapter)
const wagmiAdapter = new WagmiAdapter({
  projectId,
  networks: [polygonAmoy]
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
    setLoading(true);
    try {
      let provider;
      if (walletProvider) {
        provider = new ethers.BrowserProvider(walletProvider, 'any')
      } else {
        // Fallback or return if no provider
        return;
      }

      const signer = await provider.getSigner();
      const contract = new ethers.Contract(contractAddress, contractABI, signer);

      // Check Owner
      const contractOwner = await contract.owner();
      setOwner(contractOwner);

      // Check Balance
      const bal = await contract.balanceOf(userAddress);
      setBalance(ethers.formatUnits(bal, 18));

      // Fetch user data from backend
      try {
        const response = await fetch(`${API_URL}/api/user/${userAddress}`);
        const userData = await response.json();
        setPendingReward(parseFloat(userData.pending_balance));
        setIsPro(userData.is_pro);
      } catch (err) {
        console.error("Error fetching user data:", err);
      }

      setLoading(false);
    } catch (error) {
      console.error("Error connecting wallet:", error);
      setLoading(false);
    }
  };

  const simulateRide = async () => {
    if (!account) {
      alert("Please connect your wallet first!");
      return;
    }
    setLoading(true);

    try {
      // Simulate 10km ride
      const response = await fetch(`${API_URL}/api/ride`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: account, km: 10 })
      });
      const userData = await response.json();
      setPendingReward(parseFloat(userData.pending_balance));
    } catch (err) {
      console.error("Error simulating ride:", err);
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
    if (!walletProvider) {
      alert("Provider not found. Please connect wallet.");
      return;
    }

    if (pendingReward < 50) {
      alert("You need at least 50 CYCL to claim rewards!");
      return;
    }

    try {
      setLoading(true);
      const provider = new ethers.BrowserProvider(walletProvider, 'any');
      const signer = await provider.getSigner();
      const contract = new ethers.Contract(contractAddress, contractABI, signer);

      // 1. Fetch pending CIDs from backend
      const userResponse = await fetch(`${API_URL}/api/user/${account}`);
      const userData = await userResponse.json();
      const pendingCids = userData.pending_cids || [];

      // For this prototype, we mint a single tokenURI containing all CIDs or just the last one.
      // A better approach would be to batch mint or create a composite IPFS object.
      // Let's create a simple JSON on the fly or just use the last CID as proof.
      const tokenURI = pendingCids.length > 0 ? pendingCids[pendingCids.length - 1] : "ipfs://QmEmpty";

      // 2. Mint with Token URI
      const tx = await contract.mint(account, ethers.parseUnits(pendingReward.toString(), 18), tokenURI);
      await tx.wait();

      // 3. Notify backend of success to reset balance
      await fetch(`${API_URL}/api/claim-success`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: account })
      });

      // Refresh balance
      const bal = await contract.balanceOf(account);
      setBalance(ethers.formatUnits(bal, 18));
      setPendingReward(0);
      setLoading(false);
      alert("Reward claimed successfully! Data saved on IPFS.");
    } catch (error) {
      console.error("Error claiming reward:", error);
      setLoading(false);
      alert("Error claiming reward: " + (error.reason || error.message));
    }
  };

  return (
    <div className="app-container">
      <nav className="container nav">
        <div className="logo">
          <Bike size={32} color="var(--accent-primary)" />
          <span>CyclingToken</span>
        </div>
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={() => open()}
          className="flex items-center space-x-2 bg-gradient-to-r from-neon-green to-emerald-500 text-black px-6 py-3 rounded-full font-bold shadow-lg shadow-neon-green/20 hover:shadow-neon-green/40 transition-all"
        >
          <Wallet className="w-5 h-5" />
          <span>{isConnected ? `${address.substring(0, 6)}...${address.substring(38)}` : "Connect Wallet"}</span>
        </motion.button>
      </nav>

      <main className="container hero">
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
            <button className="btn-secondary">
              Learn More
            </button>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, delay: 0.2 }}
          className="card-container"
        >
          <div className="card-glow"></div>
          <div className="card">
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
                disabled={loading || pendingReward < 50}
                style={{ width: '100%', justifyContent: 'center', opacity: pendingReward < 50 ? 0.5 : 1 }}
              >
                {loading ? 'Processing...' : (pendingReward < 50 ? `Min 50 CYCL to Claim` : 'Claim Reward')}
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
