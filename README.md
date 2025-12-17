# CyclingToken (CYCL) - Project 2.0

Benvenuto nel repository di CyclingToken! Questo progetto è un'applicazione "Bike-to-Earn" che premia gli utenti con token ERC-20 per i chilometri percorsi in bicicletta.

Il progetto è stato aggiornato per supportare lo sviluppo locale rapido e il deploy economico su Polygon Amoy.

---

## 📂 Struttura del Progetto

Ecco i file e le cartelle principali che sono stati creati o modificati:

### Smart Contracts & Backend (Hardhat)
- **`contracts/CyclingToken.sol`**: Lo Smart Contract ERC-20 del token.
- **`hardhat.config.js`**: Configurazione aggiornata per supportare **Localhost** e **Polygon Amoy**.
- **`test/CyclingToken.js`** (Nuovo): Suite di test automatizzati per verificare il funzionamento del contratto senza spendere gas.
- **`scripts/deploy_amoy.js`** (Nuovo): Script specifico per il deploy sulla testnet Polygon Amoy.

### Frontend (React + Vite)
- **`frontend/`** (Nuovo): Cartella contenente l'applicazione web.
  - **`src/App.jsx`**: Logica principale dell'interfaccia e connessione al Wallet.
  - **`src/index.css`**: Stili globali con design "Dark Mode" premium.
  - **`package.json`**: Gestione dipendenze (Vite, Ethers, Framer Motion).

### Backend (Node.js + Express)
- **`backend/`** (Nuovo): Server API per la gestione dei dati e del database.
  - **`server.js`**: Entry point del server Express.
  - **`package.json`**: Dipendenze del backend (Express, CORS, Dotenv).

---

## 🌟 Nuove Funzionalità (v2.1 - Server-Side Minting)

### 1. Gasless Claiming (Server-Side Minting)
L'utente non deve più pagare le fee del gas per reclamare i token.
- **Frontend**: Invia una richiesta di claim al server.
- **Server**: Verifica i dati e "minta" i token direttamente nel wallet dell'utente pagando il gas.
- **Vantaggio**: Esperienza utente fluida (nessun popup di firma) e zero costi per l'utente finale.

### 2. Verifica Dati IPFS (Security) 🔒
Prima di mintare i token, il server esegue un controllo di sicurezza rigoroso:
1.  **Scarica** i dati grezzi delle corse da IPFS (Pinata).
2.  **Ricalcola** indipendentemente la somma dei premi previsti.
3.  **Confronta** il totale calcolato con il saldo pendente nel database.
4.  Se i due valori non coincidono, la transazione viene bloccata.
Questo garantisce che **ogni token mintato sia supportato da dati reali e immutabili su IPFS**.

### 3. Architettura Ibrida (IPFS + Blockchain)
Per ridurre i costi del gas e mantenere i dati decentralizzati:
- **Dati Off-Chain (IPFS)**: I dettagli della corsa vengono salvati su IPFS tramite il Backend.
- **Dati On-Chain (Polygon)**: Lo Smart Contract salva il CID (Content Identifier) come prova del lavoro svolto.

---

## 🚀 Guida Rapida: Come Attivare Tutto

### 1. Prerequisiti
Assicurati di avere installato:
- Node.js (v18 o superiore)
- Un Wallet (Rabby o MetaMask) installato nel browser.
- PostgreSQL installato e attivo.

### 2. Setup Database (PostgreSQL)
1.  **Crea Utente e Database**:
    ```bash
    sudo -u postgres psql -c "CREATE USER cycling_user WITH PASSWORD 'secure_password';"
    sudo -u postgres psql -c "CREATE DATABASE cycling_token_db OWNER cycling_user;"
    ```
2.  **Crea le Tabelle**:
    ```bash
    sudo -u postgres psql -d cycling_token_db -c "CREATE TABLE users (wallet_address VARCHAR(42) PRIMARY KEY, pending_balance NUMERIC DEFAULT 0, total_km NUMERIC DEFAULT 0, is_pro BOOLEAN DEFAULT FALSE, pro_expiry TIMESTAMP, pending_cids TEXT[] DEFAULT '{}');"
    sudo -u postgres psql -d cycling_token_db -c "GRANT ALL PRIVILEGES ON TABLE users TO cycling_user;"
    ```

### 3. Setup Backend (Server API)
Spostati nella cartella del backend:
```bash
cd backend
npm install
```

#### Configurazione `.env`
Crea un file `.env` nella cartella `backend/` con i seguenti dati (CRITICO):
```env
# Database
DB_USER=cycling_user
DB_HOST=localhost
DB_NAME=cycling_token_db
DB_PASSWORD=secure_password
DB_PORT=5432

# Blockchain (Il Server paga il Gas!)
RPC_URL=https://rpc-amoy.polygon.technology/
PRIVATE_KEY=tua_chiave_privata_del_wallet_owner
CONTRACT_ADDRESS=0x2AAd40100641dBd6336eDC60832fc237bFe39C95

# IPFS
PINATA_JWT=tua_chiave_jwt_lunghissima
```

Avvia il server:
```bash
node server.js
```
Il server sarà attivo su `http://localhost:3000`.

### 4. Setup Frontend (Interfaccia Web)
Spostati nella cartella del frontend:
```bash
cd frontend
npm install
npm run dev
```
Apri `http://localhost:5173`, connetti il wallet e inizia a pedalare!

---

## 🛠 Tecnologie Usate
- **Blockchain**: Solidity, Hardhat, Ethers.js
- **Server**: Node.js, Express, pg (PostgreSQL)
- **Frontend**: React 19, Vite, Reown AppKit (WalletConnect)
- **Storage**: Pinata (IPFS)
- **Network**: Polygon Amoy (Testnet)

---

## 📝 Note per lo Sviluppo
- **Design**: L'interfaccia usa CSS puro con variabili per un look moderno e facile da modificare.
- **Compatibilità**: Il progetto è configurato per funzionare con Node.js v18 (LTS).

## 🔮 Roadmap
- **Disaster Recovery**: Ricostruzione automatica del DB partendo dai dati on-chain e IPFS.
- **Mobile App**: Deploy della versione Android già configurata in `frontend/android`.
