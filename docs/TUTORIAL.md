# Tutorial — Vitrine Virtual (Backend)

Guia completo para rodar o projeto localmente em qualquer máquina e publicar na AWS Academy com um único comando Terraform.

---

## Índice

1. [Requisitos](#1-requisitos)
2. [Clonar o projeto](#2-clonar-o-projeto)
3. [Rodar localmente com Docker (recomendado)](#3-rodar-localmente-com-docker-recomendado)
4. [Rodar localmente sem Docker](#4-rodar-localmente-sem-docker)
5. [Rodar os testes de integração](#5-rodar-os-testes-de-integração)
6. [Testando a API](#6-testando-a-api)
7. [Publicar na AWS Academy via Terraform](#7-publicar-na-aws-academy-via-terraform)
8. [Atualizar o deploy sem recriar a infraestrutura](#8-atualizar-o-deploy-sem-recriar-a-infraestrutura)
9. [Adicionar frontend React ou Angular](#9-adicionar-frontend-react-ou-angular)
10. [Problemas comuns](#10-problemas-comuns)

---

## 1. Requisitos

### Para rodar localmente com Docker

| Ferramenta | Versão mínima | Como verificar | Como instalar |
|---|---|---|---|
| Git | qualquer | `git --version` | https://git-scm.com |
| Docker Desktop | 24+ | `docker --version` | https://www.docker.com/products/docker-desktop |

> **Windows:** instale o Docker Desktop e certifique-se de que ele está rodando (ícone na bandeja do sistema).  
> **macOS:** mesma coisa — Docker Desktop deve estar aberto.  
> **Linux:** instale `docker-ce` + `docker-compose-plugin` e inicie o serviço com `sudo systemctl start docker`.

---

### Para rodar localmente sem Docker (opcional)

| Ferramenta | Versão | Como verificar |
|---|---|---|
| Java JDK | **21 (LTS)** | `java -version` |
| PostgreSQL | 15+ | `psql --version` |

> **Java 21 no Ubuntu/Debian:**
> ```bash
> sudo apt-get update && sudo apt-get install -y openjdk-21-jdk
> export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
> ```
>
> **Java 21 no macOS (Homebrew):**
> ```bash
> brew install openjdk@21
> ```
>
> **Java 21 no Windows:** baixe o instalador em https://adoptium.net

---

### Para publicar na AWS Academy

| Ferramenta | Versão | Como verificar | Como instalar |
|---|---|---|---|
| Terraform | 1.3+ | `terraform --version` | https://developer.hashicorp.com/terraform/downloads |
| AWS CLI | v2 | `aws --version` | https://aws.amazon.com/cli |

> **Terraform no Ubuntu:**
> ```bash
> sudo apt-get install -y gnupg software-properties-common
> curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
> echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
> sudo apt-get update && sudo apt-get install terraform
> ```
>
> **AWS CLI v2 no Ubuntu:**
> ```bash
> curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
> unzip awscliv2.zip && sudo ./aws/install
> ```

---

## 2. Clonar o projeto

```bash
git clone https://github.com/demarchi-jarvis/vitrine-artesao.git
cd vitrine-artesao
```

> **Nota:** o repositório principal é `vitrine-artesao`. Se você tiver o clone antigo como `independencia-backend`, ambos são o mesmo código — use qualquer um.

---

## 3. Rodar localmente com Docker (recomendado)

Este é o método mais simples. Não precisa instalar Java nem PostgreSQL — só Docker.

### Passo 1 — Configurar variáveis de ambiente

```bash
cp .env.example .env
```

Abra o arquivo `.env` e preencha os dois valores obrigatórios:

```bash
# Gerar uma senha segura para o banco:
openssl rand -hex 12
# Exemplo de resultado: a3f8c2d1e9b4f7a2c5d8

# Gerar o segredo JWT (mínimo 32 caracteres):
openssl rand -hex 32
# Exemplo: 8f3a2c1d9e4b7f6a5c8d2e1f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2
```

Seu `.env` deve ficar assim:

```env
DB_USERNAME=bazar_user
DB_PASSWORD=a3f8c2d1e9b4f7a2c5d8
JWT_SECRET=8f3a2c1d9e4b7f6a5c8d2e1f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2
FRONTEND_URL=
```

> **Atenção:** nunca commite o arquivo `.env` — ele está no `.gitignore` por segurança.

---

### Passo 2 — Iniciar a aplicação

**Opção A — script automático (mais simples):**
```bash
./start.sh
```

**Opção B — Makefile:**
```bash
make dev
```

**Opção C — Docker Compose direto:**
```bash
docker compose up -d --build
```

Na primeira execução, o Docker vai:
1. Baixar a imagem do PostgreSQL (~150 MB)
2. Compilar o projeto Java com Maven (~5–10 min dependendo da internet)
3. Criar a imagem da aplicação
4. Subir os dois containers

As próximas execuções são muito mais rápidas (dependências em cache).

---

### Passo 3 — Verificar se está funcionando

```bash
# Verificar se os containers estão rodando:
docker compose ps

# Deve mostrar:
# vitrine-db   Up (healthy)
# vitrine-app  Up (healthy)
```

```bash
# Testar o health check da API (aguarde ~60s após o start):
curl http://localhost:8081/actuator/health
# Resposta esperada: {"status":"UP"}
```

Se o health check não responder após 2 minutos, veja os logs:
```bash
docker compose logs -f app
```

---

### Parar a aplicação

```bash
docker compose down          # para os containers (mantém os dados do banco)
docker compose down -v       # para E apaga o volume do banco (começa do zero)
```

---

## 4. Rodar localmente sem Docker

Use esta opção se quiser iterar rápido com hot-reload ou depurar na IDE.

### Passo 1 — Subir apenas o PostgreSQL via Docker

```bash
docker run -d \
  --name vitrine-db-dev \
  -e POSTGRES_DB=bazar \
  -e POSTGRES_USER=bazar_user \
  -e POSTGRES_PASSWORD=senha_dev_123 \
  -p 5432:5432 \
  postgres:15-alpine
```

Aguarde o banco ficar pronto:
```bash
docker logs vitrine-db-dev 2>&1 | grep "ready to accept"
# Deve aparecer: database system is ready to accept connections
```

---

### Passo 2 — Configurar variáveis e rodar o app

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bazar
export DB_USERNAME=bazar_user
export DB_PASSWORD=senha_dev_123
export JWT_SECRET=dev-secret-key-only-for-local-deve-ter-32-chars
export PORT=8081

./mvnw spring-boot:run
```

> **IntelliJ IDEA:** em Run → Edit Configurations → Environment variables, adicione as mesmas variáveis acima.

A aplicação vai inicializar o Flyway automaticamente (cria as tabelas na primeira vez).

---

## 5. Rodar os testes de integração

Os testes exigem um banco PostgreSQL separado na porta `5433` com as credenciais de teste.

### Passo 1 — Iniciar o banco de testes

```bash
# Via Makefile (recomendado):
make test-db-up

# Ou manualmente:
docker compose --profile test up -d test-db
```

Isso sobe o `vitrine-test-db` na porta `5433` com usuário/senha `bazar_test`.

---

### Passo 2 — Rodar os testes

```bash
# Via Makefile:
make test

# Ou diretamente com Maven:
./mvnw test
```

Resultado esperado:
```
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Cobertura dos testes:**
| Módulo | Testes | O que valida |
|---|---|---|
| Autenticação | 8 | Registro, login, token inválido, email duplicado, senha não exposta |
| Produtos | 12 | CRUD, autorização por autor, filtros, busca, senha não exposta |
| Pedidos | 9 | Estoque, autorização, cascata na exclusão |
| Usuários | 8 | Perfil, loja ativa/inativa, filtragem, senha não exposta |

---

### Parar o banco de testes

```bash
make test-db-down
# ou
docker compose --profile test stop test-db
```

---

## 6. Testando a API

A API fica disponível em `http://localhost:8081`. Todos os endpoints (exceto login e registro) exigem um token JWT no header `Authorization: Bearer <token>`.

### Fluxo básico de teste

**1. Registrar um usuário:**
```bash
curl -s -X POST http://localhost:8081/api/autenticacao/registrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Artesã","email":"maria@vitrine.com","senha":"senha123"}' | python3 -m json.tool
```

Resposta:
```json
{
  "nome": "Maria Artesã",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**2. Guardar o token:**
```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

**3. Ver dados do usuário logado:**
```bash
curl -s http://localhost:8081/api/usuarios/logado \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

**4. Ativar a loja (para vender produtos):**
```bash
curl -s -X PATCH http://localhost:8081/api/usuarios/loja/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":true}'
```

**5. Criar uma categoria:**
```bash
curl -s -X POST http://localhost:8081/api/categorias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Cestaria","descricao":"Produtos de palha e bambu","icone":"cestaria.png"}' | python3 -m json.tool
# Guardar o "id" da resposta como CATEGORIA_ID
```

**6. Criar um produto:**
```bash
curl -s -X POST http://localhost:8081/api/produtos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"nome\": \"Cesto Artesanal\",
    \"preco\": 89.90,
    \"quantidade\": 10,
    \"descricao\": \"Feito à mão com bambu nativo\",
    \"imagem\": \"cesto.jpg\",
    \"icone\": \"cesto-icone.jpg\",
    \"categoriaId\": \"$CATEGORIA_ID\"
  }" | python3 -m json.tool
```

**7. Listar produtos (sem token):**
```bash
curl -s "http://localhost:8081/api/produtos/filtro" | python3 -m json.tool
```

> **Documentação completa dos endpoints:** ver [API.md](API.md)

---

## 7. Publicar na AWS Academy via Terraform

O Terraform cria toda a infraestrutura automaticamente: Security Group, instância EC2 e bootstrap da aplicação via Docker Compose — em um único `terraform apply`.

### Pré-requisito único: conta AWS Academy ativa

Você precisa ter um laboratório do AWS Academy iniciado (status **In Progress** no painel).

---

### Passo 1 — Obter as credenciais temporárias do Lab

1. Acesse o painel do **AWS Academy** → seu Lab → clique em **AWS Details**
2. Clique em **AWS CLI** — você verá algo como:

```
[default]
aws_access_key_id=ASIA...
aws_secret_access_key=wJalr...
aws_session_token=AQoXny...MUITO_LONGO...
```

3. Copie **os três valores** — você vai precisar deles.

> **Importante:** as credenciais do Academy expiram em **4 horas**. Se o `terraform apply` falhar com erro de autenticação, volte aqui e renove as credenciais.

---

### Passo 2 — Baixar a chave SSH (vockey.pem)

1. No mesmo painel **AWS Academy** → **AWS Details** → clique em **Download PEM**
2. Salve o arquivo como `vockey.pem` na sua pasta home (`~/vockey.pem`)
3. Ajuste as permissões:

```bash
chmod 400 ~/vockey.pem
```

> Sem essa chave você não conseguirá acessar a instância EC2 via SSH.

---

### Passo 3 — Configurar as credenciais AWS no terminal

**Opção A — variáveis de ambiente (mais simples, funciona no Windows/Mac/Linux):**

Cole exatamente o que o painel do Academy mostrou:
```bash
export AWS_ACCESS_KEY_ID="ASIA..."
export AWS_SECRET_ACCESS_KEY="wJalr..."
export AWS_SESSION_TOKEN="AQoXny..."
export AWS_DEFAULT_REGION="us-east-1"
```

> No **Windows (PowerShell):**
> ```powershell
> $env:AWS_ACCESS_KEY_ID="ASIA..."
> $env:AWS_SECRET_ACCESS_KEY="wJalr..."
> $env:AWS_SESSION_TOKEN="AQoXny..."
> $env:AWS_DEFAULT_REGION="us-east-1"
> ```

**Opção B — arquivo de credenciais (persiste no terminal):**
```bash
mkdir -p ~/.aws
cat > ~/.aws/credentials << 'EOF'
[default]
aws_access_key_id=ASIA...
aws_secret_access_key=wJalr...
aws_session_token=AQoXny...
EOF

echo '[default]
region=us-east-1' > ~/.aws/config
```

---

### Passo 4 — Verificar se as credenciais funcionam

```bash
aws sts get-caller-identity
```

Resposta esperada (os números variam):
```json
{
    "UserId": "AROA...:user@cloudshell",
    "Account": "123456789012",
    "Arn": "arn:aws:sts::123456789012:assumed-role/..."
}
```

Se der erro, revise se copiou os três valores corretamente (access key, secret key **e** session token).

---

### Passo 5 — Configurar as variáveis do Terraform

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
```

Abra `terraform.tfvars` e preencha:

```hcl
# Região AWS — Academy sempre us-east-1 (não altere)
aws_region = "us-east-1"

# Chave SSH do Academy (não altere se baixou o arquivo como vockey.pem)
key_name = "vockey"

# Tipo de instância
instance_type = "t3.small"

# Banco de dados
db_username = "bazar_user"
db_password = "MinhaS3nhaF0rte2025!"   # mínimo 12 chars

# Segredo JWT — gere com: openssl rand -hex 32
jwt_secret = "cole_aqui_o_resultado_de_openssl_rand_hex_32"

# URL do frontend (deixe vazio por enquanto)
frontend_url = ""
```

**Gerar os valores seguros:**
```bash
# Senha do banco:
openssl rand -hex 12
# → ex: a3f8c2d1e9b4f7a2

# Segredo JWT:
openssl rand -hex 32
# → ex: 8f3a2c1d9e4b7f6a5c8d2e1f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2
```

> **Atenção:** `terraform.tfvars` está no `.gitignore` — nunca será commitado. É seguro guardar suas senhas aqui.

---

### Passo 6 — Inicializar o Terraform

Execute **uma única vez** (baixa os plugins da AWS):

```bash
cd infra/terraform
terraform init
```

Resultado esperado:
```
Terraform has been successfully initialized!
```

---

### Passo 7 — Executar o deploy

```bash
terraform apply
```

O Terraform vai mostrar um plano com tudo que será criado:
```
Plan: 2 to add, 0 to change, 0 to destroy.
  + aws_security_group.vitrine  (abre as portas 22, 80, 8081, 3000)
  + aws_instance.vitrine        (EC2 t3.small, Amazon Linux 2023)
```

Digite `yes` para confirmar.

Após ~30 segundos, o Terraform termina e exibe:

```
Outputs:

api_url      = "http://54.X.X.X:8081"
health_url   = "http://54.X.X.X:8081/actuator/health"
public_ip    = "54.X.X.X"
ssh_command  = "ssh -i ~/vockey.pem ec2-user@54.X.X.X"
deploy_log   = "ssh -i ~/vockey.pem ec2-user@54.X.X.X 'tail -f /var/log/userdata.log'"
```

---

### Passo 8 — Aguardar o bootstrap completar

A instância EC2 ainda está executando o script de inicialização (instalar Docker, clonar o projeto, buildar a imagem Java). Isso leva **5 a 10 minutos**.

**Acompanhar em tempo real (copie o comando `deploy_log` da saída acima):**
```bash
ssh -i ~/vockey.pem ec2-user@54.X.X.X 'tail -f /var/log/userdata.log'
```

O log termina com:
```
======================================================
  Deploy concluído!
  API:    http://54.X.X.X:8081
  Health: http://54.X.X.X:8081/actuator/health
======================================================
```

**Verificar que a API está respondendo:**
```bash
curl http://54.X.X.X:8081/actuator/health
# {"status":"UP"}
```

---

### Passo 9 — Acessar via SSH (opcional)

```bash
ssh -i ~/vockey.pem ec2-user@54.X.X.X

# Dentro da instância, ver logs da aplicação:
cd /opt/vitrine-artesao
docker compose logs -f app

# Ver status dos containers:
docker compose ps
```

---

### Destruir a infraestrutura

Para evitar cobranças ou quando o Lab encerrar:

```bash
cd infra/terraform
terraform destroy
```

> No Academy, os recursos são destruídos automaticamente quando o Lab encerra. O `terraform destroy` é útil quando você quer recriar do zero dentro do mesmo Lab.

---

## 8. Atualizar o deploy sem recriar a infraestrutura

Quando você fizer mudanças no código e quiser atualizar a instância sem destruir e recriar tudo:

```bash
# 1. Na sua máquina — commitar e dar push
git add .
git commit -m "feat: minha nova feature"
git push origin main

# 2. Na instância EC2 (via SSH)
ssh -i ~/vockey.pem ec2-user@54.X.X.X

# 3. Dentro da EC2 — rodar o script de deploy incremental
cd /opt/vitrine-artesao
./infra/deploy.sh
```

O `deploy.sh` faz `git pull`, rebuild apenas do container `app`, e verifica o health check automaticamente.

> **Alternativa:** se você mudou configurações de infraestrutura (Security Group, tipo de instância, variáveis de ambiente), rode `terraform apply` novamente — ele atualiza apenas o que mudou.

---

## 9. Adicionar frontend React ou Angular

O `docker-compose.yml` já tem um bloco comentado reservado para o frontend. Quando o frontend estiver pronto:

**1. Na instância EC2, clonar o repositório do frontend:**
```bash
cd /opt/vitrine-artesao
git clone https://github.com/demarchi-jarvis/vitrine-frontend.git frontend
```

**2. Descomentar o serviço `frontend` no `docker-compose.yml`:**
```yaml
frontend:
  build: ./frontend
  ports:
    - "3000:3000"
  environment:
    - REACT_APP_API_URL=http://localhost:8081
    # ou para Angular:
    # - NG_API_URL=http://localhost:8081
  networks:
    - vitrine-net
```

**3. Subir o frontend:**
```bash
docker compose up -d --build frontend
```

O frontend ficará disponível em `http://54.X.X.X:3000`.

**4. Atualizar o CORS do backend:**

No `terraform.tfvars`, atualizar `frontend_url`:
```hcl
frontend_url = "http://54.X.X.X:3000"
```

E rodar `terraform apply` para propagar a variável de ambiente na EC2.

---

## 10. Problemas comuns

### "DB_PASSWORD is required" ao rodar docker compose

Você não criou o arquivo `.env` ou ele está vazio.
```bash
cp .env.example .env
# Editar .env e preencher DB_PASSWORD e JWT_SECRET
```

---

### Porta 8081 já em uso

```bash
# Verificar o que está usando a porta:
lsof -i :8081          # Linux/macOS
netstat -ano | findstr 8081   # Windows

# Parar os containers do projeto:
docker compose down
```

---

### "No space left on device" ao buildar

O Docker está sem espaço em disco. Limpar imagens antigas:
```bash
docker system prune -f
docker compose build app
```

---

### LazyInitializationException nos logs

Já corrigido — todos os relacionamentos críticos estão com `FetchType.EAGER`. Se aparecer novamente, verifique se adicionou um novo relacionamento sem configurar o fetch type.

---

### Credenciais AWS expiradas (erro 401 ou "ExpiredToken")

As credenciais do Academy duram **4 horas**. Renove:

1. Volte ao painel do Academy → **AWS Details** → **AWS CLI**
2. Copie os novos valores e exporte novamente:
```bash
export AWS_ACCESS_KEY_ID="novo_valor"
export AWS_SECRET_ACCESS_KEY="novo_valor"
export AWS_SESSION_TOKEN="novo_valor"
```
3. Rode `terraform apply` novamente (ele reusa o estado existente).

---

### terraform apply falha com "InvalidClientTokenId"

Você esqueceu de exportar o `AWS_SESSION_TOKEN`. No Academy, as credenciais são temporárias e **sempre incluem um session token** — sem ele, as chamadas falham.

```bash
# Confirmar que o session token está na env:
echo $AWS_SESSION_TOKEN
# Deve imprimir uma string longa (começa com AQo...)
```

---

### IP da EC2 mudou após reiniciar

No AWS Academy, instâncias EC2 recebem um **IP público novo a cada reinício**. Para descobrir o IP atual:

```bash
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=vitrine-artesao" \
  --query "Reservations[0].Instances[0].PublicIpAddress" \
  --output text
```

Ou atualize o estado do Terraform:
```bash
cd infra/terraform
terraform refresh
terraform output public_ip
```

---

### A API respondeu mas os dados sumiram

O banco usa um volume Docker (`postgres_data`) persistente na EC2. Se a instância foi **terminada** (não apenas parada), o volume é destruído.

Para prevenir perda de dados em produção real, use um banco externo (AWS RDS). Para o MVP da incubadora, o volume Docker é suficiente enquanto a instância estiver rodando.

---

### Testes falham com "Connection refused" (porta 5433)

O banco de testes não está rodando.
```bash
make test-db-up
# ou
docker compose --profile test up -d test-db
```

---

## Referências rápidas

| Comando | O que faz |
|---|---|
| `./start.sh` | Inicia a stack completa localmente |
| `make dev` | Idem (com Makefile) |
| `make test` | Sobe test-db e roda toda a suite de testes |
| `make logs` | Acompanha logs do app em tempo real |
| `make down` | Para todos os containers |
| `make clean` | Para e apaga volumes (reset total) |
| `make deploy` | Executa terraform apply |
| `terraform output` | Exibe IP e URLs após o deploy |
| `terraform destroy` | Remove toda a infraestrutura AWS |

---

*Projeto Vitrine Virtual — Incubadora Vassouras Tec / UniVassouras*  
*Desenvolvido por Antonio Demarchi*
