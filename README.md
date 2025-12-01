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

## 🌟 Nuove Funzionalità (v2.0)

### 1. Lazy Minting & Claiming
Il sistema non pre-minta più i token. I token vengono creati ("mintati") solo quando un utente li reclama.
- **Vantaggio**: Supply dinamica che cresce solo con l'attività reale degli utenti.

### 2. Simulazione "Serverless"
In questa demo, il frontend simula un'architettura client-server completa direttamente nel browser.
- **Utente**: Clicca su "Start Riding" per accumulare km e token (simulati).
- **Server (Owner)**: Quando l'utente clicca su "Claim Reward", il wallet connesso (se è l'Owner) firma la transazione e paga le gas fee, simulando il comportamento di un server backend che premia l'utente.
- **Sicurezza**: Il frontend impedisce a wallet non-owner di eseguire il claim, garantendo che solo il "Server" possa autorizzare il minting.

---

## 🚀 Guida Rapida: Come Attivare Tutto

Segui questi passaggi per far partire il progetto sul tuo computer.

### 1. Prerequisiti
Assicurati di avere installato:
- Node.js (v18 o superiore)
- Un Wallet (Rabby o MetaMask) installato nel browser.

### 2. Setup Smart Contracts (Backend)

Installa le dipendenze nella cartella principale:
```bash
npm install
```

#### Eseguire i Test (Consigliato)
Verifica che il contratto funzioni correttamente eseguendo i test locali:
```bash
npx hardhat test
```
*Dovresti vedere 5 spunte verdi.*

#### (Opzionale) Deploy su Polygon Amoy
Il contratto è già stato deployato sulla testnet Polygon Amoy all'indirizzo: `0x4944D1A1d57e118f50B318B039210f53a5c9B7Eb`.

Se vuoi farne uno nuovo:
1. Crea un file `.env` con la tua `PRIVATE_KEY` e `POLYGON_AMOY_RPC_URL`.
2. Esegui:
   ```bash
   npx hardhat run scripts/deploy.js --network amoy
   ```

### 3. Setup Frontend (Interfaccia Web)

Spostati nella cartella del frontend e installa le librerie:
```bash
cd frontend
npm install
```

Avvia il server di sviluppo:
```bash
npm run dev
```

Ora apri il link che appare nel terminale (solitamente `http://localhost:5173`).
Clicca su **"Connect Wallet"** in alto a destra per collegare il tuo Rabby Wallet!

### 4. Setup Backend (Server API)

Spostati nella cartella del backend e avvia il server:
```bash
cd backend
npm install
node server.js
```
Il server sarà attivo su `http://localhost:3000`.

### 5. Setup Database (PostgreSQL)

Per far funzionare il backend, devi configurare un database PostgreSQL locale.

1.  **Installa PostgreSQL**:
    ```bash
    sudo apt-get install postgresql postgresql-contrib
    sudo service postgresql start
    ```

2.  **Crea Utente e Database**:
    Esegui questi comandi nel terminale:
    ```bash
    sudo -u postgres psql -c "CREATE USER cycling_user WITH PASSWORD 'secure_password';"
    sudo -u postgres psql -c "CREATE DATABASE cycling_token_db OWNER cycling_user;"
    ```

3.  **Crea la Tabella e Assegna Permessi**:
    ```bash
    sudo -u postgres psql -d cycling_token_db -c "CREATE TABLE users (wallet_address VARCHAR(42) PRIMARY KEY, pending_balance NUMERIC DEFAULT 0, total_km NUMERIC DEFAULT 0, is_pro BOOLEAN DEFAULT FALSE, pro_expiry TIMESTAMP);"
    sudo -u postgres psql -d cycling_token_db -c "GRANT ALL PRIVILEGES ON TABLE users TO cycling_user;"
    ```

### 6. Abbonamento PRO 🏆
Il sistema include ora un abbonamento "Pro" che offre vantaggi esclusivi:
- **Costo**: 100 CYCL (bruciati per sempre).
- **Vantaggio**: +20% di guadagno sui km percorsi (12 CYCL ogni 10km invece di 10).
- **Attivazione**: Clicca sull'icona della coppa 🏆 nella card principale. Se hai abbastanza token, potrai fare l'upgrade.

### 7. Versione Mobile (Android) 📱

Il progetto è pronto per essere trasformato in un'App Android nativa.

1. **Installa Android Studio**: Scaricalo dal sito ufficiale.
2. **Apri il progetto**:
   - Apri Android Studio.
   - Seleziona "Open" e naviga nella cartella `cycling_token/frontend/android`.
3. **Genera l'APK**:
   - Attendi che Gradle finisca la sincronizzazione.
   - Vai su `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
   - Troverai il file `.apk` nella cartella di output (Android Studio ti mostrerà una notifica "locate").
4. **Installa sul telefono**: Invia il file `.apk` al tuo telefono e installalo!

---

## 🛠 Tecnologie Usate
- **Blockchain**: Solidity, Hardhat, Ethers.js v6
- **Frontend**: React 19, Vite, Framer Motion (Animazioni), Lucide React (Icone)
- **Network**: Hardhat Localhost (Sviluppo), Polygon Amoy (Testnet)

---

## 📝 Note per lo Sviluppo
- **Design**: L'interfaccia usa CSS puro con variabili per un look moderno e facile da modificare.
- **Compatibilità**: Il progetto è configurato per funzionare con Node.js v18 (LTS).
