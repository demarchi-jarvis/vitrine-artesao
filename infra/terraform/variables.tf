variable "aws_region" {
  description = "AWS region (Academy padrão: us-east-1)"
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Nome da aplicação — usado para nomear recursos AWS"
  type        = string
  default     = "vitrine-artesao"
}

variable "ami_id" {
  description = "Amazon Linux 2023 AMI para us-east-1 (verifique a versão mais recente)"
  type        = string
  default     = "ami-0453ec754f44f9a4a"
}

variable "instance_type" {
  description = "Tipo da instância EC2"
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "Nome do par de chaves EC2 (AWS Academy padrão: vockey)"
  type        = string
  default     = "vockey"
}

variable "repo_url" {
  description = "URL do repositório Git a clonar na instância"
  type        = string
  default     = "https://github.com/demarchi-jarvis/vitrine-artesao.git"
}

variable "db_username" {
  description = "Usuário do PostgreSQL"
  type        = string
  default     = "bazar_user"
}

variable "db_password" {
  description = "Senha do PostgreSQL"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Chave secreta JWT (mínimo 32 caracteres)"
  type        = string
  sensitive   = true
}

variable "frontend_url" {
  description = "URL do frontend para CORS (deixe vazio enquanto não estiver implantado)"
  type        = string
  default     = ""
}
