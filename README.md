# Vitrine Virtual — Backend

Plataforma de e-commerce para artesãos do Vale do Café, desenvolvida para a incubadora **Vassouras Tec / UniVassouras**.

Artesãos cadastram lojas e produtos. Compradores navegam, criam pedidos e acompanham compras — tudo via API REST segura com JWT.

---

## Início rápido (3 comandos)

```bash
git clone https://github.com/demarchi-jarvis/vitrine-artesao.git
cd vitrine-artesao
./start.sh   # cria .env, builda e sobe tudo via Docker
```

API disponível em `http://localhost:8081` · Health: `http://localhost:8081/actuator/health`

> **Tutorial completo** (local + AWS Academy): [docs/TUTORIAL.md](docs/TUTORIAL.md)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Framework | Spring Boot 3.4.1 |
| Linguagem | Java 21 (LTS) |
| Banco | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Segurança | Spring Security + JWT (Auth0) |
| Migrações | Flyway |
| Infraestrutura | Docker Compose + Terraform (AWS) |

---

## Comandos úteis

```bash
make dev          # sobe o stack completo (postgres + app)
make test         # inicia test-db e roda os 38 testes de integração
make logs         # acompanha logs do app
make down         # para tudo
make deploy       # terraform apply → sobe na AWS Academy
```

---

## Publicar na AWS Academy

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# preencher db_password e jwt_secret

# colar credenciais do Lab no terminal:
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_SESSION_TOKEN="..."

terraform init && terraform apply
```

Infraestrutura criada em ~30s · API live em ~8 min · [Tutorial completo](docs/TUTORIAL.md#7-publicar-na-aws-academy-via-terraform)

---

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [docs/TUTORIAL.md](docs/TUTORIAL.md) | **Passo a passo completo: local + AWS Academy** |
| [docs/API.md](docs/API.md) | Todos os endpoints, payloads e exemplos |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Camadas, pacotes, fluxo de dados |
| [docs/DOMAIN.md](docs/DOMAIN.md) | Entidades, relacionamentos, regras de negócio |
| [docs/SECURITY.md](docs/SECURITY.md) | JWT, BCrypt, CORS, filtros |
| [docs/DATABASE.md](docs/DATABASE.md) | Tabelas, colunas, Flyway, índices |
| [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md) | Regras de domínio e validações |
| [docs/PATTERNS.md](docs/PATTERNS.md) | Convenções de código, como adicionar features |

---

## Variáveis de ambiente

Copie `.env.example` → `.env` e preencha:

```
DB_USERNAME=bazar_user
DB_PASSWORD=<openssl rand -hex 12>
JWT_SECRET=<openssl rand -hex 32>
FRONTEND_URL=                       # URL do React/Angular em produção
```

---

## Projeto

Iniciativa da incubadora **Vassouras Tec** — Vale do Café, RJ.  
Desenvolvido por Antonio Demarchi.
