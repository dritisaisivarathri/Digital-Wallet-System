# Digital Wallet System HLD

This document shows the request flow, service-to-service communication, and sync/async integration points in the system.

## 1. Overall HLD

```mermaid
flowchart LR
    U["Client / Frontend / Postman"] --> G["API Gateway :8090<br/>JWT validation<br/>Route forwarding"]

    G --> A["Auth Service :8081<br/>Login / Signup / Profile"]
    G --> US["User Service :8082<br/>KYC / User profile"]
    G --> W["Wallet Service :8083<br/>Balance / Top-up / Transfer"]
    G --> T["Transaction Service :8084<br/>Transaction history / Ledger"]
    G --> R["Rewards Service :8085<br/>Points / Catalog / Redeem"]
    G --> N["Notification Service :8086<br/>Notification history / Email"]
    G --> AD["Admin Service :8087<br/>Campaigns / KYC approval"]

    subgraph Infra["Supporting Infrastructure"]
        E["Eureka Server :8761<br/>Service discovery"]
        C["Config Server :8888<br/>Central config server"]
        K["Kafka :9092<br/>Event bus"]
        RD["Redis :6379<br/>Wallet balance cache"]
        PG["PostgreSQL :5433<br/>Service databases"]
        Z["Zipkin :9411<br/>Distributed tracing"]
        SMTP["SMTP / Gmail<br/>Email delivery"]
    end

    A -.registers.-> E
    US -.registers.-> E
    W -.registers.-> E
    T -.registers.-> E
    R -.registers.-> E
    N -.registers.-> E
    AD -.registers.-> E
    G -.registers.-> E
    C -.registers.-> E

    W --> RD
    A --> PG
    US --> PG
    W --> PG
    T --> PG
    R --> PG
    N --> PG
    AD --> PG

    W --> K
    AD --> K
    K --> T
    K --> R
    K --> N

    N --> SMTP

    G -.traces.-> Z
    A -.traces.-> Z
    US -.traces.-> Z
    W -.traces.-> Z
    T -.traces.-> Z
    R -.traces.-> Z
    N -.traces.-> Z
    AD -.traces.-> Z
    E -.traces.-> Z
    C -.traces.-> Z
```

## 2. Request Entry Pattern

- External client calls always enter through `API Gateway`.
- Gateway adds `X-Internal-Gateway-Token`.
- Most secured routes also require JWT in `Authorization: Bearer <token>`.
- Backend services block direct access if `X-Internal-Gateway-Token` is missing.
- Gateway routes use `lb://service-name`, so routing is discovery-aware.

## 3. Synchronous vs Asynchronous Communication

### Synchronous REST

- `Client -> API Gateway -> Auth Service`
- `Client -> API Gateway -> User Service`
- `Client -> API Gateway -> Wallet Service`
- `Client -> API Gateway -> Transaction Service`
- `Client -> API Gateway -> Rewards Service`
- `Client -> API Gateway -> Admin Service`
- `Admin Service -> API Gateway(localhost:8090) -> User Service`
- `User Service -> API Gateway(localhost:8090) -> Auth Service`

### Asynchronous Event Flow via Kafka

- `Wallet Service -> Kafka topic wallet.topup.success`
- `Wallet Service -> Kafka topic wallet.transfer.completed`
- `Admin Service -> Kafka topic kyc.status.updated`
- `Transaction Service <- wallet.topup.success`
- `Transaction Service <- wallet.transfer.completed`
- `Rewards Service <- wallet.topup.success`
- `Rewards Service <- wallet.transfer.completed`
- `Notification Service <- wallet.topup.success`
- `Notification Service <- wallet.transfer.completed`
- `Notification Service <- kyc.status.updated`

## 4. Core Business Flow Diagrams

### 4.1 Signup and Login Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant AuthDB as Auth DB

    Client->>Gateway: POST /api/auth/signup
    Gateway->>Auth: Forward request
    Auth->>AuthDB: Save credentials/profile
    AuthDB-->>Auth: Saved
    Auth-->>Gateway: Signup response
    Gateway-->>Client: Success

    Client->>Gateway: POST /api/auth/login
    Gateway->>Auth: Forward request
    Auth->>AuthDB: Validate credentials
    AuthDB-->>Auth: User found
    Auth-->>Gateway: JWT token
    Gateway-->>Client: JWT token
```

### 4.2 KYC Submission and Approval Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant User as User Service
    participant UserDB as User DB
    participant Admin as Admin Service
    participant Auth as Auth Service
    participant Kafka
    participant Notify as Notification Service
    participant Mail as SMTP

    Client->>Gateway: POST /api/users/kyc
    Gateway->>User: Submit KYC
    User->>UserDB: Save KYC as PENDING
    UserDB-->>User: Saved
    User-->>Gateway: KYC submitted
    Gateway-->>Client: Response

    Client->>Gateway: POST /api/admin/kyc/{userId}/approve
    Gateway->>Admin: Approve KYC
    Admin->>Gateway: Internal REST call to /api/users/internal/kyc/{userId}/approve
    Gateway->>User: Forward internal KYC approval
    User->>UserDB: Update KYC status = APPROVED
    User->>Gateway: Internal REST call to /api/auth/internal/users/{userId}/status
    Gateway->>Auth: Update auth status = ACTIVE
    Auth-->>Gateway: Updated
    Gateway-->>User: Updated
    User-->>Gateway: User email + KYC response
    Gateway-->>Admin: Response
    Admin->>Kafka: Publish kyc.status.updated
    Kafka-->>Notify: Consume KYC event
    Notify->>Mail: Send KYC approval email
    Notify->>UserDB: Save notification history
    Admin-->>Gateway: Approval success
    Gateway-->>Client: Response
```

### 4.3 Wallet Top-up Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Wallet as Wallet Service
    participant Redis
    participant WalletDB as Wallet DB
    participant Kafka
    participant Tx as Transaction Service
    participant Rewards as Rewards Service
    participant Notify as Notification Service

    Client->>Gateway: POST /api/wallet/topup
    Gateway->>Wallet: Top-up request
    Wallet->>WalletDB: Update wallet balance
    Wallet->>Redis: Refresh cached balance
    Wallet->>Kafka: Publish wallet.topup.success
    Wallet-->>Gateway: Top-up successful
    Gateway-->>Client: Response

    Kafka-->>Tx: Consume top-up event
    Tx->>WalletDB: No call
    Tx->>Tx: Save transaction + ledger entries

    Kafka-->>Rewards: Consume top-up event
    Rewards->>Rewards: Calculate and save points

    Kafka-->>Notify: Consume top-up event
    Notify->>Notify: Save notification history
```

### 4.4 Wallet Transfer Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Wallet as Wallet Service
    participant Redis
    participant WalletDB as Wallet DB
    participant Kafka
    participant Tx as Transaction Service
    participant Rewards as Rewards Service
    participant Notify as Notification Service

    Client->>Gateway: POST /api/wallet/transfer
    Gateway->>Wallet: Transfer request
    Wallet->>WalletDB: Debit sender, credit receiver
    Wallet->>Redis: Refresh both cached balances
    Wallet->>Kafka: Publish wallet.transfer.completed
    Wallet-->>Gateway: Transfer successful
    Gateway-->>Client: Response

    Kafka-->>Tx: Consume transfer event
    Tx->>Tx: Save transaction + debit/credit ledger entries

    Kafka-->>Rewards: Consume transfer event
    Rewards->>Rewards: Award points to sender

    Kafka-->>Notify: Consume transfer event
    Notify->>Notify: Save sender/receiver notifications
```

## 5. Service Responsibility Map

| Service | Main Responsibility | Talks To |
|---|---|---|
| API Gateway | Entry point, JWT validation, route forwarding | All business services, Eureka |
| Auth Service | Signup, login, token validation, user auth status | PostgreSQL |
| User Service | KYC submit/status, internal KYC operations | PostgreSQL, API Gateway -> Auth |
| Wallet Service | Balance, top-up, transfer | PostgreSQL, Redis, Kafka |
| Transaction Service | Ledger + transaction history | PostgreSQL, Kafka |
| Rewards Service | Reward points and catalog redemption | PostgreSQL, Kafka |
| Notification Service | Notification history and KYC email delivery | PostgreSQL, Kafka, SMTP |
| Admin Service | KYC approval/rejection, campaigns, dashboard | PostgreSQL, API Gateway -> User, Kafka |
| Eureka Server | Service registry | All services |
| Config Server | Central config host | Available in repo |

## 6. Important Communication Notes

- Wallet updates are immediate in `wallet-service`, but ledger/history/rewards/notifications happen asynchronously through Kafka.
- Because of that, `top-up` or `transfer` API response can come before transaction history or rewards summary gets updated.
- `Admin Service` does not directly call `User Service` by service name in current code; it calls `http://localhost:8090/...`, meaning it goes through API Gateway.
- `User Service` also updates `Auth Service` through API Gateway, not direct service-to-service discovery routing.
- `Redis` is used only by `wallet-service` for fast balance reads.
- `Notification Service` sends real email only for KYC status events; wallet top-up/transfer currently persist notification history.

## 7. Quick Flow Summary

- User onboarding flow: `Client -> Gateway -> Auth`
- KYC flow: `Client -> Gateway -> User -> Admin -> Gateway -> User -> Gateway -> Auth -> Kafka -> Notification`
- Wallet top-up flow: `Client -> Gateway -> Wallet -> Kafka -> Transaction + Rewards + Notification`
- Wallet transfer flow: `Client -> Gateway -> Wallet -> Kafka -> Transaction + Rewards + Notification`
- Rewards read flow: `Client -> Gateway -> Rewards`
- Transaction history flow: `Client -> Gateway -> Transaction`
