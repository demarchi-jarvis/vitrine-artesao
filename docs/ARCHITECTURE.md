# Arquitetura — independencia-backend

## Visão geral

Arquitetura em camadas clássica do Spring Boot com separação clara de responsabilidades. Não é hexagonal pura, mas segue o mesmo princípio de isolamento: o Controller nunca toca o banco, o Service nunca conhece HTTP.

```
┌─────────────────────────────────────────────┐
│                  HTTP Client                 │
│            (Angular / Postman / etc)         │
└──────────────────────┬──────────────────────┘
                       │ REST JSON
┌──────────────────────▼──────────────────────┐
│              Spring Security Filter          │
│          (JWT validation, auth context)      │
└──────────────────────┬──────────────────────┘
                       │ Principal injetado
┌──────────────────────▼──────────────────────┐
│                  Controllers                 │
│    /api/produtos  /api/pedidos  /api/...     │
│  Recebem Request, retornam ResponseEntity    │
└──────────────────────┬──────────────────────┘
                       │ chamadas de método
┌──────────────────────▼──────────────────────┐
│                   Services                   │
│   Lógica de negócio, validações, orquestração│
└──────────────────────┬──────────────────────┘
                       │ Spring Data
┌──────────────────────▼──────────────────────┐
│                 Repositories                 │
│          JpaRepository<Entidade, UUID>       │
└──────────────────────┬──────────────────────┘
                       │ JDBC
┌──────────────────────▼──────────────────────┐
│              PostgreSQL (bazar)              │
└─────────────────────────────────────────────┘
```

---

## Estrutura de pacotes

```
com.bazar.bazar/
├── BazarApplication.java          # @SpringBootApplication — entry point
│
├── config/
│   └── CorsConfig.java            # Configuração de CORS (origens permitidas)
│
├── security/
│   ├── SecurityConfig.java        # FilterChain, regras de autorização, BCrypt bean
│   ├── SecurityFilter.java        # OncePerRequestFilter — extrai e valida JWT
│   ├── TokenService.java          # Gera e valida JWT com HMAC256
│   └── CustomUserDetailsService   # Implementa UserDetailsService (carrega por email)
│
├── model/                         # Entidades JPA (@Entity)
│   ├── Usuario.java
│   ├── Produto.java
│   ├── Categoria.java
│   ├── Pedido.java
│   ├── ItemPedido.java
│   ├── Endereco.java
│   ├── Cliente.java               # Legado — preferir Usuario
│   ├── Vendas.java                # Auditoria
│   └── Evento.java
│
├── repositories/                  # Interfaces JpaRepository
│   ├── UsuarioRepository.java
│   ├── ProdutoRepository.java
│   ├── CategoriaRepository.java
│   ├── PedidoRepository.java
│   ├── ItemPedidoRepository.java
│   ├── EnderecoRepository.java
│   ├── ClienteRepository.java
│   └── EventoRepository.java
│
├── service/                       # Lógica de negócio
│   ├── UsuarioService.java
│   ├── ProdutoService.java
│   ├── CategoriaService.java
│   ├── PedidoService.java
│   ├── ItemPedidoService.java
│   ├── EnderecoService.java
│   └── ClienteService.java
│
├── controller/                    # Endpoints REST
│   ├── AutenticacaoController.java  # /api/autenticacao
│   ├── UsuarioController.java       # /api/usuarios
│   ├── ProdutoController.java       # /api/produtos
│   ├── CategoriaController.java     # /api/categorias
│   ├── PedidoController.java        # /api/pedidos
│   ├── ItemPedidoController.java    # /api/item
│   ├── EnderecoController.java      # /api/endereco
│   └── ClienteController.java       # /api/clientes (legado)
│
├── dto/                           # Data Transfer Objects (leitura)
├── request/                       # Payloads de entrada (escrita)
├── response/                      # Respostas paginadas e compostas
├── mapper/                        # Conversão Model → DTO
└── event/                         # Entidade de eventos
```

---

## Responsabilidades por camada

### Controller
- Extrai `@AuthenticationPrincipal` / `SecurityContextHolder` para obter o usuário logado
- Converte Request → chama Service → converte resultado em ResponseEntity
- **Não** contém lógica de negócio
- **Não** acessa Repository diretamente

### Service
- Contém toda a lógica de negócio (validação de estoque, regras de pedido, etc)
- Lança `ResponseStatusException` com HTTP status semântico
- Métodos de escrita anotados com `@Transactional`
- **Não** conhece `HttpServletRequest` ou `ResponseEntity`

### Repository
- Estende `JpaRepository<Entidade, UUID>`
- Queries derivadas (findByXxx) para casos simples
- `@Query` JPQL com `JOIN FETCH` para evitar N+1 em listas paginadas

### Model
- Entidades JPA puras com Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`)
- `@GeneratedValue(strategy = GenerationType.UUID)` em todos os IDs
- Sem lógica de negócio nas entidades (exceção: métodos helper `addItem`/`removeItem` no Pedido)

---

## Fluxo de autenticação (diagrama de sequência)

```
Client → POST /api/autenticacao/login {email, senha}
           │
           ▼
    AutenticacaoController
           │ → UsuarioService.fazerLogin()
           │       │ → UsuarioRepository.findByEmail()
           │       │ → BCrypt.matches(senha, hash)
           │       │ → TokenService.generateToken(usuario)
           │       └ → ResponseDTO {nome, token}
           └ → 200 OK {nome, token}

Client → GET /api/produtos (Authorization: Bearer <token>)
           │
           ▼
    SecurityFilter.doFilterInternal()
           │ → recoverToken(request) → "Bearer xxx" → "xxx"
           │ → TokenService.validateToken("xxx") → email
           │ → UsuarioRepository.findByEmail(email) → Usuario
           │ → SecurityContextHolder.setAuthentication(usuario)
           ▼
    ProdutoController (usuário disponível via getPrincipal())
```

---

## Decisões de design

| Decisão | Motivo |
|---|---|
| Sessions STATELESS | JWT dispensam estado no servidor; escala horizontalmente |
| BCrypt para senhas | Padrão da indústria; custo configurável |
| `Optional<ResponseDTO>` no login | Evita exceções para fluxo normal de credencial inválida |
| `ResponseStatusException` nos services | Carrega o HTTP status sem poluir a camada com try/catch |
| JOIN FETCH nas queries paginadas | Evita LazyInitializationException e N+1 |
| `ddl-auto=update` + Flyway | Dev: Hibernate cria tabelas. Prod: Flyway gerencia migrações (ver DATABASE.md) |
