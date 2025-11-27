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
Se vuoi mettere il contratto online sulla testnet pubblica:
1. Crea un file `.env` con la tua `PRIVATE_KEY` e `POLYGON_AMOY_RPC_URL`.
2. Esegui:
   ```bash
   npx hardhat run scripts/deploy_amoy.js --network amoy
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

### 4. Versione Mobile (Android) 📱

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
