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

        setLoading(false);
      } catch (error) {
        console.error("Error connecting wallet:", error);
        setLoading(false);
      }
    } else {
      alert("Please install a wallet like Rabby or Metamask!");
    }
  };

  const simulateRide = () => {
    setLoading(true);
    setTimeout(() => {
      setPendingReward(prev => prev + 10);
      setLoading(false);
    }, 1000);
  };

  const claimReward = async () => {
    if (!account) return;

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
              <div className="icon-box">
                <Trophy size={32} />
              </div>
            </div>

            <div className="btn-group" style={{ marginTop: '1.5rem' }}>
              {pendingReward > 0 && (
                <button
                  onClick={claimReward}
                  className="btn-primary"
                  disabled={loading}
                  style={{ flex: 1 }}
                >
                  <Wallet size={18} />
                  <span>Claim Reward</span>
                </button>
              )}
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
    </div>
  );
}

export default App;
