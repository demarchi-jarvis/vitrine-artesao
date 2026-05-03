# Deploy — independencia-backend

## Opção 1 — Docker (recomendado para qualquer ambiente)

### Dockerfile
Criar `Dockerfile` na raiz do projeto:

```dockerfile
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q

COPY src src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/bazar-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml
Para subir app + banco juntos:

```yaml
version: '3.9'
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: bazar
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build: .
    ports:
      - "${PORT:-8081}:8081"
    environment:
      DB_URL: jdbc:postgresql://db:5432/bazar
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      PORT: 8081
      JPA_SHOW_SQL: "false"
    depends_on:
      db:
        condition: service_healthy

volumes:
  postgres_data:
```

**Rodar:**
```bash
# Criar .env com as variáveis reais
cp .env.example .env

# Build e start
docker-compose up -d --build

# Ver logs
docker-compose logs -f api

# Parar
docker-compose down
```

---

## Opção 2 — AWS EC2

### Infraestrutura mínima

```
Internet → ALB (443/80) → EC2 (8081) → RDS PostgreSQL
```

### Passos

**1. Criar RDS PostgreSQL**
```
Tipo: db.t4g.micro (free tier)
Engine: PostgreSQL 16
DB name: bazar
Username: postgres
Password: <forte, mínimo 16 chars>
VPC: mesma da EC2
Security group: permitir 5432 apenas da EC2
```

**2. Criar EC2**
```
AMI: Amazon Linux 2023
Tipo: t3.micro
Security group:
  - Inbound 8081 do ALB (ou 0.0.0.0/0 para teste)
  - Inbound 22 do seu IP
```

**3. Instalar Java 24 na EC2**
```bash
# Amazon Linux 2023
sudo dnf install -y java-24-amazon-corretto-headless
java -version
```

**4. Transferir e rodar o jar**
```bash
# Build local
./mvnw package -DskipTests

# Enviar para EC2
scp -i chave.pem target/bazar-0.0.1-SNAPSHOT.jar ec2-user@IP:/home/ec2-user/

# Na EC2 — rodar como serviço systemd
sudo nano /etc/systemd/system/bazar.service
```

**Conteúdo do bazar.service:**
```ini
[Unit]
Description=Bazar API
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user
Environment="DB_URL=jdbc:postgresql://RDS_ENDPOINT:5432/bazar"
Environment="DB_USERNAME=postgres"
Environment="DB_PASSWORD=senha_forte_aqui"
Environment="JWT_SECRET=chave_jwt_32_chars_aqui"
Environment="PORT=8081"
ExecStart=/usr/bin/java -jar /home/ec2-user/bazar-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable bazar
sudo systemctl start bazar
sudo systemctl status bazar
```

---

## Opção 3 — AWS Elastic Beanstalk (mais simples)

```bash
# Instalar EB CLI
pip install awsebcli

# Inicializar
eb init independencia-backend --platform java --region us-east-1

# Criar ambiente
eb create producao

# Configurar variáveis de ambiente
eb setenv \
  DB_URL=jdbc:postgresql://RDS_ENDPOINT:5432/bazar \
  DB_USERNAME=postgres \
  DB_PASSWORD=senha \
  JWT_SECRET=chave

# Deploy
eb deploy

# Ver logs
eb logs
```

---

## Variáveis de ambiente — produção

| Variável | Exemplo | Observação |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://mydb.xxx.rds.amazonaws.com:5432/bazar` | URL do RDS |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `S3nh@F0rte!2025` | Mínimo 12 chars, especiais |
| `JWT_SECRET` | `a8f3e2b1c9d4...` (32+ chars) | `openssl rand -hex 32` |
| `PORT` | `8081` | Ou 80 se sem reverse proxy |
| `JPA_SHOW_SQL` | `false` | Nunca true em produção |

---

## CORS em produção

Atualizar `CorsConfig.java` com o domínio real do frontend:

```java
configuration.setAllowedOrigins(Arrays.asList(
    "https://vitrine.vassouras-tec.com.br",
    "https://www.vitrine.vassouras-tec.com.br"
));
```

---

## Checklist de go-live

- [ ] `JWT_SECRET` com valor forte e aleatório (`openssl rand -hex 32`)
- [ ] `DB_PASSWORD` forte, sem caracteres especiais problemáticos em URL
- [ ] `JPA_SHOW_SQL=false`
- [ ] `ddl-auto=none` (ou `validate`) em produção + Flyway com scripts
- [ ] CORS com domínio real do frontend (não `localhost`)
- [ ] HTTPS configurado no ALB ou Nginx
- [ ] Security group do RDS fechado para internet (acesso só da EC2)
- [ ] Backup automático do RDS habilitado
- [ ] `.env` no `.gitignore` e nunca commitado
- [ ] Logs centralizados (CloudWatch, Datadog, etc)
- [ ] Monitoramento de uptime (AWS CloudWatch Alarms ou UptimeRobot)

---

## Flyway em produção

Antes do primeiro deploy em produção com Flyway:

1. Criar `src/main/resources/db/migration/V1__create_tables.sql` (ver [DATABASE.md](DATABASE.md))
2. Ajustar `application.properties`:
   ```properties
   spring.jpa.hibernate.ddl-auto=none
   spring.flyway.enabled=true
   spring.flyway.locations=classpath:db/migration
   ```
3. Para banco já existente (criado pelo Hibernate em dev), usar Flyway baseline:
   ```properties
   spring.flyway.baseline-on-migrate=true
   spring.flyway.baseline-version=0
   ```
