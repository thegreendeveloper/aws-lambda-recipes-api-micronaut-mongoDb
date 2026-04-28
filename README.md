# Recipes API — Micronaut on AWS Lambda

A spec-first REST API built with Micronaut, structured as microservices — one Lambda function per endpoint — behind API
Gateway, with DynamoDB as the data store.

## Prerequisites

- Java 17
- Maven 3.9+
- Docker (for local testing via SAM)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)

## Local testing

There is no embedded server. Local testing runs the actual Lambda runtime in Docker via SAM CLI.
`sam build` compiles the project and packages the Lambda function in one step.

```bash
sam build
sam local start-api
```

The API is available at `http://localhost:3000`.

### Start local DynamoDB

A `docker-compose.yml` is included that starts DynamoDB Local and creates the `Recipes` table:

```bash
docker compose up
```

Then in a second terminal:

```bash
sam build
sam local start-api --env-vars env.json
```

> **Note:** `sam local start-api` runs entirely in Docker on your machine — it never touches AWS and incurs no cost.

### Debugging with IntelliJ

Start SAM with the JDWP debug port exposed:

```bash
sam build
sam local start-api --debug-port 5858 --debug-args "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5858"
```

Then in IntelliJ create a **Remote JVM Debug** run configuration:

| Setting       | Value                |
|---------------|----------------------|
| Host          | `localhost`          |
| Port          | `5858`               |
| Debugger mode | Attach to remote JVM |

With `suspend=y` the Lambda container waits for the debugger to attach before processing each request. Send a request (
e.g. `curl http://localhost:3000/recipes`), then launch the debug configuration in IntelliJ — execution will stop at
your breakpoints.

### Endpoints

| Method | Path            | Description        |
|--------|-----------------|--------------------|
| `GET`  | `/recipes`      | List all recipes   |
| `GET`  | `/recipes/{id}` | Get a recipe by ID |
| `POST` | `/recipes`      | Create a recipe    |

### Example requests

```bash
# List all recipes
curl http://localhost:3000/recipes

# Get by ID
curl http://localhost:3000/recipes/1

# Create
curl -X POST http://localhost:3000/recipes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pasta Carbonara",
    "cuisine": "Italian",
    "prepTimeMinutes": 20,
    "ingredients": ["pasta", "eggs", "pancetta", "parmesan"],
    "steps": ["boil pasta", "fry pancetta", "mix eggs and cheese", "combine"]
  }'
```

## Deploy

### First deploy

```bash
sam build
sam deploy --guided
```

Follow the prompts — your answers are saved to `samconfig.toml` for future deploys.

After deploy completes, wire the S3 trigger for the import function:

```bash
make configure-recipes-import-trigger
```

This is a one-time step that tells the S3 bucket to invoke `ImportRecipesFunction` on upload.

### Subsequent deploys

```bash
sam build
sam deploy
```

### Dead Letter Queue

`ImportRecipesFunction` is invoked asynchronously by S3. If the handler throws on every attempt (after 2 retries),
Lambda forwards the original S3 event JSON to an SQS Dead Letter Queue so the failure is not silently lost.

#### Verify the DLQ is wired after deploy

In the AWS console: **Lambda → Functions → `recipes-import-<stack-name>` → Configuration → Asynchronous invocation**

Check that:

- **Retry attempts** is **2**
- Under **Destinations**, there is an entry labelled **Async inv** with an SQS ARN ending in
  `recipes-import-dlq-<stack-name>`

The DLQ URL is also printed as a stack output at the end of `sam deploy`.

#### Trigger a test failure

Upload a file with invalid JSON to the import bucket:

```bash
echo "not-valid-json" | aws s3 cp - s3://recipes-import-<account-id>-<region>/bad-import.json
```

Lambda will attempt the invocation 3 times total with exponential back-off (~3–5 minutes), then route the event to the
DLQ.

#### Confirm the message landed

```bash
aws sqs receive-message \
  --queue-url <dlq-url> \
  --attribute-names All
```

The DLQ URL is printed as a stack output after `sam deploy`. The message body is the original S3 event JSON. The
`ErrorCode` and `ErrorMessage` message attributes describe why it failed.

**In the AWS console:** SQS → Queues → `recipes-import-dlq-<stack>` → Send and receive messages → Poll for messages.

#### Replay a failed event

Once you have fixed the underlying problem (e.g. replaced the bad file), re-invoke the function manually using the DLQ
message body:

```bash
aws lambda invoke \
  --function-name $(aws cloudformation describe-stack-resource \
    --stack-name <stack-name> \
    --logical-resource-id ImportRecipesFunction \
    --query "StackResourceDetail.PhysicalResourceId" --output text) \
  --invocation-type RequestResponse \
  --payload '<message-body-from-dlq>' \
  response.json
```

Then purge the message from the DLQ so it is not processed again:

**SQS console** → `recipes-import-dlq-<stack>` → Purge.

### Tearing down

Before running `sam delete`, you must manually empty the S3 import bucket — CloudFormation cannot delete a non-empty
bucket:

**S3** → `recipes-import-<account-id>-<region>` → select all files → **Delete**

Then:

```bash
sam delete
```

> **Note:** The DynamoDB table has `DeletionPolicy: Retain` — it survives `sam delete` and keeps its data. If you want a
> clean redeploy, manually delete the `Recipes` table from the DynamoDB console before running `sam deploy` again,
> otherwise CloudFormation will fail trying to create a table that already exists.

## Project structure

```
recipes-repository/   DynamoDB entity + repository
recipes-service/      Business logic + domain models
recipes-api/          Lambda handlers + OpenAPI spec + fat JAR
template.yaml         SAM template (3 Lambda functions + API Gateway + DynamoDB)
```
