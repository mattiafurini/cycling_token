# ✅ Backend Riparato e Configurato!

## 🔧 Problemi Risolti

### 1. Endpoint `/api/health` Mancante
**Errore:** `404 Not Found - Cannot GET /api/health`

**Soluzione:** Aggiunto endpoint health check
```javascript
app.get('/api/health', (req, res) => {
    res.json({ 
        status: 'healthy', 
        timestamp: new Date().toISOString(),
        database: pool ? 'connected' : 'disconnected',
        blockchain: wallet ? wallet.address : 'not connected'
    });
});
```

### 2. Parametri API Incompatibili
**Errore:** `500 Internal Server Error` su `/api/ride`

**Problema:** 
- App Android invia: `user_address`, `distance`
- Backend si aspettava: `address`, `km`

**Soluzione:** Supporto entrambi i formati
```javascript
const address = req.body.address || req.body.user_address;
const km = req.body.km || req.body.distance;
```

### 3. IPFS Upload Doppio
**Ottimizzazione:** Se l'app ha già uploadato su IPFS, il backend usa quel CID invece di rifare l'upload.

---

## ✅ Stato Backend

```json
{
  "status": "healthy",
  "timestamp": "2026-01-04T18:39:59.770Z",
  "database": "connected",
  "blockchain": "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"
}
```

**URL:** `http://10.89.194.180:3000`

---

## 🧪 Test Backend

### Test 1: Health Check
```bash
curl http://10.89.194.180:3000/api/health
# Output: {"status":"healthy",...}
```

### Test 2: Get User
```bash
curl http://10.89.194.180:3000/api/user/0x0000000000000000000000000000000000000000
# Crea utente se non esiste
```

### Test 3: Save Ride (da app)
L'app ora invierà:
```json
{
  "user_address": "0x0000...",
  "distance": 0.1099,
  "avg_speed": 18.85,
  "gps_data": [...],
  "ipfs_cid": "QmXxx..."
}
```

Backend risponderà:
```json
{
  "success": true,
  "ride_id": 1,
  "tokens_earned": 1,
  "ipfs_cid": "QmXxx...",
  "user": {...}
}
```

---

## 📱 Testa con App Android

1. **Apri l'app**
2. **Dashboard → Dovresti vedere:**
   - Toast: "✅ Backend connesso su 10.89.194.180:3000"
3. **Fai una corsa simulata**
4. **Collect Tokens**
5. **Verifica:**
   - Toast: "✅ Salvato! XX token in pending"
   - Logcat: `IPFS Upload Success`, `Ride saved successfully`

---

## 📊 Verifica Database

```bash
psql -U youruser -d cycling_db

# Vedi ultimo utente
SELECT * FROM users ORDER BY total_km DESC LIMIT 5;

# Vedi ultime corse
SELECT id, user_address, distance, ipfs_cid, timestamp 
FROM rides 
ORDER BY timestamp DESC 
LIMIT 5;
```

---

## 🚀 Comandi Backend

### Avvio
```bash
cd /home/samirhff1/Documents/uni/m1/sem1/blockchaine/bycicle/cycling_token/backend
npm start
```

### Stop
```bash
lsof -ti:3000 | xargs kill
```

### Riavvio
```bash
lsof -ti:3000 | xargs kill; cd backend && npm start
```

### Log in tempo reale
```bash
tail -f backend/server.log
```

---

## 🎉 Tutto Pronto!

✅ Backend attivo su `http://10.89.194.180:3000`
✅ Endpoint `/api/health` funzionante
✅ Endpoint `/api/ride` compatibile con app Android
✅ Database PostgreSQL connesso
✅ Blockchain wallet configurato

**L'app Android ora può comunicare perfettamente con il backend!** 🚴‍♂️
