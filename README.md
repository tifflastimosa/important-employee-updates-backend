# important-employee-updates-backend
A prototype of a web application that would help managers organize important messages for employees and help employees manage numerous work messages.

This repository is the backend: a REST API built with **Spring Boot 4** (Java 17) and **MongoDB** (Atlas). Managers publish important messages to the whole company or to specific departments, optionally with a deadline. Employees see the messages addressed to them, acknowledge (sign) them, and see a countdown to each deadline. Managers can see who has and hasn't signed.

## Prerequisites

- **Java 17 or newer**: check with `java -version`.
- **A MongoDB database**: a free [MongoDB Atlas](https://cloud.mongodb.com) cluster works.
- Maven is **not** required. The project includes the Maven wrapper (`./mvnw`).

## Setup

### 1. Set up MongoDB Atlas

1. Create a cluster, or use an existing one.
2. Under **Security → Database & Network Access → Database Users**, create a database user with a username and password.
3. Under **Security → Database & Network Access → IP Access List**, add your current IP address. If you're deploying to Heroku, add `0.0.0.0/0` (see [Deploying to Heroku](#deploying-to-heroku)).
4. Click **Connect → Drivers** on your cluster and copy the connection string.

### 2. Create `application.properties`

Database credentials are kept out of git. `src/main/resources/application.properties` is git-ignored, so create it from the template:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Open the new file and replace `<user>`, `<password>` and `<cluster>` in `spring.mongodb.uri` with your values. If your password contains special characters such as `@ : / ? # %`, [URL-encode](https://www.mongodb.com/docs/manual/reference/connection-string/#std-label-connections-standard-connection-string-format) them. For example, `@` becomes `%40`.

```properties
spring.mongodb.uri=mongodb+srv://myuser:mypassword@cluster0.abcde.mongodb.net/
spring.mongodb.database=important_messages
app.cors.allowed-origins=http://localhost:5173
```

**Never commit this file.**

## Running the prototype

```bash
./mvnw spring-boot:run
```

The API starts at **http://localhost:8080**. Check it's working by opening http://localhost:8080/api/employees. You should get a JSON list, `[]` if the database is empty.

> Opening http://localhost:8080/ by itself shows a "Whitelabel Error Page". That's expected: the API only has routes under `/api/...`.

To run the frontend against it, start the React app (it expects the API at `http://localhost:8080`). Browser requests are allowed from the origins in `app.cors.allowed-origins`, which defaults to `http://localhost:5173`.

You can also run it from IntelliJ by running `ImportantMessagesPrototypeApplication`.

### Try it out

```bash
# Create a manager
curl -X POST localhost:8080/api/employees -H 'Content-Type: application/json' -d '{
  "employeeNumber": "E-001", "firstName": "Maya", "lastName": "Lee",
  "email": "maya@example.com", "department": "Engineering", "storeNumber": "0042",
  "role": "MANAGER", "agreedToElectronicSignature": true }'

# Create an employee
curl -X POST localhost:8080/api/employees -H 'Content-Type: application/json' -d '{
  "employeeNumber": "E-002", "firstName": "Sam", "lastName": "Cruz",
  "email": "sam@example.com", "department": "Engineering", "storeNumber": "0042" }'

# Publish a message with a deadline (use the manager's "id" from the first response)
curl -X POST localhost:8080/api/messages -H 'Content-Type: application/json' -d '{
  "title": "Updated Code of Conduct", "content": "Please read and sign by Friday.",
  "priority": "HIGH", "author": { "id": "<manager-id>" },
  "targetDepartments": ["Engineering"], "deadline": "2026-12-04T17:00:00-08:00" }'

# See what the employee still needs to sign, then sign it
curl localhost:8080/api/employees/<employee-id>/messages/pending
curl -X POST localhost:8080/api/messages/<message-id>/acknowledgements/<employee-id>

# See who has and hasn't signed
curl localhost:8080/api/messages/<message-id>/acknowledgements
```

## Running the tests

```bash
./mvnw test
```

**Note:** the integration tests run against the database configured in `application.properties`. Each test uses uniquely named data and deletes it afterwards, but point it at a development database, not production.

## API overview

### Employees: `/api/employees`

| Method | Path | Description |
|---|---|---|
| GET | `/api/employees?department=&storeNumber=` | List employees; both filters are optional |
| GET | `/api/employees/{id}` | Get one employee |
| POST | `/api/employees` | Create an employee |
| PUT | `/api/employees/{id}` | Update an employee (replaces all fields) |
| DELETE | `/api/employees/{id}` | Delete an employee |
| GET | `/api/employees/{id}/messages` | Messages addressed to this employee |
| GET | `/api/employees/{id}/messages/pending` | Messages this employee still needs to sign, soonest deadline first |

Employee fields: `employeeNumber`\*, `firstName`\*, `lastName`\*, `email`\*, `department`\*, `storeNumber`\*, `jobTitle`, `role` (`EMPLOYEE` by default, `MANAGER`, `ADMIN`), `agreedToElectronicSignature` (false by default). \* = required. Email and employee number must be unique.

### Important messages: `/api/messages`

| Method | Path | Description |
|---|---|---|
| GET | `/api/messages?priority=` | List messages, newest first; `priority` filter is optional |
| GET | `/api/messages/{id}` | Get one message |
| POST | `/api/messages` | Publish a message (author must be a `MANAGER` or `ADMIN`) |
| PUT | `/api/messages/{id}` | Update a message |
| DELETE | `/api/messages/{id}` | Delete a message |
| POST | `/api/messages/{id}/acknowledgements/{employeeId}` | Employee signs/acknowledges the message |
| GET | `/api/messages/{id}/acknowledgements` | Who has and hasn't signed |
| GET | `/api/messages/{id}/countdown` | Time left until the deadline |
| GET | `/api/messages/overdue` | Messages past their deadline that still have people who haven't signed |
| GET | `/api/messages/due-soon?days=7` | Messages due within the next N days |

Message fields: `title`\*, `content`\*, `author` (`{ "id": "..." }`)\*, `priority` (`LOW`, `NORMAL` by default, `HIGH`, `URGENT`), `targetDepartments` (empty means company-wide), `deadline` (optional, ISO-8601, e.g. `2026-12-04T17:00:00-08:00`; must be in the future).

When a message is published, everyone it's addressed to (except the author) is listed in `notAcknowledgedBy`. Signing moves them to `acknowledgedBy`. Every message response includes a `countdown` with `secondsRemaining`, `days`/`hours`/`minutes`/`seconds`, `overdue` and a `display` string such as `"2d 4h 13m left"`.

Errors are returned as JSON: **400** for invalid input (with a field-by-field `errors` list), **404** if something isn't found, **409** if a business rule is broken (for example, a duplicate email, or an employee trying to publish).

## Deploying to Heroku

The repo includes a `Procfile` and `system.properties` (Java 17). `application.properties` isn't in git, so on Heroku the settings come from **config vars**:

```bash
heroku config:set -a <your-app> \
  SPRING_MONGODB_URI="mongodb+srv://<user>:<password>@<cluster>.mongodb.net/" \
  SPRING_MONGODB_DATABASE="important_messages" \
  APP_CORS_ALLOWED_ORIGINS="https://<your-frontend>.herokuapp.com"
```

- **The names matter.** Spring Boot reads `SPRING_MONGODB_URI` and `SPRING_MONGODB_DATABASE`; a variable named just `MONGODB_URI` is ignored. Without `SPRING_MONGODB_DATABASE` the app fails to start with "Database name must not be empty".
- **Atlas must allow Heroku's IPs.** Heroku's IP addresses change, so add `0.0.0.0/0` to the Atlas IP Access List, and use a strong, unique database password. If the API times out with an `SSLException: internal_error` in `heroku logs`, this is the cause.
- Then deploy by pushing to Heroku, or with GitHub integration. Check it at `https://<your-app>.herokuapp.com/api/employees`.

## Project structure

```
src/main/java/com/example/important_messages_prototype/
├── controller/   REST endpoints and JSON error handling
├── model/        Employee, ImportantMessage, DeadlineCountdown, response summaries
├── repository/   Spring Data MongoDB repositories
└── service/      Business rules (who can publish, recipients, signing, deadlines)
```

## Known limitations

This is a prototype:

- **No authentication.** Anyone who can reach the API can read and change all data, and act as any employee. Add login before using real employee data.
- **Deleting an employee** doesn't update the messages they wrote or were sent.
- **Recipients are fixed when a message is published.** Employees added later aren't included unless the message's target departments are edited.
