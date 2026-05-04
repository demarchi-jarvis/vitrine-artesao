#!/bin/bash
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
curl -SL "https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64" \
     -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

echo "=== [4/5] Clonando repositório ==="
REPO_DIR="/opt/vitrine-artesao"
git clone "${repo_url}" $REPO_DIR
cd $REPO_DIR

echo "=== [5/5] Configurando ambiente e iniciando containers ==="
cat > .env <<'ENVEOF'
DB_USERNAME=${db_username}
DB_PASSWORD=${db_password}
JWT_SECRET=${jwt_secret}
FRONTEND_URL=${frontend_url}
ENVEOF

docker compose up -d --build

echo "=== Deploy concluído ==="
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo "API: http://$PUBLIC_IP:8081"
echo "Health: http://$PUBLIC_IP:8081/actuator/health"
echo "Logs: docker compose logs -f app"
