terraform {
  required_version = ">= 1.3"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# AWS Academy usa credenciais temporárias (com session token).
# Configure via variáveis de ambiente ANTES de rodar terraform apply:
#
#   export AWS_ACCESS_KEY_ID="..."
#   export AWS_SECRET_ACCESS_KEY="..."
#   export AWS_SESSION_TOKEN="..."         ← obrigatório no Academy
#   export AWS_DEFAULT_REGION="us-east-1"
#
# Ou coloque em ~/.aws/credentials com profile [default].
# O provider pega AWS_SESSION_TOKEN automaticamente se estiver na env.
provider "aws" {
  region = var.aws_region

  # Necessário no AWS Academy: evita chamadas IAM que a sandbox bloqueia
  skip_credentials_validation = true
  skip_requesting_account_id  = true
  skip_metadata_api_check     = true
}

data "aws_vpc" "default" {
  default = true
}

resource "aws_security_group" "vitrine" {
  name        = "${var.app_name}-sg"
  description = "Vitrine Virtual — API + SSH"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Spring Boot API"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP (Nginx / futuro frontend)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Frontend React / Angular"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.app_name}-sg"
    Project = var.app_name
  }
}

resource "aws_instance" "vitrine" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.vitrine.id]

  user_data = templatefile("${path.module}/userdata.tpl", {
    repo_url     = var.repo_url
    db_username  = var.db_username
    db_password  = var.db_password
    jwt_secret   = var.jwt_secret
    frontend_url = var.frontend_url
  })

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  tags = {
    Name    = var.app_name
    Project = var.app_name
  }
}
