.PHONY: help dev dev-build test test-db-up test-db-down logs status down clean deploy

# Mostra este menu
help:
	@echo ""
	@echo "  Vitrine Virtual — comandos disponíveis"
	@echo ""
	@echo "  LOCAL"
	@echo "  make dev          Inicia stack completa (build + postgres + app)"
	@echo "  make dev-build    Reconstrói e reinicia somente o container app"
	@echo "  make logs         Acompanha logs do app em tempo real"
	@echo "  make status       Lista containers em execução"
	@echo "  make down         Para todos os containers"
	@echo "  make clean        Para e remove volumes (apaga dados)"
	@echo ""
	@echo "  TESTES"
	@echo "  make test         Inicia test-db e roda toda a suite Maven"
	@echo "  make test-db-up   Inicia apenas o banco de testes (porta 5433)"
	@echo "  make test-db-down Para o banco de testes"
	@echo ""
	@echo "  AWS"
	@echo "  make deploy       Executa terraform apply (requer credenciais AWS)"
	@echo "  make destroy      Executa terraform destroy"
	@echo ""

# ---------------------------------------------------------------------------
# LOCAL
# ---------------------------------------------------------------------------

dev: .env
	docker compose up -d --build
	@echo ""
	@echo "  API:    http://localhost:8081"
	@echo "  Health: http://localhost:8081/actuator/health"
	@echo ""
	@echo "  Acompanhe os logs com: make logs"

dev-build:
	docker compose build app
	docker compose up -d --no-deps app

logs:
	docker compose logs -f app

status:
	docker compose ps

down:
	docker compose --profile test down

clean:
	docker compose --profile test down -v
	@echo "Volumes removidos."

# Garante que .env existe antes de subir
.env:
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo ""; \
		echo "  .env criado a partir de .env.example."; \
		echo "  ATENÇÃO: edite .env e defina DB_PASSWORD e JWT_SECRET antes de continuar."; \
		echo ""; \
		exit 1; \
	fi

# ---------------------------------------------------------------------------
# TESTES
# ---------------------------------------------------------------------------

test: test-db-up
	./mvnw test
	@echo ""
	@echo "  Suite finalizada. Banco de testes continua rodando (make test-db-down para parar)."

test-db-up:
	docker compose --profile test up -d test-db
	@echo "Aguardando banco de testes ficar pronto..."
	@docker compose --profile test run --rm \
		-e PGPASSWORD=bazar_test \
		test-db \
		sh -c 'until pg_isready -h test-db -U bazar_test; do sleep 1; done' 2>/dev/null || \
		sleep 5
	@echo "Banco de testes pronto (localhost:5433)."

test-db-down:
	docker compose --profile test stop test-db
	docker compose --profile test rm -f test-db

# ---------------------------------------------------------------------------
# AWS ACADEMY
# ---------------------------------------------------------------------------

deploy:
	@echo "Verificando credenciais AWS..."
	@aws sts get-caller-identity --query Account --output text 2>/dev/null || \
		(echo "ERRO: credenciais AWS não configuradas. Cole as credenciais do Lab primeiro." && exit 1)
	cd infra/terraform && terraform init -upgrade && terraform apply

destroy:
	@echo "ATENÇÃO: isto vai destruir todos os recursos AWS criados pelo Terraform."
	@read -p "Confirmar? (yes/no): " c; [ "$$c" = "yes" ] || exit 1
	cd infra/terraform && terraform destroy
