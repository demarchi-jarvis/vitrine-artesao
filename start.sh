#!/bin/bash
# start.sh — inicia a Vitrine Virtual localmente com Docker em 1 comando
set -euo pipefail

# Cria .env a partir do exemplo se não existir
if [ ! -f .env ]; then
  cp .env.example .env
  echo ""
  echo "  .env criado! Edite o arquivo e defina:"
  echo "    DB_PASSWORD=<senha forte>"
  echo "    JWT_SECRET=<openssl rand -hex 32>"
  echo ""
  echo "  Depois rode ./start.sh novamente."
  exit 0
fi

# Verifica se Docker está rodando
docker info > /dev/null 2>&1 || {
  echo "Docker não está rodando. Inicie o Docker e tente novamente."
  exit 1
}

echo ""
echo "  Iniciando Vitrine Virtual..."
echo ""

docker compose up -d --build

echo ""
echo "  Pronto! Aguarde ~60s para a API inicializar."
echo ""
echo "  API:    http://localhost:8081"
echo "  Health: http://localhost:8081/actuator/health"
echo "  Logs:   docker compose logs -f app"
echo "  Parar:  docker compose down"
echo ""
