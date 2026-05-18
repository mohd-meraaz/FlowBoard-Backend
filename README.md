# FlowBoard Backend

Spring Boot microservices backend for FlowBoard.

## Services

- `api-gateway` on port `8080`
- `auth-service` on port `8081`
- `workspace-service` on port `8082`
- `board-service` on port `8083`
- `list-service` on port `8084`
- `card-service` on port `8085`
- `notification-service` on port `8086`
- `payment-service` on port `8088`
- `eureka-server` on port `8761`

## Infrastructure

- MySQL
- Redis
- RabbitMQ
- Eureka
- SonarQube

## Requirements

- Java 21
- Maven Wrapper
- Docker and Docker Compose

## Setup

1. Go to the backend folder:

```bash
cd FlowBoard-Backend
```

2. Create or update the shared `.env` file:

```text
FlowBoard-Backend/.env
```

3. Start infrastructure and services with Docker:

```bash
docker compose up -d --build
```

4. Open the main services if needed:

- Eureka: `http://localhost:8761`
- API Gateway: `http://localhost:8080`
- SonarQube: `http://localhost:9000`
- RabbitMQ UI: `http://localhost:15672`

## Environment Variables

The backend uses a shared `.env` file loaded by the services.

### Core

- `DB_USERNAME`
- `DB_PASSWORD`
- `AUTH_DB_URL`
- `WORKSPACE_DB_URL`
- `BOARD_DB_URL`
- `LIST_DB_URL`
- `CARD_DB_URL`
- `NOTIFICATION_DB_URL`
- `PAYMENT_DB_URL`

### Mail

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

### Auth and Security

- `JWT_SECRET`
- `JWT_EXPIRATION`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### Redis

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

### RabbitMQ

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`

### Eureka and Service Discovery

- `EUREKA_SERVER_URL`
- `EUREKA_INSTANCE_HOSTNAME`
- `EUREKA_INSTANCE_IP`

### Frontend and Cross-Service URLs

- `FRONTEND_BASE_URL`
- `AUTH_SERVICE_BASE_URL`
- `WORKSPACE_SERVICE_BASE_URL`
- `LIST_SERVICE_BASE_URL`
- `CARD_SERVICE_BASE_URL`

### Scheduler

- `DUE_DATE_REMINDER_HOURS_BEFORE`
- `DUE_DATE_URGENT_HOURS`

### Stripe

- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PRICE_PRO_MONTHLY`
- `STRIPE_PRICE_PRO_YEARLY`
- `STRIPE_PRICE_BUSINESS_MONTHLY`
- `STRIPE_PRICE_BUSINESS_YEARLY`

## Example Local `.env`

Use values that match your machine or Docker network:

```env
DB_USERNAME=root
DB_PASSWORD=your-password

AUTH_DB_URL=jdbc:mysql://localhost:3306/flowboard_auth
WORKSPACE_DB_URL=jdbc:mysql://localhost:3306/flowboard_workspace
BOARD_DB_URL=jdbc:mysql://localhost:3306/flowboard_board
LIST_DB_URL=jdbc:mysql://localhost:3306/flowboard_list
CARD_DB_URL=jdbc:mysql://localhost:3306/flowboard_card
NOTIFICATION_DB_URL=jdbc:mysql://localhost:3306/flowboard_notification
PAYMENT_DB_URL=jdbc:mysql://localhost:3306/flowboard_payment

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-app-password

JWT_SECRET=your-jwt-secret
JWT_EXPIRATION=86400000

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

EUREKA_SERVER_URL=http://localhost:8761/eureka/
FRONTEND_BASE_URL=http://localhost:4200

STRIPE_SECRET_KEY=your-stripe-secret
STRIPE_WEBHOOK_SECRET=your-webhook-secret
STRIPE_PRICE_PRO_MONTHLY=price_xxx
STRIPE_PRICE_PRO_YEARLY=price_xxx
STRIPE_PRICE_BUSINESS_MONTHLY=price_xxx
STRIPE_PRICE_BUSINESS_YEARLY=price_xxx
```

## Running Without Docker

You can also run services individually.

Example:

```bash
cd auth-service
./mvnw spring-boot:run
```

Repeat the same pattern for:

- `eureka-server`
- `api-gateway`
- `auth-service`
- `workspace-service`
- `board-service`
- `list-service`
- `card-service`
- `notification-service`
- `payment-service`

Start supporting services first:

- MySQL
- Redis
- RabbitMQ
- Eureka

## Test Instructions

### Run Tests For One Service

Example:

```bash
cd board-service
./mvnw test
```

### Run Tests For All Services

Run each service separately:

```bash
cd auth-service && ./mvnw test
cd ../workspace-service && ./mvnw test
cd ../board-service && ./mvnw test
cd ../list-service && ./mvnw test
cd ../card-service && ./mvnw test
cd ../notification-service && ./mvnw test
cd ../payment-service && ./mvnw test
cd ../api-gateway && ./mvnw test
cd ../eureka-server && ./mvnw test
```

### Coverage and Verification

Some services are configured with JaCoCo and Sonar properties. To run verification where configured:

```bash
./mvnw verify
```

## API and Docs

Most services expose Swagger/OpenAPI endpoints.

Common local paths:

- `/api-docs`
- `/swagger-ui.html`

Example:

```text
http://localhost:8081/swagger-ui.html
```

## Deployment Notes

- The frontend-facing base URL should be set with `FRONTEND_BASE_URL`.
- Board share emails, workspace invites, OAuth redirects, and payment callbacks depend on correct frontend and mail configuration.
- The API gateway should be the main public entry point for frontend API traffic.
