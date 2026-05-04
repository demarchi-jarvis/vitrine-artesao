#!/bin/bash
# EC2 UserData — Vitrine Virtual (AWS Academy)
# Cola isso em "Advanced Details > User Data" ao lançar a instância EC2
# Testado em: Amazon Linux 2023 AMI, t3.small

set -euo pipefail
exec > /var/log/userdata.log 2>&1

echo "=== [1/5] Atualizando sistema ==="
dnf update -y

echo "=== [2/5] Instalando Docker ==="
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
REPO_DIR="/opt/vitrine-virtual"
git clone https://github.com/demarchi-jarvis/independencia-backend.git "$REPO_DIR"
cd "$REPO_DIR"

echo "=== [5/5] Criando .env e iniciando containers ==="
# ATENÇÃO: troque os valores abaixo antes de usar em produção!
cat > .env <<EOF
DB_USERNAME=bazar_user
DB_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)
FRONTEND_URL=
EOF

docker compose up -d --build

echo "=== Deploy concluído ==="
echo "API disponível em: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8081"
echo "Health: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8081/actuator/health"
