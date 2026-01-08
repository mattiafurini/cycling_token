import { useState } from 'react';
import { ethers } from 'ethers';
import { ShoppingBag, Loader } from 'lucide-react';
import { contractAddress, contractABI, API_URL } from '../config';
import { useAppKitAccount, useAppKitProvider } from "@reown/appkit/react";
import { motion } from 'framer-motion';

const ITEMS = [
    { id: '1', name: 'Cycling Cap', price: 50, image: '🧢' },
    { id: '2', name: 'Water Bottle', price: 30, image: '💧' },
    { id: '3', name: 'Pro Jersey', price: 200, image: '👕' }
];

export default function Shop({ onBack }) {
    const { address, isConnected } = useAppKitAccount();
    const { walletProvider } = useAppKitProvider('eip155');
    const [loading, setLoading] = useState(false);

    const handleBuy = async (item) => {
        if (!isConnected || !walletProvider) return alert("Connect wallet first");

        setLoading(true);
        try {
            const provider = new ethers.BrowserProvider(walletProvider, 'any');
            const signer = await provider.getSigner();
            const contract = new ethers.Contract(contractAddress, contractABI, signer);

            // 1. Get Permit Params
            const nonce = await contract.nonces(address);
            const name = await contract.name();
            const chainId = (await provider.getNetwork()).chainId;

            // Deadline = 1 hour from now
            const deadline = Math.floor(Date.now() / 1000) + 3600;
            const value = ethers.parseUnits(item.price.toString(), 18);

            // 2. Define Typed Data
            const domain = {
                name: name,
                version: '1',
                chainId: chainId,
                verifyingContract: contractAddress
            };

            const types = {
                Permit: [
                    { name: 'owner', type: 'address' },
                    { name: 'spender', type: 'address' },
                    { name: 'value', type: 'uint256' },
                    { name: 'nonce', type: 'uint256' },
                    { name: 'deadline', type: 'uint256' }
                ]
            };

            // Spender is the backend wallet. 
            // We assume the backend wallet address is known or derived.
            // Wait, we need the backend wallet address to approve IT!
            // In server.js we saw: `wallet = new ethers.Wallet(PRIVATE_KEY, provider);`
            // We need to expose the server's address to the frontend or fetch it.
            // Let's assume for now we fetch it or hardcode it. 
            // Actually, querying /api/health gives `blockchain: wallet.address`.

            const healthRes = await fetch(`${API_URL}/api/health`);
            const healthData = await healthRes.json();
            const spenderAddress = healthData.blockchain;

            if (!spenderAddress || spenderAddress === 'not connected') {
                throw new Error("Server wallet not connected");
            }

            const message = {
                owner: address,
                spender: spenderAddress,
                value: value,
                nonce: nonce,
                deadline: deadline
            };

            // 3. Sign Typed Data
            // Note: ethers v6 Use signTypedData (no underscore)
            const signature = await signer.signTypedData(domain, types, message);
            const { v, r, s } = ethers.Signature.from(signature);

            // 4. Send to Backend
            const response = await fetch(`${API_URL}/api/buy`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    address,
                    item_id: item.id,
                    price: item.price,
                    permit: { deadline, v, r, s }
                })
            });

            const resData = await response.json();
            if (response.ok) {
                alert(`Purchased ${item.name}! Tx: ${resData.txHash}`);
            } else {
                throw new Error(resData.error || "Purchase failed");
            }

        } catch (err) {
            console.error(err);
            alert("Error: " + err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container" style={{ padding: '2rem 0' }}>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <button onClick={onBack} className="btn-secondary" style={{ marginBottom: '1rem' }}>
                    &larr; Back to Dashboard
                </button>
                <h2>Shop</h2>
                <div className="card-container" style={{ display: 'grid', gap: '1rem', marginTop: '1rem' }}>
                    {ITEMS.map(item => (
                        <div key={item.id} className="card" style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                <span style={{ fontSize: '2rem' }}>{item.image}</span>
                                <div>
                                    <h3>{item.name}</h3>
                                    <p className="text-secondary">{item.price} CYCL</p>
                                </div>
                            </div>
                            <button
                                className="btn-primary"
                                onClick={() => handleBuy(item)}
                                disabled={loading}
                            >
                                {loading ? <Loader className="spin" size={16} /> : "Buy"}
                            </button>
                        </div>
                    ))}
                </div>
            </motion.div>
        </div>
    );
}
