# Digital Wallet & Rewards System

A production-ready, highly scalable, and event-driven Digital Wallet and Loyalty/Rewards backend. This system is designed around a microservices architecture using Java 17, Spring Boot 3, and Spring Cloud.

## Architecture Highlights
- **10 Core Microservices**: Eureka Server, Config Server, API Gateway, Auth, User, Wallet, Transaction, Rewards, Notification, Admin.
- **Microservices Communication**: REST for synchronous communication, Kafka for event-driven asynchronous processing (`wallet.topup.success`, `wallet.transfer.completed`, etc.).
- **Security**: Stateless JWT authentication layer validated upstream at the API Gateway.
- **Persistence & Caching**: PostgreSQL for relational entity states and Ledger. Redis for quick Wallet Balance lookup.
- **Resilience**: Centralized configuration management and service discovery. Ledger-first implementation for financial accuracy.

## Prerequisites
- Java 17
- Maven 3.8+
- Docker and Docker Compose
- *Network Connection* (to download Maven dependencies successfully)

## Project Structure
- `eureka-server` (Port: 8761)
- `config-server` (Port: 8888)
- `api-gateway` (Port: 8080)
- `auth-service` (Port: 8081)
- `user-service` (Port: 8082)
- `wallet-service` (Port: 8083)
- `transaction-service` (Port: 8084)
- `rewards-service` (Port: 8085)
- `notification-service` (Port: 8086)
- `admin-service` (Port: 8087)

## Local Setup & Run

### 1. Start Infrastructure Dependencies
Use Docker Compose to start the necessary infrastructure (PostgreSQL, Redis, Zookeeper, Kafka, Zipkin):
```bash
docker-compose down -v  # Recommended the first time to ensure init-db.sql runs
docker-compose up -d
```
Wait a minute for the brokers and database to initialize.
**Note**: The `init-db.sql` script will automatically create the 7 required microservice databases (`digital_wallet_*`).
Zipkin UI will be available at `http://localhost:9411`.

### 2. Build the Project
From the root directory, force a maven build to fetch all dependencies:
```bash
mvn clean install -U
```
*Note: If you experience Connection Reset errors, make sure you are not behind a restrictive proxy and have stable internet access.*

### 3. Start the Microservices
In a full production setup, these would be containerized. For local development, start them in the following specific order. You can start them via your IDE or via terminal using `mvn spring-boot:run`.

1. **Service Registry**: `eureka-server`
2. **Configuration**: `config-server`
3. **Gateway**: `api-gateway`
4. **Core Services**: `auth-service`, `user-service`, `wallet-service`, `transaction-service`
5. **Auxiliary Services**: `rewards-service`, `notification-service`, `admin-service`

### 4. API Documentation
All endpoints are strictly documented via OpenAPI/Swagger. Once a service is running, you can view its endpoints via:
- `http://localhost:<PORT>/swagger-ui.html`

### Usage Flow Example
1. Register a user at `POST http://localhost:8080/api/auth/register` (Gateway routes it to Auth).
2. Login to get a JWT token: `POST http://localhost:8080/api/auth/login`.
3. Submit KYC via User Service: `POST http://localhost:8080/api/users/kyc`.
4. Approve KYC via Admin Service: `POST http://localhost:8080/api/admin/kyc/{userId}/approve`.
5. Top-up Wallet: `POST http://localhost:8080/api/wallet/topup`. (Kafka will trigger Transaction Service to write Ledger, Rewards to grant points, and Notification to send email).
6. View Points Balance Sandbox: `GET http://localhost:8080/api/rewards/summary`.

## Distributed Tracing with Zipkin
- All Spring applications support Zipkin export via `http://localhost:9411/api/v2/spans`.
- Tracing is enabled by default. Start Zipkin before starting the services to avoid connection-refused errors.
- Optionally override the endpoint:
```bash
set ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
```
- Start Zipkin with `docker-compose up -d zipkin` and open `http://localhost:9411` to inspect traces across gateway and services.

## SonarQube and SonarLint
- SonarQube Maven analysis is configured at the parent project level.
- JaCoCo coverage reports are generated during test runs and are wired into Sonar analysis.
- VS Code workspace recommendations include the SonarLint extension.
- Setup steps are documented in [docs/sonarqube-setup.md](/D:/Digital-Wallet-System/docs/sonarqube-setup.md).

Run analysis from the repository root:
```powershell
$env:SONAR_HOST_URL="http://localhost:9000"
$env:SONAR_TOKEN="your_generated_token"
mvn clean verify sonar:sonar
```
