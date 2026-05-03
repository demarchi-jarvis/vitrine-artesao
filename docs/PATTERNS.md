# Padrões de Código — independencia-backend

Este documento define como adicionar novas features mantendo consistência com o código existente.

---

## Padrão de uma feature completa

Para adicionar uma nova entidade/recurso (ex: `Avaliacao`), seguir exatamente esta ordem:

### 1. Model (Entidade JPA)
```java
@Entity
@Table(name = "avaliacao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // SEMPRE UUID, nunca AUTO
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false)
    private int nota;  // 1 a 5

    private String comentario;
    private LocalDateTime dataCriacao;
}
```

**Regras do Model:**
- `@GeneratedValue(strategy = GenerationType.UUID)` sempre
- FetchType.LAZY para relacionamentos ManyToOne (evita N+1)
- Apenas FetchType.EAGER para dados sempre necessários (raramente)
- Sem lógica de negócio na entidade

---

### 2. Repository
```java
@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    // Query derivada — para casos simples
    List<Avaliacao> findByProdutoId(UUID produtoId);
    Page<Avaliacao> findByAutorId(UUID autorId, Pageable pageable);

    // @Query JPQL com JOIN FETCH — quando precisa de dados relacionados em lista
    @Query(value = "SELECT a FROM Avaliacao a " +
                   "JOIN FETCH a.produto p " +
                   "JOIN FETCH a.autor u " +
                   "WHERE a.produto.id = :produtoId",
           countQuery = "SELECT COUNT(a) FROM Avaliacao a WHERE a.produto.id = :produtoId")
    Page<Avaliacao> buscarPorProdutoComDetalhes(@Param("produtoId") UUID produtoId, Pageable pageable);
}
```

**Regras do Repository:**
- Sempre extende `JpaRepository<Entidade, UUID>`
- Queries derivadas para buscas simples (Spring gera o SQL)
- `@Query` JPQL com `JOIN FETCH` quando a query envolve navegação em lista paginada
- `countQuery` obrigatório junto com `value` quando usar JOIN FETCH + paginação (evita erro de contagem)
- `@Param` em todos os parâmetros da `@Query`

---

### 3. Request (payload de entrada)
```java
// Para requests com muitos campos — usar classe
public class AvaliacaoRequest {
    private UUID produtoId;
    private int nota;
    private String comentario;

    // getters/setters via Lombok ou explícitos
}

// Para requests simples — usar record
public record AvaliacaoSimpleRequest(UUID produtoId, int nota) {}
```

**Regras do Request:**
- Campos que o cliente envia para criar/atualizar
- Nunca expor o ID do autor no request (vem do token JWT)
- Separado do Model para evitar mass assignment

---

### 4. DTO (resposta de leitura)
```java
public class AvaliacaoDTO {
    private UUID id;
    private ProdutoItemDTO produto;   // DTO reduzido do produto
    private UsuarioDTO autor;          // DTO do usuário
    private int nota;
    private String comentario;
    private LocalDateTime dataCriacao;

    // construtor a partir do Model
    public AvaliacaoDTO(Avaliacao a) {
        this.id = a.getId();
        this.nota = a.getNota();
        this.comentario = a.getComentario();
        this.dataCriacao = a.getDataCriacao();
        this.autor = new UsuarioDTO(a.getAutor());
        // ...
    }
}
```

**Regras do DTO:**
- Nunca retornar o Model diretamente quando tem relacionamentos (evita serialização infinita)
- Usar DTOs reduzidos para entidades aninhadas (ex: `ProdutoItemDTO` em vez de `ProdutoDTO` completo)
- Construtor que recebe o Model facilita conversão no Service

---

### 5. Service
```java
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final ProdutoRepository produtoRepository;

    @Autowired  // ou construtor com @RequiredArgsConstructor
    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                            ProdutoRepository produtoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Avaliacao criar(AvaliacaoRequest request, Usuario autor) {
        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Produto não encontrado: " + request.getProdutoId()));

        if (request.getNota() < 1 || request.getNota() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nota deve ser entre 1 e 5.");
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setProduto(produto);
        avaliacao.setAutor(autor);
        avaliacao.setNota(request.getNota());
        avaliacao.setComentario(request.getComentario());
        avaliacao.setDataCriacao(LocalDateTime.now());

        return avaliacaoRepository.save(avaliacao);
    }

    public Page<Avaliacao> listarPorProduto(UUID produtoId, Pageable pageable) {
        return avaliacaoRepository.findByProdutoId(produtoId, pageable);
    }
}
```

**Regras do Service:**
- `@Transactional` em todos os métodos de escrita (create, update, delete)
- Lança `ResponseStatusException` com status HTTP semântico (nunca RuntimeException genérica)
- Recebe `Usuario autor` como parâmetro — nunca acessa `SecurityContextHolder` no Service (isso é responsabilidade do Controller)
- Exceção: `ItemPedidoService` acessa o contexto porque é uma query de dados do usuário logado — padrão aceitável neste caso

---

### 6. Controller
```java
@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @Autowired
    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoDTO> criar(
            @RequestBody AvaliacaoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        Avaliacao avaliacao = avaliacaoService.criar(request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AvaliacaoDTO(avaliacao));
    }

    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<Page<AvaliacaoDTO>> listarPorProduto(
            @PathVariable UUID produtoId,
            @PageableDefault(page = 0, size = 12, sort = "dataCriacao") Pageable pageable) {

        Page<AvaliacaoDTO> page = avaliacaoService.listarPorProduto(produtoId, pageable)
                .map(AvaliacaoDTO::new);
        return ResponseEntity.ok(page);
    }
}
```

**Regras do Controller:**
- Usar `@AuthenticationPrincipal Usuario usuarioLogado` em vez de `SecurityContextHolder` diretamente
- Retornar sempre `ResponseEntity<DTO>`, nunca a entidade crua
- Converter Model → DTO aqui ou no service (ambos aceitáveis)
- Não colocar lógica de negócio no controller

---

## Convenções de nomenclatura

| Elemento | Convenção | Exemplo |
|---|---|---|
| Entidade | PascalCase, singular | `Avaliacao`, `ItemPedido` |
| Tabela DB | snake_case, singular | `avaliacao`, `item_pedido` |
| Repository | `{Entidade}Repository` | `AvaliacaoRepository` |
| Service | `{Entidade}Service` | `AvaliacaoService` |
| Controller | `{Entidade}Controller` | `AvaliacaoController` |
| Request (entrada) | `{Entidade}Request` | `AvaliacaoRequest` |
| DTO (saída) | `{Entidade}DTO` | `AvaliacaoDTO` |
| Endpoint base | plural, kebab-case | `/api/avaliacoes` |
| Método de criar | `criar` ou `create{Entidade}` | `criarAvaliacao` |
| Método de buscar | `buscar{Entidade}Por{Campo}` | `buscarAvaliacaoPorId` |
| Método de listar | `listar{Entidades}` | `listarAvaliacoes` |

---

## HTTP Status codes

| Situação | Status |
|---|---|
| Criação bem-sucedida | 201 Created |
| Leitura/update bem-sucedido | 200 OK |
| Delete bem-sucedido | 204 No Content |
| Recurso não encontrado | 404 Not Found |
| Usuário não autenticado | 401 Unauthorized |
| Usuário autenticado mas sem permissão | 403 Forbidden |
| Dados inválidos no body | 400 Bad Request |
| Conflito (email duplicado, etc) | 409 Conflict |
| Erro interno | 500 (lançar exceção — Spring trata) |

---

## Paginação

Padrão do projeto para endpoints que listam coleções:

```java
// Controller — sempre com @PageableDefault
@GetMapping
public ResponseEntity<Page<MinhaEntidadeDTO>> listar(
    @PageableDefault(page = 0, size = 12, sort = "nome") Pageable pageable) {
    
    return ResponseEntity.ok(service.listar(pageable).map(MinhaEntidadeDTO::new));
}
```

Quando o frontend precisa de metadados extras, usar `PaginaResponse<T>`:
```java
PaginaResponse<MinhaDTO> resposta = new PaginaResponse<>();
resposta.setContent(page.getContent());
resposta.setPage(page.getNumber());
resposta.setSize(page.getSize());
resposta.setTotalElements(page.getTotalElements());
resposta.setTotalPages(page.getTotalPages());
```

---

## Anti-padrões a evitar

| Anti-padrão | Por quê evitar | Alternativa |
|---|---|---|
| `System.out.println` em produção | Sem nível de log, sem controle | Usar `java.util.logging` ou SLF4J |
| Retornar entidade JPA diretamente em endpoint com relacionamentos | Serialização infinita ou LazyInit | Usar DTO |
| `@RequestParam` sem anotar parâmetro | Spring não injeta, sempre null | Sempre anotar `@RequestParam` |
| `@GeneratedValue` sem strategy | Usa AUTO que pode quebrar com UUID | Sempre `strategy = GenerationType.UUID` |
| Lógica de negócio no Controller | Viola SRP, dificulta teste | Mover para Service |
| Acesso a SecurityContext no Service | Dificulta teste unitário | Passar o usuário como parâmetro |
| `ddl-auto=update` em produção | Pode perder dados em rename | Usar Flyway com scripts SQL |
| Credenciais hardcoded | Vaza com o código | Variáveis de ambiente |
