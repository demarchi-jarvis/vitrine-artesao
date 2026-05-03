# Segurança — independencia-backend

## Visão geral

Autenticação stateless com JWT. Cada request carrega o token no header. O servidor valida o token, carrega o usuário do banco e disponibiliza via `SecurityContextHolder`.

---

## Fluxo completo

```
1. POST /api/autenticacao/login {email, senha}
        │
        ▼
   UsuarioService.fazerLogin()
        │ → findByEmail(email) → Usuario
        │ → BCrypt.matches(senha, usuario.senha)
        │ → TokenService.generateToken(usuario)
        │       → JWT.create()
        │           .withSubject(email)
        │           .withExpiresAt(now + 2h)
        │           .sign(HMAC256(JWT_SECRET))
        └ → ResponseDTO { nome, token }

2. GET /api/qualquer-coisa
   Header: Authorization: Bearer eyJhbGci...
        │
        ▼
   SecurityFilter (OncePerRequestFilter)
        │ → recoverToken(request) → extrai header, remove "Bearer "
        │ → TokenService.validateToken(token)
        │       → JWT.require(HMAC256).verify(token).getSubject() → email
        │ → UsuarioRepository.findByEmail(email) → Usuario
        │ → UsernamePasswordAuthenticationToken(usuario, null, [ROLE_USER])
        └ → SecurityContextHolder.setAuthentication(...)

3. Controller
        │ → SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        │    ou @AuthenticationPrincipal Usuario usuarioLogado
        └ → acesso ao objeto Usuario completo
```

---

## JWT

**Biblioteca:** `com.auth0:java-jwt:4.4.0`

| Campo | Valor |
|---|---|
| Issuer | `login-auth-api` |
| Subject | email do usuário |
| Expiração | 2 horas a partir da criação |
| Algoritmo | HMAC256 |
| Secret | Variável de ambiente `JWT_SECRET` |

**Geração do token** (`TokenService.generateToken`):
```java
JWT.create()
   .withIssuer("login-auth-api")
   .withSubject(usuario.getEmail())
   .withExpiresAt(LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00")))
   .sign(Algorithm.HMAC256(secret))
```

**Validação** (`TokenService.validateToken`):
- Retorna o email (subject) se o token for válido
- Retorna `null` se inválido ou expirado (captura `JWTVerificationException`)
- O `SecurityFilter` ignora o token se o retorno for null (request segue sem autenticação)

---

## Hashing de senhas

**Algoritmo:** BCrypt via `BCryptPasswordEncoder`

- Registrar: `passwordEncoder.encode(requestDTO.senha())`
- Login: `passwordEncoder.matches(senhaPlana, hashSalvo)`
- O bean `PasswordEncoder` é declarado em `SecurityConfig` e injetado via DI

---

## Filtros e configuração

### SecurityFilter (OncePerRequestFilter)
Executa antes de `UsernamePasswordAuthenticationFilter` para todos os requests.

```
Extração do token:
  request.getHeader("Authorization")  → "Bearer eyJhbGci..."
  authHeader.replace("Bearer ", "")   → "eyJhbGci..."
```

Se o token for null (request sem Authorization header), o filtro passa adiante sem autenticar. O Spring Security depois decide se o endpoint precisa de autenticação.

### SecurityConfig — regras de autorização

```
POST /api/autenticacao/login     → PÚBLICO
POST /api/autenticacao/registrar → PÚBLICO
GET  /api/produtos/filtro/**     → PÚBLICO (vitrine sem login)
Qualquer outro                   → AUTENTICADO
```

Sessions: `STATELESS` — sem `HttpSession`, sem cookies de sessão.

---

## CORS

Configurado em `CorsConfig.java`.

**Origens permitidas:**
- `http://localhost:4200` (Angular em desenvolvimento)
- `http://your-angular-domain.com` (placeholder — substituir pelo domínio real em produção)

**Métodos:** GET, POST, PUT, PATCH, DELETE, OPTIONS

**Headers:** `*`

**Credentials:** `true` (permite cookies se necessário)

**Como atualizar para produção:**
```java
// CorsConfig.java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:4200",
    "https://vitrine.vassouras-tec.com.br"  // domínio real
));
```

---

## Roles

Atualmente existe apenas uma role: `ROLE_USER`.

```java
var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
```

Não há distinção de permissão por role no sistema atual. A lógica de "apenas artesão pode criar produto" é verificada via campo `loja` na entidade `Usuario`, dentro do Service/Controller.

---

## Autorização granular (por recurso)

Verificações que vão além do JWT:

| Endpoint | Verificação |
|---|---|
| `PATCH /api/produtos/{id}` | `produto.autor.id == usuarioLogado.id` → 403 se falso |
| `PUT /api/endereco` | Endereço buscado pelo `usuarioLogado.id` — impossível alterar endereço alheio |
| `GET /api/usuarios/logado` | Dados sempre do usuário no token |
| `PATCH /api/usuarios/alterar` | Sempre atualiza o usuário do token |
| `GET /api/item/comprador` | Query filtrada por `usuarioLogado.id` |
| `GET /api/item/vendedor` | Query filtrada por `usuarioLogado.id` |

---

## Variáveis de ambiente críticas

| Variável | Uso | Risco se vazar |
|---|---|---|
| `JWT_SECRET` | Assina e valida todos os tokens | Qualquer um pode gerar tokens válidos |
| `DB_PASSWORD` | Acesso ao banco | Acesso direto a todos os dados |

**Em produção:**
- `JWT_SECRET` deve ter no mínimo 32 caracteres aleatórios
- Gerar com: `openssl rand -hex 32`
- Nunca versionar o valor real (use `.env` + `.gitignore`)

---

## Vulnerabilidades conhecidas e mitigadas

| Vulnerabilidade | Mitigação |
|---|---|
| Força bruta no login | Retorna 401 genérico (não distingue usuário inválido de senha errada) |
| CSRF | Desabilitado (API stateless sem cookies de sessão) |
| SQL Injection | Spring Data JPA com queries parametrizadas |
| Exposição de senha | BCrypt + senha nunca retornada nos endpoints |
| Mass assignment | DTOs e Requests separados das entidades; email não pode ser alterado via PATCH |
| Troca de dono do endereço | Campo `usuario` ignorado no body do PUT /api/endereco |
