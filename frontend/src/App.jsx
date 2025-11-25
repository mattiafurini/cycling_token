import { useState } from 'react';
import { ethers } from 'ethers';
import { Wallet, Bike, ArrowRight } from 'lucide-react';
import { motion } from 'framer-motion';

function App() {
  const [account, setAccount] = useState(null);
  const [balance, setBalance] = useState('0');
  const [loading, setLoading] = useState(false);

  const connectWallet = async () => {
    if (window.ethereum) {
      try {
        setLoading(true);
        const provider = new ethers.BrowserProvider(window.ethereum);
        const signer = await provider.getSigner();
        const address = await signer.getAddress();
        setAccount(address);

        // Here we would fetch the balance from the contract
        // const contract = new ethers.Contract(address, abi, signer);
        // const bal = await contract.balanceOf(address);
        // setBalance(ethers.formatUnits(bal, 18));

        setLoading(false);
      } catch (error) {
        console.error("Error connecting wallet:", error);
        setLoading(false);
      }
    } else {
      alert("Please install a wallet like Rabby or Metamask!");
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
            <button className="btn-primary">
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
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Current Balance</p>
                <h3 className="balance-amount">{balance} CYCL</h3>
              </div>
              <div className="icon-box">
                <Bike size={32} />
              </div>
            </div>

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
