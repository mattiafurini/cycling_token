#!/bin/bash

# Script per configurare automaticamente l'IP nell'app Android
# Cycling Token - Network Configuration Helper

echo "🔧 Cycling Token - Network Configuration Helper"
echo "================================================"
echo ""

# Trova IP locale
echo "📡 Rilevamento IP locale..."
LOCAL_IP=$(hostname -I | awk '{print $1}')

if [ -z "$LOCAL_IP" ]; then
    echo "❌ Impossibile rilevare IP automaticamente"
    echo "Esegui manualmente: ip a | grep inet"
    exit 1
fi

echo "✅ IP locale rilevato: $LOCAL_IP"
echo ""

# Verifica se backend è in esecuzione
echo "🔍 Verifica backend..."
if nc -z localhost 3000 2>/dev/null; then
    echo "✅ Backend in esecuzione sulla porta 3000"
else
    echo "⚠️  Backend NON in esecuzione!"
    echo "Avvia il backend con: cd cycling_token/backend && node server.js"
fi
echo ""

# Path del file Constants.kt
CONSTANTS_FILE="/home/samirhff1/AndroidStudioProjects/test/app/src/main/java/com/example/test/utils/Constants.kt"

echo "📝 File da modificare:"
echo "$CONSTANTS_FILE"
echo ""

echo "🔧 Istruzioni:"
echo "1. Apri il file Constants.kt"
echo "2. Cerca la riga:"
echo "   const val BASE_URL = \"http://192.168.1.XXX:3000/\""
echo ""
echo "3. Sostituisci con:"
echo "   const val BASE_URL = \"http://$LOCAL_IP:3000/\""
echo ""

# Chiedi se modificare automaticamente
read -p "Vuoi modificare automaticamente? (y/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -f "$CONSTANTS_FILE" ]; then
        # Backup
        cp "$CONSTANTS_FILE" "$CONSTANTS_FILE.backup"
        echo "✅ Backup creato: $CONSTANTS_FILE.backup"
        
        # Modifica file
        sed -i "s|const val BASE_URL = \"http://.*:3000/\"|const val BASE_URL = \"http://$LOCAL_IP:3000/\"|g" "$CONSTANTS_FILE"
        
        echo "✅ File modificato con IP: $LOCAL_IP"
        echo ""
        echo "📋 Prossimi passi:"
        echo "1. Apri Android Studio"
        echo "2. Sync Gradle (File → Sync Project)"
        echo "3. Build → Rebuild Project"
        echo "4. Run → Run 'app'"
        echo ""
    else
        echo "❌ File non trovato: $CONSTANTS_FILE"
        echo "Modifica manualmente il file."
    fi
else
    echo "OK, modifica manualmente il file."
fi

echo ""
echo "🧪 Test connettività:"
echo "Dal telefono, apri browser e vai a:"
echo "http://$LOCAL_IP:3000/api/health"
echo ""
echo "Dovresti vedere: {\"status\":\"healthy\"}"
echo ""

echo "📚 Documentazione:"
echo "- IMPLEMENTATION_SUMMARY.md   - Riepilogo completo"
echo "- NETWORK_SETUP_GUIDE.md      - Guida setup dettagliata"
echo "- INTEGRATION_ROADMAP.md      - Piano implementazione"
echo ""

echo "✅ Configurazione completata!"
