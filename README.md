# independencia-backend

Backend do projeto **Vitrine Virtual** — plataforma de e-commerce para artesãos do Vale do Café (incubadora Vassouras Tec).

Permite que artesãos cadastrem lojas, produtos e recebam pedidos. Compradores navegam produtos, criam pedidos e acompanham compras e vendas.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Framework | Spring Boot 4.0.0-SNAPSHOT |
| Linguagem | Java 24 |
| Banco | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Segurança | Spring Security + JWT (Auth0) |
| Migrações | Flyway |
| Utilitários | Lombok |

---

## Documentação interna

| Arquivo | O que cobre |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Camadas, pacotes, fluxo de dados |
| [docs/DOMAIN.md](docs/DOMAIN.md) | Entidades, relacionamentos, regras de negócio |
| [docs/API.md](docs/API.md) | Todos os endpoints, payloads e exemplos |
| [docs/SECURITY.md](docs/SECURITY.md) | JWT, BCrypt, CORS, filtros |
| [docs/DATABASE.md](docs/DATABASE.md) | Tabelas, colunas, Flyway, índices |
| [docs/PATTERNS.md](docs/PATTERNS.md) | Padrões de código, convenções, como adicionar features |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Setup local, variáveis de ambiente, como rodar |
| [docs/DEPLOY.md](docs/DEPLOY.md) | Docker, AWS, variáveis de produção |
| [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md) | Regras de domínio, validações, fluxos críticos |

---

## Início rápido

```bash
# 1. Clonar
git clone https://github.com/demarchi-jarvis/independencia-backend.git
cd independencia-backend

# 2. Configurar variáveis de ambiente
cp .env.example .env
# editar .env com suas credenciais locais

# 3. Subir PostgreSQL (Docker)
docker run -d \
  --name bazar-db \
  -e POSTGRES_DB=bazar \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:16

# 4. Rodar
./mvnw spring-boot:run
```

API disponível em `http://localhost:8081`.

---

## Variáveis de ambiente

Ver `.env.example` para lista completa. Obrigatórias em produção:

```
DB_URL=jdbc:postgresql://host:5432/bazar
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...   # mínimo 32 caracteres aleatórios
```

---

## Frontend

O frontend Angular consome esta API em `http://localhost:4200`. Configure CORS em `CorsConfig.java` se o domínio mudar.

---

## Projeto

Iniciativa da incubadora **Vassouras Tec** — Vale do Café, RJ.
Desenvolvido por Antonio Demarchi.
