import { useState, useEffect } from 'react';
import { ethers } from 'ethers';
import { Wallet, Bike, ArrowRight, Timer, Trophy } from 'lucide-react';
import { motion } from 'framer-motion';
import { contractAddress, contractABI } from './config';

function App() {
  const [account, setAccount] = useState(null);
  const [owner, setOwner] = useState(null);
  const [balance, setBalance] = useState('0');
  const [pendingReward, setPendingReward] = useState(0);
  const [isPro, setIsPro] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const checkConnection = async () => {
      if (window.ethereum) {
        try {
          const accounts = await window.ethereum.request({ method: 'eth_accounts' });
          if (accounts.length > 0) {
            connectWallet();
          }
        } catch (error) {
          console.error("Error checking connection:", error);
        }
      }
    };
    checkConnection();
  }, []);

  const connectWallet = async () => {
    if (window.ethereum) {
      try {
        setLoading(true);
        const provider = new ethers.BrowserProvider(window.ethereum);

        const network = await provider.getNetwork();
        if (network.chainId !== 80002n) {
          try {
            await window.ethereum.request({
              method: 'wallet_switchEthereumChain',
              params: [{ chainId: '0x13882' }],
            });
          } catch (switchError) {
            // This error code indicates that the chain has not been added to MetaMask.
            if (switchError.code === 4902) {
              await window.ethereum.request({
                method: 'wallet_addEthereumChain',
                params: [
                  {
                    chainId: '0x13882',
                    chainName: 'Polygon Amoy',
                    rpcUrls: ['https://rpc-amoy.polygon.technology/'],
                    nativeCurrency: {
                      name: 'POLYGON',
                      symbol: 'POL',
                      decimals: 18
                    },
                    blockExplorerUrls: ['https://amoy.polygonscan.com/']
                  }
                ],
              });
            } else {
              throw switchError;
            }
          }
        }

        const signer = await provider.getSigner();
        const address = await signer.getAddress();
        setAccount(address);

        // Here we would fetch the balance from the contract
        const contract = new ethers.Contract(contractAddress, contractABI, signer);
        const bal = await contract.balanceOf(address);
        setBalance(ethers.formatUnits(bal, 18));

        // Fetch owner
        const contractOwner = await contract.owner();
        setOwner(contractOwner);

        // Fetch user data from backend
        try {
          const response = await fetch(`http://localhost:3000/api/user/${address}`);
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
    } else {
      alert("Please install a wallet like Rabby or Metamask!");
    }
  };

  const simulateRide = async () => {
    if (!account) return;
    setLoading(true);

    try {
      // Simulate 10km ride
      const response = await fetch('http://localhost:3000/api/ride', {
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
      const provider = new ethers.BrowserProvider(window.ethereum);
      const signer = await provider.getSigner();
      const contract = new ethers.Contract(contractAddress, contractABI, signer);

      // Burn 100 tokens
      const tx = await contract.burn(ethers.parseUnits("100", 18));
      await tx.wait();

      // Notify backend
      const response = await fetch('http://localhost:3000/api/upgrade-pro', {
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

    if (pendingReward < 50) {
      alert("You need at least 50 CYCL to claim rewards!");
      return;
    }

    if (owner && account.toLowerCase() !== owner.toLowerCase()) {
      alert("Only the contract owner (Server) can process this transaction!");
      return;
    }

    try {
      setLoading(true);
      const provider = new ethers.BrowserProvider(window.ethereum);
      const signer = await provider.getSigner();
      const contract = new ethers.Contract(contractAddress, contractABI, signer);

      const tx = await contract.mint(account, ethers.parseUnits(pendingReward.toString(), 18));
      await tx.wait();

      // Notify backend of success to reset balance
      await fetch('http://localhost:3000/api/claim-success', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: account })
      });

      // Refresh balance
      const bal = await contract.balanceOf(account);
      setBalance(ethers.formatUnits(bal, 18));
      setPendingReward(0);
      setLoading(false);
      alert("Reward claimed successfully!");
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
        <button onClick={connectWallet} className="btn-connect">
          <Wallet size={18} color="var(--accent-primary)" />
          <span>{account ? `${account.substring(0, 6)}...${account.substring(38)}` : "Connect Wallet"}</span>
        </button>
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

export default App;
