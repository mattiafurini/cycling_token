# CyclingToken (CYCL)

CyclingToken è un token ERC-20 deployato sulla testnet Ethereum Sepolia.

## Dettagli Token

- **Nome**: CyclingToken
- **Simbolo**: CYCL
- **Decimali**: 18
- **Supply iniziale**: 1,000,000 CYCL
- **Network**: Ethereum Sepolia (testnet)
- **Contract address**: `[INSERISCI_CONTRACT_ADDRESS]`
- **Etherscan**: `https://sepolia.etherscan.io/address/[INSERISCI_CONTRACT_ADDRESS]`

---

## Requisiti

- Node.js >= 18
- npm >= 9
- Wallet EVM (Rabby / MetaMask) con Sepolia ETH di test

---

## Installazione

```bash
git clone https://github.com/[TUO_USERNAME]/cycling_token.git
cd cycling_token
npm install
```

---

## Configurazione `.env`

Crea un file `.env` nella root del progetto:

```bash
nano .env
```

Contenuto minimo:

```env
SEPOLIA_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/[ALCHEMY_API_KEY]
PRIVATE_KEY=[PRIVATE_KEY_SENZA_0x]
ETHERSCAN_API_KEY=[ETHERSCAN_API_KEY]
```

**Note:**
- `PRIVATE_KEY`: chiave privata dell'account che fa il deploy, senza prefisso `0x`
- Non committare mai `.env` (è già in `.gitignore`)

---

## Compilazione

```bash
npx hardhat compile
```

---

## Deploy su Sepolia

```bash
npx hardhat run scripts/deploy.js --network sepolia
```

Output atteso:

```
Deploying with account: 0x...
CyclingToken deployed to: 0x...[CONTRACT_ADDRESS]
```

Copia e incolla `CONTRACT_ADDRESS` nella sezione "Dettagli Token" qui sopra.

---

## Verifica del contratto su Etherscan

Assicurati che `ETHERSCAN_API_KEY` sia impostata nel `.env`, poi:

```bash
npx hardhat verify --network sepolia [CONTRACT_ADDRESS] 1000000
```

Dove `1000000` è l'`initialSupply` passato al costruttore.

---

## Struttura del progetto

```
cycling_token/
├── contracts/
│   └── CyclingToken.sol
├── scripts/
│   ├── deploy.js
│   └── transfer.js        # opzionale
├── .env                   # NON va su Git
├── .gitignore
├── hardhat.config.js
├── package.json
└── README.md
```

---

## Trasferire token

### Metodo 1: Tramite Wallet

1. Importa il token in Rabby/MetaMask usando `CONTRACT_ADDRESS`
2. Vai su Sepolia network
3. Clicca "Send" sul token CYCL
4. Inserisci indirizzo destinatario e quantità
5. Conferma la transazione

### Metodo 2: Tramite script

Crea `scripts/transfer.js`:

```javascript
const hre = require("hardhat");

async function main() {
  const tokenAddress = "[CONTRACT_ADDRESS]";
  const recipientAddress = "[INDIRIZZO_DESTINATARIO]";
  const amount = hre.ethers.utils.parseUnits("1000", 18); // 1000 CYCL

  const CyclingToken = await hre.ethers.getContractAt("CyclingToken", tokenAddress);
  const tx = await CyclingToken.transfer(recipientAddress, amount);

  console.log("Transfer TX:", tx.hash);
  await tx.wait();
  console.log("Transfer completato!");
}

main().catch(console.error);
```

Esegui con:

```bash
npx hardhat run scripts/transfer.js --network sepolia
```

---

## Per utenti/collaboratori

Se vuoi usare CyclingToken senza fare il deploy:

1. Installa Rabby Wallet o MetaMask
2. Aggiungi Ethereum Sepolia network
3. Importa il token con questi dettagli:
   - **Contract address**: `[CONTRACT_ADDRESS]`
   - **Symbol**: CYCL
   - **Decimals**: 18
4. Richiedi token al creatore del progetto

---

## Tecnologie utilizzate

- [Hardhat](https://hardhat.org/) - Framework di sviluppo Ethereum
- [OpenZeppelin Contracts](https://openzeppelin.com/contracts/) - Contratti sicuri ERC-20
- [ethers.js](https://docs.ethers.org/) - Libreria Ethereum
- [Alchemy](https://www.alchemy.com) - Provider RPC

---

## Link utili

- [Sepolia Faucet (Alchemy)](https://www.alchemy.com/faucets/ethereum-sepolia)
- [Sepolia Faucet (Chainlink)](https://faucets.chain.link/sepolia)
- [Sepolia Etherscan](https://sepolia.etherscan.io/)
- [Documentazione OpenZeppelin ERC-20](https://docs.openzeppelin.com/contracts/4.x/erc20)

---

## Licenza

MIT
