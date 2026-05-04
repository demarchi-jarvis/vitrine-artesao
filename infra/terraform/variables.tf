variable "aws_region" {
  description = "Região AWS — Academy sempre us-east-1"
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Nome da aplicação (usado nos nomes dos recursos AWS)"
  type        = string
  default     = "vitrine-artesao"
}

variable "ami_id" {
  description = "Amazon Linux 2023 AMI para us-east-1. Verifique a versão mais recente em: https://aws.amazon.com/amazon-linux-ami/"
  type        = string
  default     = "ami-0453ec754f44f9a4a"
}

variable "instance_type" {
  description = "Tipo da instância EC2 (t3.micro = free tier; t3.small = melhor performance)"
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "Nome do par de chaves EC2. No Academy: 'vockey' (baixe vockey.pem no painel do Lab > SSH Key)"
  type        = string
  default     = "vockey"
}

variable "repo_url" {
  description = "URL pública do repositório Git a clonar na instância"
  type        = string
  default     = "https://github.com/demarchi-jarvis/vitrine-artesao.git"
}

variable "db_username" {
  description = "Usuário do PostgreSQL"
  type        = string
  default     = "bazar_user"
}

variable "db_password" {
  description = "Senha do PostgreSQL — mínimo 12 caracteres"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Chave de assinatura JWT — mínimo 32 caracteres aleatórios (gere com: openssl rand -hex 32)"
  type        = string
  sensitive   = true
}

variable "frontend_url" {
  description = "URL do frontend para CORS (ex: http://IP_EC2:3000). Deixe vazio enquanto o frontend não estiver implantado."
  type        = string
  default     = ""
}
