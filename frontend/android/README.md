# 🚴 CyclingToken Android App

App Android nativa per il progetto **CyclingToken** - guadagna token pedalando!

---

## 📱 Descrizione

CyclingToken è un'applicazione Android **Bike-to-Earn** che permette agli utenti di guadagnare token CYCL (ERC-20) pedalando. L'app traccia le corse simulate, carica i dati su IPFS tramite Pinata, e interagisce con uno smart contract sulla rete **Polygon Amoy Testnet** per il minting dei token.

---

## 🛠️ Tecnologie Utilizzate

### **Linguaggio e Framework**
- **Kotlin** - Linguaggio principale
- **Android SDK** (API 34) - Framework nativo Android
- **Material Design 3** - UI Components

### **Blockchain e Web3**
- **Web3j v4.9.8** - Interazione con blockchain Ethereum/Polygon
- **Polygon Amoy Testnet** (Chain ID: 80002)
- **Alchemy RPC** - Provider RPC per query blockchain
- **Smart Contract CYCL**: `0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991`

### **Backend e Storage**
- **Retrofit 2.9.0** - HTTP client per API REST
- **OkHttp** - Networking
- **IPFS/Pinata** - Storage decentralizzato dei dati delle corse
- **Backend Node.js** - Server su `https://cycling.98.66.138.159.nip.io/`

### **Librerie Android**
- **Lifecycle & ViewModel** - Architettura MVVM
- **Coroutines** - Programmazione asincrona
- **Google Play Services Location** - GPS tracking
- **SharedPreferences** - Persistenza locale
- **ViewBinding** - Binding sicuro delle view

### **Build Tools**
- **Gradle 8.7** con Kotlin DSL
- **Android Gradle Plugin 8.5.2**
- **Kotlin 1.9.0**

---

## 🏗️ Architettura

L'app segue il pattern **MVVM** (Model-View-ViewModel):

```
app/
├── data/                    # Modelli dati e repository locale
│   ├── RideRepository.kt   # Gestione corse salvate localmente
│   └── LocationPoint.kt    # Modello punto GPS
├── network/                # Layer networking
│   ├── ApiService.kt       # Definizione API Retrofit
│   ├── RetrofitClient.kt   # Client HTTP
│   └── models/             # DTO per API
├── repository/             # Repository pattern
│   └── NetworkRepository.kt # Comunicazione backend
├── ui/                     # Layer presentazione
│   ├── dashboard/          # Tracking corse
│   ├── history/            # Storico corse
│   ├── profile/            # Profilo utente
│   └── wallet/             # Connessione wallet
├── utils/                  # Utilities
│   ├── Constants.kt        # Configurazioni
│   └── IpfsHelper.kt       # Upload IPFS
└── wallet/                 # Blockchain interaction
    ├── WalletManager.kt    # Gestione wallet
    └── Web3jContractHelper.kt # Interazione smart contract
```

---

## 🚀 Funzionalità

### **1. Dashboard - Tracking Corse**
- **Simulazione GPS** automatica (nessun GPS reale richiesto)
- Tracking in tempo reale di:
  - Velocità (km/h)
  - Distanza percorsa (km)
  - Accelerazione (m/s²)
- **Background tracking**: il ride continua anche cambiando tab
- Calcolo automatico token: **1 token per km**
- Bottone "Riscuoti X.XX CYCL" che mostra i token da guadagnare
- Stato persistente: il bottone rimane visibile tra le navigazioni

### **2. Wallet Connection**
- Setup guidato con istruzioni passo-passo
- Copia automatica di:
  - Configurazione rete Polygon Amoy
  - Indirizzo contratto token
- Validazione indirizzo wallet (formato Ethereum)
- Salvataggio persistente in SharedPreferences

### **3. Profilo Utente**
- Visualizzazione stato wallet (Attivo/Inattivo)
- Indirizzo wallet abbreviato
- **Saldo token CYCL** caricato dalla blockchain in tempo reale
- Formato con 2 decimali

### **4. Storico Corse**
- Lista corse salvate localmente
- Dettagli per ogni corsa:
  - Data e ora
  - Distanza percorsa
  - Velocità media
  - Token guadagnati
- Storage JSON persistente

---

## 🔄 Flusso di Funzionamento

### **1. Setup Iniziale**
1. Apri l'app → vai a **Wallet**
2. Segui la configurazione guidata:
   - Aggiungi rete Polygon Amoy in MetaMask
   - Aggiungi token CYCL (`0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991`)
   - Inserisci il tuo indirizzo wallet

### **2. Registrare una Corsa**
1. Vai alla **Dashboard**
2. Premi **"Start Riding"**
   - La simulazione GPS parte automaticamente
   - Velocità e distanza si aggiornano ogni secondo
3. Pedala (simulato) - la distanza si accumula
4. Premi **"Stop Riding"**
   - Appare bottone "Riscuoti X.XX CYCL"
5. Premi **"Riscuoti X.XX CYCL"**
   - La corsa viene salvata localmente
   - I dati vengono caricati su **IPFS** (Pinata)
   - Viene inviata richiesta al **backend**

### **3. Processo Backend (Automatico)**
1. App verifica esistenza utente nel database
2. App carica su IPFS:
   ```json
   {
     "user": "0x981e...",
     "km": 2.45,
     "reward": 2.45,
     "timestamp": 1736268000000,
     "type": "Cycling Session",
     "app": "CyclingToken Android"
   }
   ```
3. Backend riceve richiesta `/api/ride` con GPS data (solo per DB)
4. **Attesa 4 secondi** per sincronizzazione Pinata
5. Backend riceve richiesta `/api/claim`
6. Backend verifica dati su IPFS
7. **Smart contract minta i token** all'utente
8. **Attesa 2 secondi** per conferma blockchain
9. App aggiorna saldo dal contratto

### **4. Visualizzare i Token**
1. Vai al **Profilo**
2. Il saldo viene caricato automaticamente dalla blockchain
3. Oppure apri MetaMask per vedere i token

---

## 🔐 Sicurezza e Privacy

- **Nessuna chiave privata** salvata nell'app
- **Solo lettura** dalla blockchain (query balance)
- **Backend gestisce il minting** (chiave privata lato server)
- Wallet address salvato in SharedPreferences (locale)
- Validazione formato indirizzo Ethereum

---

## 📊 Dati Salvati

### **IPFS (Pinata)**
```json
{
  "user": "0x...",
  "km": 2.45,
  "reward": 2.45,
  "timestamp": 1736268000000,
  "type": "Cycling Session",
  "app": "CyclingToken Android"
}
```
**Nota**: **NO GPS coordinates** su IPFS (solo distanza e reward)

### **Backend Database (PostgreSQL)**
- Utente: wallet_address, pending_balance, total_km
- Corse: distance, avg_speed, gps_data, ipfs_cid, timestamp

### **Local Storage (Android)**
- Corse completate (JSON in SharedPreferences)
- Indirizzo wallet
- Storico ride

---

## ⚙️ Configurazione

### **File `Constants.kt`**
```kotlin
const val BASE_URL = "https://cycling.98.66.138.159.nip.io/"
const val CONTRACT_ADDRESS = "0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991"
const val RPC_URL = "https://polygon-amoy.g.alchemy.com/v2/VdS_PBkq5kFNVYrolRkh4"
const val CHAIN_ID = 80002L
const val PINATA_JWT = "eyJhbGci..."
```

### **Permessi Android**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## 🚧 Build e Installazione

### **Prerequisiti**
- Android Studio Hedgehog o superiore
- JDK 17
- Android SDK 34
- Device/Emulator Android 8.0+ (API 26)

### **Build**
```bash
cd /home/samirhff1/AndroidStudioProjects/test
./gradlew assembleDebug
```

### **Installazione**
```bash
./gradlew installDebug
```

### **Build Release (Firmato)**
```bash
./gradlew assembleRelease
```

---

## 🐛 Troubleshooting

### **"Backend offline"**
- Verifica che il server su `https://cycling.98.66.138.159.nip.io/` sia attivo
- Controlla connessione internet
- L'app funziona in modalità offline (salva localmente)

### **"Saldo 0 CYCL"**
- Verifica che il wallet sia connesso
- Controlla su PolygonScan se i token sono stati mintati
- Aspetta qualche secondo per aggiornamento blockchain

### **"Failed to fetch CID from IPFS"**
- Pinata potrebbe essere lento (attesa 4 secondi integrata)
- Verifica PINATA_JWT valido
- Check logs per errori di rete

### **Token mismatch DB/IPFS**
- RISOLTO: App ora usa stessa struttura dati del frontend
- Calcolo: 1 token per km (Float, non Int)

---

## 📝 Note di Sviluppo

### **Modifiche Recenti**
1. ✅ Rimossa invio GPS a IPFS (solo distanza e reward)
2. ✅ Distanza si accumula solo durante ride attivo
3. ✅ Ride continua in background (cambio tab)
4. ✅ Bottone "Riscuoti" mostra token esatti
5. ✅ Bottone persiste tra navigazioni (onSaveInstanceState)
6. ✅ Notifica backend mostra indirizzo corretto da Constants
7. ✅ Token calculation: Float invece di Int (matching server)
8. ✅ 2-phase claiming: save ride → delay 4s → claim → delay 2s

### **Differenze con Frontend Web**
| Feature | Android | Web |
|---------|---------|-----|
| GPS | Simulato | Simulato |
| Wallet | Manuale (copia/incolla) | WalletConnect (AppKit) |
| IPFS Upload | App-side | Server-side |
| Token Balance | Web3j query | Ethers.js |
| Storage | SharedPreferences | LocalStorage |

---

## 📞 Supporto

- **Repository**: cycling_token
- **Smart Contract CYCL**: [PolygonScan Amoy](https://amoy.polygonscan.com/address/0xa5D6df2fF2ab79fbf77A588CB2AdDc125667a991)
- **Backend**: Node.js + PostgreSQL + Hardhat
- **Frontend Web**: React + Vite + ethers.js

---

## 🎯 Roadmap Future

- [ ] GPS reale invece di simulazione
- [ ] Notifiche push per token mintati
- [ ] Leaderboard utenti
- [ ] Pro membership con moltiplicatori
- [ ] NFT badges per milestone
- [ ] Dark mode
- [ ] Multi-lingua (EN, IT)

---

## 📄 Licenza

Progetto universitario - M1 Blockchain 2026

---

**Versione**: 1.0.0  
**Ultima modifica**: 7 Gennaio 2026  
**Testato su**: Android 14 (API 34)
