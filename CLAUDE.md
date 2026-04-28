# Recipes API — Micronaut Lambda Microservices

## Purpose

A reference project demonstrating a true serverless microservice setup on AWS:
one Lambda function per endpoint, API Gateway for routing, and MongoDB Atlas as the data store.
The focus is on project structure and separation of concerns across Lambda functions.

The project follows a spec-first approach — the OpenAPI spec drives model generation — though
this is partially compromised by the microservice handler pattern: the handlers call the service
layer directly, bypassing HTTP routing, so the spec no longer governs the entry points.
The generated models (`CreateRecipeRequest`, `RecipeDetailResponse`, `RecipeSummaryResponse`)
remain the canonical contract, but the routing annotations are gone.

## Project structure

Multi-module Maven project:

| Module               | Artifact             | Purpose                                   |
|----------------------|----------------------|-------------------------------------------|
| `recipes-repository` | `recipes-repository` | MongoDB entity + repository               |
| `recipes-service`    | `recipes-service`    | Business logic, domain models, exceptions |
| `recipes-api`        | `recipes-api-rest`   | Lambda handlers, OpenAPI spec, fat JAR    |

## Testing

There is no embedded HTTP server and no `main()` class. Local testing is done
exclusively via SAM CLI, which emulates the Lambda runtime in Docker.

`sam build` runs the full Maven build internally (via the Makefile in `recipes-api/`)
— no separate `mvn package` step is needed.

### Prerequisites

- Docker running
- [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)
  installed

### Start local MongoDB

A `docker-compose.yml` is included that starts a local MongoDB 7 instance:

```bash
docker compose up -d
```

Data is persisted in a named Docker volume (`recipes-mongo-data`) — it survives container restarts.
Use `docker compose down -v` to wipe the volume and start fresh.

### Start local API

In a second terminal, after MongoDB is running:

```bash
sam build
sam local start-api --env-vars env.json
```

API is available at `http://localhost:3000`.

> `sam local start-api` runs entirely in Docker — no AWS calls, no cost.

### Debug with IntelliJ

```bash
sam local start-api --debug-port 5858 --debug-args "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5858"
```

Create a **Remote JVM Debug** run configuration in IntelliJ (`Run → Edit Configurations → + → Remote JVM Debug`) with
host `localhost` and port `5858`. With `suspend=y` the container pauses until the debugger attaches — send a request
first, then attach.

### Example requests

```bash
# List recipes
curl http://localhost:3000/recipes

# Filter by ingredients (ALL must match)
curl "http://localhost:3000/recipes?ingredients=tomato,garlic"

# Get recipe by ID
curl http://localhost:3000/recipes/{id}

# Create recipe
curl -X POST http://localhost:3000/recipes \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta","cuisine":"Italian","prepTimeMinutes":20,"ingredients":["pasta","sauce"],"steps":["boil","mix"]}'
```

### Invoke a single function

```bash
sam local invoke ListRecipesFunction --event events/list-recipes.json --env-vars env.json
sam local invoke GetRecipeByIdFunction --event events/get-recipe.json --env-vars env.json
sam local invoke CreateRecipeFunction --event events/create-recipe.json --env-vars env.json
```

## Lambda handlers

Each function has a dedicated handler class in `recipes-api/src/main/java/com/recipes/api/handler/`:

| Function                | Handler class          |
|-------------------------|------------------------|
| `ListRecipesFunction`   | `ListRecipesHandler`   |
| `GetRecipeByIdFunction` | `GetRecipeByIdHandler` |
| `CreateRecipeFunction`  | `CreateRecipeHandler`  |

Each extends `MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`,
injects `RecipesService` via Micronaut DI, and handles its own error mapping.

## Coding conventions

### Extract complex chains into named methods

Any builder chain or stream pipeline longer than 2 steps must be extracted into
a private method with a descriptive name.

Extract the builder/stream call into a private method named after what it does, and call that method from the public
one.

### Prefer Optional over if/else for nullable branching

When a value may or may not be present and drives a decision, use `Optional` to express the
branch as a pipeline rather than an imperative if/else block. This keeps the calling method
linear and readable.

**Avoid:**

```java
String param = queryParams != null ? queryParams.get("x") : null;
if(param !=null&&!param.

isBlank()){
result =

doSomethingWith(parse(param));
        }else{
result =

doDefault();
}
```

**Prefer:**

```java
result =

parseX(input)          // returns Optional<T>
        .

map(this::doSomethingWith)
        .

orElseGet(this::doDefault);
```

Extract the parsing/resolution logic into a private method that returns `Optional<T>`.
The `execute` method should read as a linear pipeline with no branching.

## Known limitations

### No integration tests for `RecipesRepository`

Integration tests using Testcontainers (to spin up a real MongoDB instance in Docker) are not
yet in place. Testcontainers had connectivity issues on Docker Desktop 4.67.0 on Windows — both
the TCP proxy (`localhost:2375`) and the named pipe (`//./pipe/docker_engine`) return HTTP 400
for the Docker `/info` call that Testcontainers uses to validate the connection.

The unit tests in `RecipesRepositoryUnitTest` cover the logic via Mockito mocks on
`MongoCollection<RecipeEntity>`. If integration tests are needed in future, consider:

- Installing [Testcontainers Desktop](https://testcontainers.com/desktop/) which provides a
  bridge service that resolves this Docker Desktop compatibility issue
- Running tests inside WSL2 where the Docker Unix socket is directly accessible

## Design decisions

- **No main class** — this is a Lambda-only project. There is no embedded server
  and no `main()` entry point. Local development uses `sam local start-api`.
- **One handler per operation** — each Lambda function has a dedicated handler class
  (`ListRecipesHandler`, `GetRecipeByIdHandler`, `CreateRecipeHandler`) that extends
  `MicronautRequestHandler` and calls the service directly. All three share a single fat JAR.
- **OpenAPI generator produces models only** — `generateApis=false` in the plugin config.
  Model classes (`CreateRecipeRequest`, `RecipeDetailResponse`, `RecipeSummaryResponse`) are
  generated from the spec at build time. There is no generated API interface or controller.
- **No Netty dependency** — excluded from the fat JAR to keep it lean. Lambda
  does not need an embedded HTTP server.
