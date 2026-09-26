# Arquitetura — Cardápio Semanal API

## 1. Visão de contexto (Cenário 1.1)

Três módulos autônomos. O Front nunca fala com a TheMealDB: todo acesso à externa
passa pela API, que trata e remapeia os dados antes de devolvê-los.

```mermaid
flowchart LR
    subgraph cliente["Navegador"]
        FE["<b>Front-End</b><br/>Vite + React + TS<br/><i>repo separado</i>"]
    end

    subgraph backend["API Back-End — este repositório"]
        API["<b>Spring Boot 3</b><br/>Java 21 · porta 8080"]
    end

    DB[("<b>PostgreSQL</b><br/>menu_item<br/>shopping_list_check")]
    EXT["<b>TheMealDB</b><br/><i>serviço externo público</i><br/>search.php · lookup.php"]

    FE -->|"REST / JSON<br/>CORS liberado"| API
    API -->|"JPA / Hibernate"| DB
    API -->|"HTTPS<br/>WebClient + timeout"| EXT

    classDef front fill:#eef2ff,stroke:#6366f1,stroke-width:2px,color:#312e81
    classDef api fill:#ecfdf5,stroke:#10b981,stroke-width:2px,color:#065f46
    classDef db fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#78350f
    classDef ext fill:#fee2e2,stroke:#ef4444,stroke-width:2px,color:#7f1d1d
    class FE front
    class API api
    class DB db
    class EXT ext
    style cliente fill:#ffffff,stroke:#cbd5e1
    style backend fill:#ffffff,stroke:#cbd5e1
```

## 2. Camadas internas

Cada camada só conhece a de baixo. `TheMealDbClient` é o **único** ponto do sistema
que faz chamadas HTTP externas.

Ficam fora do fluxo acima, por serem transversais:

- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) — intercepta qualquer exceção
  vinda dos controllers e devolve sempre um `ApiError` padronizado (400, 404, 503, 500).
- **`CorsConfig`** — libera as origens do Front, configuráveis por `CORS_ALLOWED_ORIGINS`.
- **`Measure`** — faz o parsing das medidas não estruturadas da TheMealDB
  (`"1/2 cup"`, `"½"`, `"Dash"`), usado pelo `ShoppingListService`.
- **`WeekRef`** — normaliza qualquer data para a segunda-feira da semana correspondente.

```mermaid
flowchart TD
    REQ(["Requisição HTTP"]) --> C1 & C2 & C3

    subgraph CTRL["&nbsp;Controller · REST + Bean Validation&nbsp;"]
        C1["MealSearchController<br/><small>GET /meals/search</small>"]
        C2["MenuItemController<br/><small>GET · POST · PUT · DELETE</small>"]
        C3["ShoppingListController<br/><small>GET · PATCH</small>"]
    end

    subgraph SVC["&nbsp;Service · regras de negócio&nbsp;"]
        S1["MenuItemService"]
        S2["ShoppingListService<br/><small>consolidação</small>"]
        S3["TheMealDbClient<br/><small>única saída HTTP</small>"]
    end

    subgraph REPO["&nbsp;Repository + Model · Spring Data JPA&nbsp;"]
        R1["MenuItemRepository<br/><small>→ MenuItem</small>"]
        R2["ShoppingListCheckRepository<br/><small>→ ShoppingListCheck</small>"]
    end

    C1 --> S3
    C2 --> S1
    C3 --> S2
    S1 --> S3
    S2 --> S3
    S1 --> R1
    S2 --> R1
    S2 --> R2
    R1 --> DB[("PostgreSQL")]
    R2 --> DB
    S3 --> EXT["TheMealDB<br/><small>serviço externo</small>"]

    classDef layer fill:#f8fafc,stroke:#94a3b8,color:#0f172a
    classDef ext fill:#fee2e2,stroke:#ef4444,color:#7f1d1d
    classDef db fill:#fef3c7,stroke:#f59e0b,color:#78350f
    class C1,C2,C3,S1,S2,S3,R1,R2 layer
    class EXT ext
    class DB db
    style CTRL fill:#ffffff,stroke:#cbd5e1
    style SVC fill:#ffffff,stroke:#cbd5e1
    style REPO fill:#ffffff,stroke:#cbd5e1
```

## 3. Fluxo da regra de negócio principal

`GET /shopping-list?week=` — transforma o cardápio da semana numa lista de compras
consolidada. O cache por request evita consultar a externa duas vezes pela mesma receita.

```mermaid
sequenceDiagram
    autonumber
    participant FE as Front-End
    participant CT as ShoppingListController
    participant SV as ShoppingListService
    participant RP as Repositories
    participant MD as TheMealDbClient
    participant EX as TheMealDB

    FE->>CT: GET /shopping-list?week=2026-09-21
    CT->>SV: consolidate(weekRef)
    SV->>SV: WeekRef.normalize() → segunda-feira
    SV->>RP: findByWeekRef(week)
    RP-->>SV: List<MenuItem>

    loop para cada mealId único
        SV->>MD: lookup(mealId)
        alt já no cache do request
            MD-->>SV: MealDetail (sem chamada externa)
        else primeira vez
            MD->>EX: GET /lookup.php?i={id}
            EX-->>MD: JSON cru
            MD-->>SV: MealDetail (DTO próprio)
        end
    end

    SV->>SV: Measure.parse() e escala por servings/4
    SV->>SV: agrupa por ingrediente, somando por unidade
    SV->>RP: findByWeekRef(week) — ShoppingListCheck
    RP-->>SV: estado "comprado" já marcado
    SV-->>CT: List<ShoppingListItemResponse> ordenada
    CT-->>FE: 200 OK

    note over MD,EX: Falha ou timeout aqui vira<br/>ExternalServiceException → 503,<br/>nunca uma stack trace crua
```

## 4. Modelo de dados

```mermaid
erDiagram
    MENU_ITEM {
        bigint id PK
        varchar day_of_week "SEG..DOM"
        varchar meal_type "CAFE, ALMOCO, JANTAR"
        varchar meal_id "id na TheMealDB"
        varchar meal_name
        varchar thumbnail_url
        int servings
        date week_ref "segunda-feira da semana"
    }

    SHOPPING_LIST_CHECK {
        bigint id PK
        varchar ingredient_name
        boolean checked
        date week_ref
    }
```

As duas tabelas não têm FK entre si: a lista de compras é **derivada** dos `menu_item`
a cada requisição, e `shopping_list_check` guarda apenas o que o usuário marcou —
ligadas logicamente por `week_ref` + nome do ingrediente. Assim, trocar uma receita ou
o número de porções refaz a lista sem deixar resíduo no banco.
