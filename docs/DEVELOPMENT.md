# Guia de Desenvolvimento Local

## Pré-requisitos

| Ferramenta | Versão mínima | Verificar |
|---|---|---|
| Java | 24 | `java -version` |
| Maven | 3.9+ (ou usar `./mvnw`) | `mvn -version` |
| Docker | 24+ | `docker -version` |
| Git | qualquer | `git --version` |

---

## Setup inicial

### 1. Clonar o repositório
```bash
git clone https://github.com/demarchi-jarvis/independencia-backend.git
cd independencia-backend
```

### 2. Configurar variáveis de ambiente
```bash
cp .env.example .env
```

Editar `.env`:
```
DB_URL=jdbc:postgresql://localhost:5432/bazar
DB_USERNAME=postgres
DB_PASSWORD=password
JWT_SECRET=dev-secret-key-only-for-local
PORT=8081
JPA_SHOW_SQL=true
```

### 3. Subir PostgreSQL com Docker
```bash
docker run -d \
  --name bazar-db \
  -e POSTGRES_DB=bazar \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:16
```

Verificar se subiu:
```bash
docker logs bazar-db
# Deve aparecer: "database system is ready to accept connections"
```

### 4. Rodar a aplicação

**Com Maven Wrapper (recomendado):**
```bash
./mvnw spring-boot:run
```

**Passando variáveis de ambiente na linha de comando:**
```bash
DB_URL=jdbc:postgresql://localhost:5432/bazar \
DB_USERNAME=postgres \
DB_PASSWORD=password \
JWT_SECRET=dev-key-123 \
./mvnw spring-boot:run
```

**Com IDE (IntelliJ):**
- Run → Edit Configurations → Spring Boot
- Active profiles: `dev`
- Environment variables: copiar do `.env`

---

## Verificação de que está funcionando

```bash
# Health check — deve retornar 401 (endpoint protegido)
curl -i http://localhost:8081/api/produtos

# Registrar usuário
curl -X POST http://localhost:8081/api/autenticacao/registrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teste","email":"teste@email.com","senha":"123456"}'

# Login
curl -X POST http://localhost:8081/api/autenticacao/login \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@email.com","senha":"123456"}'
# → Retorna { "nome": "Teste", "token": "eyJ..." }

# Usar o token
curl http://localhost:8081/api/usuarios/logado \
  -H "Authorization: Bearer eyJ..."
```

---

## Estrutura de branches

| Branch | Propósito |
|---|---|
| `main` | Produção — apenas via PR |
| `develop` | Integração — base para features |
| `feature/{nome}` | Nova feature |
| `fix/{nome}` | Correção de bug |
| `hotfix/{nome}` | Correção urgente em produção |

```bash
# Criar feature
git checkout develop
git pull origin develop
git checkout -b feature/avaliacao-produto

# Commitar
git add src/main/java/com/bazar/bazar/...
git commit -m "feat: adiciona endpoint de avaliação de produto"

# Abrir PR para develop
git push origin feature/avaliacao-produto
```

---

## Compilar e testar

```bash
# Compilar
./mvnw compile

# Rodar testes
./mvnw test

# Build completo (compila + testa + gera jar)
./mvnw package

# Pular testes no build
./mvnw package -DskipTests

# Rodar o jar gerado
java -jar target/bazar-0.0.1-SNAPSHOT.jar
```

---

## Banco de dados em desenvolvimento

```bash
# Acessar o banco via Docker
docker exec -it bazar-db psql -U postgres -d bazar

# Comandos úteis dentro do psql
\dt              # listar tabelas
\d usuarios      # descrever tabela
SELECT * FROM usuarios LIMIT 5;
SELECT * FROM produto ORDER BY data_criacao DESC LIMIT 10;

# Resetar o banco (CUIDADO — apaga tudo)
docker rm -f bazar-db
# subir novamente com o docker run acima
```

---

## Problemas comuns

### Porta 5432 em uso
```bash
# Verificar quem está usando
lsof -i :5432
# Parar PostgreSQL local se instalado
sudo service postgresql stop
```

### `LazyInitializationException`
Ocorre quando um relacionamento LAZY é acessado fora de uma transação ativa.

Soluções:
1. Adicionar `@Transactional` no método do Service
2. Usar `JOIN FETCH` na query do Repository
3. Garantir que `spring.jpa.open-in-view=false` (já configurado)

### Flyway checksum mismatch
```
FlywayException: Validate failed: Detected failed migration to version 1
```

Em desenvolvimento, se editar um script já executado:
```bash
# Apagar a tabela de controle do Flyway (só em dev!)
docker exec -it bazar-db psql -U postgres -d bazar -c "DROP TABLE flyway_schema_history;"
# Reiniciar a aplicação
```

### JWT expirado
Tokens expiram em 2h. Fazer login novamente para obter novo token.

---

## Variáveis de ambiente — referência completa

| Variável | Padrão | Obrigatória em prod | Descrição |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bazar` | Sim | URL JDBC do PostgreSQL |
| `DB_USERNAME` | `postgres` | Sim | Usuário do banco |
| `DB_PASSWORD` | `password` | Sim | Senha do banco |
| `JWT_SECRET` | `my-secret-key-change-in-production` | **Sim** | Chave HMAC256 para JWT |
| `PORT` | `8081` | Não | Porta HTTP do servidor |
| `JPA_SHOW_SQL` | `false` | Não | Loga SQL gerado pelo Hibernate |
