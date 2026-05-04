#!/bin/bash
# AWS Academy — Criação de Security Group + EC2 via CLI
# Execute na sua máquina local com AWS Academy credentials configuradas
# Limitações Academy: sem criar IAM users, sem EIP persistente, região us-east-1

set -euo pipefail

REGION="us-east-1"
APP_NAME="vitrine-virtual"
EC2_TYPE="t3.small"          # t2.micro para free tier; t3.small para melhor performance
AMI_ID="ami-0453ec754f44f9a4a" # Amazon Linux 2023 us-east-1 (verifique a mais recente)
KEY_NAME="vockey"            # Chave padrão do AWS Academy Lab

echo "=== [1/3] Criando Security Group ==="
SG_ID=$(aws ec2 create-security-group \
  --region "$REGION" \
  --group-name "${APP_NAME}-sg" \
  --description "Vitrine Virtual — API + SSH" \
  --query 'GroupId' \
  --output text)

echo "Security Group criado: $SG_ID"

# SSH — restrinja ao seu IP em produção real
aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
  --protocol tcp --port 22 --cidr "0.0.0.0/0"

# API Spring Boot
aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
  --protocol tcp --port 8081 --cidr "0.0.0.0/0"

# HTTP (para futura configuração de Nginx/React)
aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
  --protocol tcp --port 80 --cidr "0.0.0.0/0"

# React (se servido na EC2)
aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
  --protocol tcp --port 3000 --cidr "0.0.0.0/0"

echo "=== [2/3] Lançando instância EC2 ==="
INSTANCE_ID=$(aws ec2 run-instances \
  --region "$REGION" \
  --image-id "$AMI_ID" \
  --instance-type "$EC2_TYPE" \
  --key-name "$KEY_NAME" \
  --security-group-ids "$SG_ID" \
  --user-data file://userdata.sh \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":20,"VolumeType":"gp3"}}]' \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${APP_NAME}}]" \
  --query 'Instances[0].InstanceId' \
  --output text)

echo "Instância lançada: $INSTANCE_ID"

echo "=== [3/3] Aguardando IP público ==="
sleep 15
PUBLIC_IP=$(aws ec2 describe-instances \
  --region "$REGION" \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo ""
echo "=============================================="
echo "  Deploy iniciado!"
echo "  IP público: $PUBLIC_IP"
echo "  SSH: ssh -i ~/vockey.pem ec2-user@$PUBLIC_IP"
echo "  API (aguarde ~3min): http://$PUBLIC_IP:8081"
echo "  Health: http://$PUBLIC_IP:8081/actuator/health"
echo "=============================================="
echo ""
echo "  IMPORTANTE: O UserData leva ~5 min para terminar."
echo "  Acompanhe: ssh ec2-user@$PUBLIC_IP 'tail -f /var/log/userdata.log'"
