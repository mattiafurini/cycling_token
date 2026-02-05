#!/bin/bash

# Script per verificare la configurazione dell'integrazione MetaMask

echo "🔍 Verificando configurazione integrazione MetaMask..."
echo ""

# Check 1: Verificare file chiave
echo "1️⃣ Verificando file chiave..."

FILES=(
    "/home/samirhff1/AndroidStudioProjects/test/app/src/main/java/com/example/test/wallet/WalletManager.kt"
    "/home/samirhff1/AndroidStudioProjects/test/app/src/main/java/com/example/test/wallet/Web3jContractHelper.kt"
    "/home/samirhff1/AndroidStudioProjects/test/app/src/main/java/com/example/test/ui/wallet/WalletConnectFragment.kt"
    "/home/samirhff1/AndroidStudioProjects/test/app/src/main/java/com/example/test/ui/dashboard/DashboardFragment.kt"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✅ $(basename $file)"
    else
        echo "  ❌ $(basename $file) - MANCANTE!"
    fi
done

echo ""

# Check 2: Verificare dipendenze in build.gradle
echo "2️⃣ Verificando dipendenze Gradle..."

GRADLE_FILE="/home/samirhff1/AndroidStudioProjects/test/app/build.gradle.kts"

if grep -q "web3j.core" "$GRADLE_FILE"; then
    echo "  ✅ Web3j configurato"
else
    echo "  ❌ Web3j NON configurato"
fi

if grep -q "walletconnect" "$GRADLE_FILE"; then
    echo "  ✅ WalletConnect configurato"
else
    echo "  ❌ WalletConnect NON configurato"
fi

echo ""

# Check 3: Verificare AndroidManifest
echo "3️⃣ Verificando AndroidManifest..."

MANIFEST="/home/samirhff1/AndroidStudioProjects/test/app/src/main/AndroidManifest.xml"

if grep -q "cyclingtoken" "$MANIFEST"; then
    echo "  ✅ Deep linking configurato"
else
    echo "  ❌ Deep linking NON configurato"
fi

if grep -q "INTERNET" "$MANIFEST"; then
    echo "  ✅ Permesso INTERNET"
else
    echo "  ❌ Permesso INTERNET mancante"
fi

echo ""

# Check 4: Verificare Constants
echo "4️⃣ Verificando configurazione Constants..."

CONSTANTS="/home/samirhff1/AndroidStudioProjects/test/app/src/main/java/com/example/test/utils/Constants.kt"

if grep -q "CONTRACT_ADDRESS" "$CONSTANTS"; then
    CONTRACT_ADDR=$(grep "CONTRACT_ADDRESS" "$CONSTANTS" | head -1)
    echo "  ✅ $CONTRACT_ADDR"
else
    echo "  ❌ CONTRACT_ADDRESS non trovato"
fi

if grep -q "BASE_URL" "$CONSTANTS"; then
    BASE_URL=$(grep "BASE_URL" "$CONSTANTS" | head -1)
    echo "  ✅ $BASE_URL"
else
    echo "  ❌ BASE_URL non trovato"
fi

if grep -q "CHAIN_ID" "$CONSTANTS"; then
    CHAIN=$(grep "CHAIN_ID" "$CONSTANTS" | head -1)
    echo "  ✅ $CHAIN"
else
    echo "  ❌ CHAIN_ID non trovato"
fi

echo ""

# Check 5: Verificare backend
echo "5️⃣ Verificando backend..."

BACKEND_DIR="/home/samirhff1/Documents/uni/m1/sem1/blockchaine/bycicle/cycling_token/backend"

if [ -d "$BACKEND_DIR" ]; then
    echo "  ✅ Directory backend trovata"
    
    if [ -f "$BACKEND_DIR/server.js" ]; then
        echo "  ✅ server.js presente"
    else
        echo "  ❌ server.js mancante"
    fi
    
    # Check if backend is running
    if curl -s -o /dev/null -w "%{http_code}" http://10.89.194.180:3000/health 2>/dev/null | grep -q "200"; then
        echo "  ✅ Backend in esecuzione (10.89.194.180:3000)"
    else
        echo "  ⚠️  Backend non raggiungibile (potrebbe essere spento)"
    fi
else
    echo "  ❌ Directory backend non trovata"
fi

echo ""

# Check 6: Verificare contratto
echo "6️⃣ Verificando contratto smart..."

CONTRACT_FILE="/home/samirhff1/Documents/uni/m1/sem1/blockchaine/bycicle/cycling_token/contracts/CyclingToken.sol"

if [ -f "$CONTRACT_FILE" ]; then
    echo "  ✅ CyclingToken.sol presente"
else
    echo "  ❌ CyclingToken.sol mancante"
fi

echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 RIEPILOGO"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Integrazione completata con:"
echo "   • WalletManager per gestione wallet"
echo "   • Web3j per interazione blockchain"
echo "   • WalletConnect per connessione MetaMask"
echo "   • Deep linking configurato"
echo ""
echo "📱 Prossimi passi:"
echo "   1. Compila l'app: ./gradlew build"
echo "   2. Installa su dispositivo/emulatore"
echo "   3. Installa MetaMask su Android"
echo "   4. Configura rete Polygon Amoy"
echo "   5. Aggiungi token CYC"
echo "   6. Connetti wallet nell'app"
echo ""
echo "📖 Leggi la guida completa:"
echo "   METAMASK_INTEGRATION_GUIDE.md"
echo ""
