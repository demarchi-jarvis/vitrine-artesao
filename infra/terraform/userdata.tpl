#!/bin/bash
# Vitrine Virtual — EC2 bootstrap script
# Gerado pelo Terraform | logs em: /var/log/userdata.log
set -euo pipefail
exec > /var/log/userdata.log 2>&1

echo "=== [1/5] Atualizando sistema ==="
dnf update -y

echo "=== [2/5] Instalando Docker + Git ==="
dnf install -y docker git
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

echo "=== [3/5] Instalando Docker Compose v2 ==="
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL \
  "https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version

echo "=== [4/5] Clonando repositório ==="
REPO_DIR="/opt/vitrine-artesao"
# Retry até 3 vezes (rede pode demorar a estabilizar no boot)
for i in 1 2 3; do
  git clone "${repo_url}" $REPO_DIR && break || {
    echo "Tentativa $i falhou, aguardando 10s..."
    sleep 10
  }
done
cd $REPO_DIR

echo "=== [5/5] Configurando ambiente e iniciando containers ==="
cat > .env <<'ENVEOF'
DB_USERNAME=${db_username}
DB_PASSWORD=${db_password}
JWT_SECRET=${jwt_secret}
FRONTEND_URL=${frontend_url}
ENVEOF

docker compose up -d --build

echo "=== Aguardando API ficar pronta ==="
MAX=24  # 24 x 10s = 4 min
for i in $(seq 1 $MAX); do
  STATUS=$(curl -s -o /dev/null -w "%%{http_code}" http://localhost:8081/actuator/health || echo "000")
  if [ "$STATUS" = "200" ]; then
    echo "API respondendo! (tentativa $i)"
    break
  fi
  echo "Tentativa $i/$MAX — status: $STATUS — aguardando..."
  sleep 10
done

PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "IP_DESCONHECIDO")
echo ""
echo "======================================================"
echo "  Deploy concluído!"
echo "  API:    http://$PUBLIC_IP:8081"
echo "  Health: http://$PUBLIC_IP:8081/actuator/health"
echo "  Logs:   docker compose logs -f app"
echo "  SSH:    ec2-user@$PUBLIC_IP"
echo "======================================================"
echo ""
echo "  Para implantar o frontend React/Angular:"
echo "  1. Copie os arquivos para $REPO_DIR/frontend"
echo "  2. Descomente o serviço 'frontend' no docker-compose.yml"
echo "  3. docker compose up -d --build frontend"
