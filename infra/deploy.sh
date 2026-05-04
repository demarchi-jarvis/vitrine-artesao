#!/bin/bash
# Deploy incremental — rode na EC2 para atualizar sem recriar volumes
# Usage: ./infra/deploy.sh
# Pré-requisito: estar no diretório do projeto com .env configurado

set -euo pipefail

REPO_DIR=$(dirname "$(dirname "$(realpath "$0")")")
cd "$REPO_DIR"

echo "=== [1/3] Atualizando código ==="
git pull origin main

echo "=== [2/3] Rebuild e restart da aplicação ==="
docker compose build app
docker compose up -d --no-deps app

echo "=== [3/3] Verificando saúde da API ==="
sleep 10
MAX_ATTEMPTS=12
for i in $(seq 1 $MAX_ATTEMPTS); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health || true)
  if [ "$STATUS" = "200" ]; then
    echo "API saudável! Status: $STATUS"
    break
  fi
  echo "Tentativa $i/$MAX_ATTEMPTS — aguardando API... (status atual: $STATUS)"
  sleep 5
done

if [ "$STATUS" != "200" ]; then
  echo "ERRO: API não respondeu após $(($MAX_ATTEMPTS * 5))s"
  echo "Logs:"
  docker compose logs --tail=50 app
  exit 1
fi

echo ""
echo "Deploy concluído com sucesso!"
docker compose ps
