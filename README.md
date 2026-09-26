# Cardápio Semanal — API Back-End

API REST que monta o cardápio da semana e transforma esse cardápio numa **lista de compras consolidada**: os ingredientes de todas as receitas da semana são somados entre si e ajustados ao número de porções escolhido.

Este é o componente **API Back-End** do Cenário 1.1: é ele — e somente ele — quem conversa com a API externa [TheMealDB](https://www.themealdb.com/api.php). O Front-End nunca chama a externa diretamente.

- Repositório do Front-End: <https://github.com/hmartiins/mvp-puc-rio-arq-software-frontend>

## Arquitetura

![Arquitetura](docs/arquitetura.svg)

```
Front-End (React) ──REST──> API Back-End (Spring Boot) ──HTTPS──> TheMealDB
                                    │
                                    └──JPA──> PostgreSQL
```

## Stack

- Java 21 + Spring Boot 3.5
- Spring Web (REST) · Spring Data JPA · Bean Validation
- `WebClient` (WebFlux) para consumir a TheMealDB
- PostgreSQL (produção) · H2 (perfil `dev` e testes)
- JUnit 5 + Mockito
- springdoc-openapi (Swagger UI)

## Como executar

### Pré-requisitos
Java 21+ (o Maven vem junto, via `./mvnw`).

### Local, sem banco externo (perfil `dev`, H2 em arquivo)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API em <http://localhost:8080> e Swagger UI em <http://localhost:8080/swagger-ui.html>.

### Local, com PostgreSQL

```bash
export DB_URL=jdbc:postgresql://localhost:5432/cardapio
export DB_USER=cardapio
export DB_PASSWORD=cardapio
./mvnw spring-boot:run
```

### Testes

```bash
./mvnw test
```

### Docker

```bash
docker build -t cardapio-semanal-api .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/cardapio \
  -e DB_USER=cardapio -e DB_PASSWORD=cardapio \
  cardapio-semanal-api
```

> O `docker-compose.yml` que sobe Front + API + banco juntos fica na raiz do **repositório do Front-End**, que é o componente principal do Cenário 1.1.

## Variáveis de ambiente

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cardapio` | JDBC URL do banco |
| `DB_USER` | `cardapio` | Usuário do banco |
| `DB_PASSWORD` | `cardapio` | Senha do banco |
| `THEMEALDB_BASE_URL` | `https://www.themealdb.com/api/json/v1/1` | Base da API externa |
| `THEMEALDB_TIMEOUT_SECONDS` | `5` | Timeout das chamadas à externa |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000,http://localhost` | Origens liberadas para o Front |
| `SERVER_PORT` | `8080` | Porta HTTP |

Há um `.env.example` na raiz com esses valores.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/meals/search?q={query}` | Busca receitas por nome (proxy tratado da TheMealDB) |
| POST | `/menu-items` | Adiciona uma receita a um dia/refeição da semana |
| GET | `/menu-items?week={aaaa-MM-dd}` | Lista os itens do cardápio da semana |
| PUT | `/menu-items/{id}` | Atualiza dia, refeição e porções de um item |
| DELETE | `/menu-items/{id}` | Remove um item do cardápio |
| GET | `/shopping-list?week={aaaa-MM-dd}` | Lista de compras consolidada da semana |
| PATCH | `/shopping-list/{ingredient}/check?week={aaaa-MM-dd}` | Marca/desmarca um ingrediente como comprado |

`week` é opcional: quando omitido, vale a semana atual. Qualquer data é normalizada para a **segunda-feira** da semana correspondente, então `2026-09-02` e `2026-08-31` se referem à mesma semana.

### Exemplos

```bash
# buscar receitas
curl "http://localhost:8080/meals/search?q=arrabiata"

# adicionar ao cardápio (8 porções na segunda, no almoço)
curl -X POST http://localhost:8080/menu-items \
  -H 'Content-Type: application/json' \
  -d '{"mealId":"52771","dayOfWeek":"SEG","mealType":"ALMOCO","servings":8,"weekRef":"2026-08-31"}'

# lista de compras da semana
curl "http://localhost:8080/shopping-list?week=2026-08-31"

# marcar ingrediente como comprado
curl -X PATCH "http://localhost:8080/shopping-list/Garlic/check?week=2026-08-31" \
  -H 'Content-Type: application/json' -d '{"checked":true}'
```

Resposta da lista de compras:

```json
[
  { "ingredient": "Garlic", "quantity": "6 cloves + 2 cloves chopped", "checked": true,  "recipes": ["Spicy Arrabiata Penne", "Lasagne"] },
  { "ingredient": "Olive oil", "quantity": "0.5 cup + 1 tblsp", "checked": false, "recipes": ["Spicy Arrabiata Penne", "Lasagne"] }
]
```

## Modelo de dados

**`menu_item`** — uma receita atribuída a um dia/refeição da semana

| Campo | Tipo |
|---|---|
| `id` | Long (PK) |
| `day_of_week` | Enum `SEG..DOM` |
| `meal_type` | Enum `CAFE, ALMOCO, JANTAR` |
| `meal_id` | String (id na TheMealDB) |
| `meal_name` | String |
| `thumbnail_url` | String |
| `servings` | Integer |
| `week_ref` | LocalDate (segunda-feira da semana) |

**`shopping_list_check`** — o que o usuário já marcou como comprado

| Campo | Tipo |
|---|---|
| `id` | Long (PK) |
| `ingredient_name` | String |
| `checked` | Boolean |
| `week_ref` | LocalDate |

A lista de compras em si não é persistida: ela é sempre recalculada a partir dos `menu_item`, e só o estado "comprado" mora no banco. Assim, mudar uma receita ou o número de porções refaz a lista sem deixar resíduo.

## Regra de negócio: consolidação da lista de compras

`ShoppingListService.consolidate(weekRef)`:

1. Busca todos os `MenuItem` da semana.
2. Para cada `mealId` único, consulta o detalhe na TheMealDB — com **cache em memória por request**, então uma receita repetida em vários dias gera uma única chamada externa.
3. Extrai os pares ingrediente/medida da receita.
4. Ajusta a quantidade proporcionalmente às porções (`servings / 4`, já que as receitas da TheMealDB rendem cerca de 4 porções).
5. Agrupa por ingrediente somando quantidades entre receitas diferentes, **respeitando a unidade** — `1/4 cup` + `2 tbs` vira `0.25 cup + 2 tbs`, não uma soma sem sentido.
6. Cruza com os `ShoppingListCheck` da semana, preservando o que já foi marcado como comprado.
7. Devolve a lista ordenada alfabeticamente.

Medidas sem número (`"Dash"`, `"to taste"`) são preservadas como texto em vez de descartadas — o item continua aparecendo na lista.

## Tratamento da API externa

- `TheMealDbClient` é o **único** componente que faz chamadas HTTP externas.
- Nada da TheMealDB é repassado cru: tudo é mapeado para DTOs próprios (`MealSearchResponse`, `MealDetail`).
- Timeout configurável; qualquer falha de transporte vira `ExternalServiceException` e chega ao cliente como **503** com corpo padronizado, nunca como stack trace.
- `{"meals": null}` (busca sem resultado) vira lista vazia, não erro.

Erros seguem sempre o mesmo formato:

```json
{
  "timestamp": "2026-09-01T21:47:12.058-03:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "details": ["servings: servings deve ser no minimo 1"]
}
```

## API externa utilizada

**[TheMealDB](https://www.themealdb.com/api.php)** — base de receitas pública e gratuita.

- Não exige cadastro: a chave de teste `1` já está embutida na URL base (`/api/json/v1/1`).
- Sem custo e sem token para as rotas usadas.
- Rotas consumidas por esta API:

| Rota externa | Uso |
|---|---|
| `GET /search.php?s={nome}` | Busca de receitas por nome (`GET /meals/search`) |
| `GET /lookup.php?i={id}` | Detalhe da receita, com ingredientes e medidas (`POST /menu-items` e consolidação da lista de compras) |

Nenhuma outra rota externa é chamada.

## Estrutura do projeto

```
src/main/java/com/henriquemartins/cardapio
├── controller/   MealSearchController, MenuItemController, ShoppingListController
├── service/      MenuItemService, ShoppingListService, TheMealDbClient, Measure, WeekRef
├── repository/   MenuItemRepository, ShoppingListCheckRepository
├── model/        MenuItem, ShoppingListCheck, DiaDaSemana, MealType
├── dto/          MenuItemRequest/Response, MealSearchResponse, MealDetail, ...
├── exception/    GlobalExceptionHandler, ExternalServiceException, ResourceNotFoundException
└── config/       CorsConfig
```
