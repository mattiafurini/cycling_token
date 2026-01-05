#!/bin/bash

# 🚀 Script per avviare Backend e Frontend - Cycling Token

echo "🚴‍♂️ Cycling Token - Avvio Applicazione"
echo "========================================"
echo ""

# Colori
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Directory
BACKEND_DIR="/home/samirhff1/Documents/uni/m1/sem1/blockchaine/bycicle/cycling_token/backend"
FRONTEND_DIR="/home/samirhff1/Documents/uni/m1/sem1/blockchaine/bycicle/cycling_token/frontend"

# Funzione per verificare se processo è in esecuzione
check_process() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo -e "${YELLOW}⚠️  Porta $port già in uso${NC}"
        return 0
    else
        return 1
    fi
}

# 1. Verifica prerequisiti
echo -e "${GREEN}📋 Verifica prerequisiti...${NC}"

# Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js non installato!${NC}"
    exit 1
fi
echo "✅ Node.js: $(node --version)"

# PostgreSQL
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠️  PostgreSQL non trovato (potrebbe essere remoto)${NC}"
else
    echo "✅ PostgreSQL installato"
fi

echo ""

# 2. Backend
echo -e "${GREEN}🔧 Avvio Backend...${NC}"
cd "$BACKEND_DIR" || exit 1

# Verifica .env
if [ ! -f ".env" ]; then
    echo -e "${RED}❌ File .env non trovato in backend!${NC}"
    echo "Crea il file .env con:"
    echo "  DB_USER=your_user"
    echo "  DB_HOST=localhost"
    echo "  DB_NAME=cycling_db"
    echo "  DB_PASSWORD=your_password"
    echo "  DB_PORT=5432"
    echo "  PRIVATE_KEY=your_private_key"
    echo "  CONTRACT_ADDRESS=0x2AAd40100641dBd6336eDC60832fc237bFe39C95"
    exit 1
fi

# Verifica dipendenze
if [ ! -d "node_modules" ]; then
    echo "📦 Installazione dipendenze backend..."
    npm install
fi

# Verifica porta 3000
if check_process 3000; then
    echo "Backend già in esecuzione su porta 3000"
else
    echo "🚀 Avvio backend su porta 3000..."
    npm start &
    BACKEND_PID=$!
    echo "Backend PID: $BACKEND_PID"
    sleep 3
fi

echo ""

# 3. Frontend
echo -e "${GREEN}🎨 Avvio Frontend...${NC}"
cd "$FRONTEND_DIR" || exit 1

# Verifica dipendenze
if [ ! -d "node_modules" ]; then
    echo "📦 Installazione dipendenze frontend..."
    npm install
fi

# Verifica porta 5173
if check_process 5173; then
    echo "Frontend già in esecuzione su porta 5173"
else
    echo "🚀 Avvio frontend su porta 5173..."
    npm run dev &
    FRONTEND_PID=$!
    echo "Frontend PID: $FRONTEND_PID"
    sleep 3
fi

echo ""
echo -e "${GREEN}✅ Applicazione avviata!${NC}"
echo ""
echo "📍 URL disponibili:"
echo "   Backend:  http://localhost:3000"
echo "   Frontend: http://localhost:5173"
echo ""
echo "🧪 Test rapidi:"
echo "   curl http://localhost:3000/api/health"
echo "   xdg-open http://localhost:5173"
echo ""
echo "🛑 Per fermare:"
echo "   pkill -f 'node server.js'"
echo "   pkill -f 'vite'"
echo ""

# Trova IP locale per Android
LOCAL_IP=$(hostname -I | awk '{print $1}')
echo -e "${YELLOW}📱 Per Android, usa:${NC}"
echo "   Backend:  http://$LOCAL_IP:3000"
echo "   Frontend: http://$LOCAL_IP:5173"
echo ""

# Mostra log in tempo reale
echo "📊 Log in tempo reale (Ctrl+C per uscire):"
echo ""
tail -f /dev/null
