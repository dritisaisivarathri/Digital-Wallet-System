# Frontend Setup

This folder contains a React + JavaScript + Tailwind CSS frontend for the Digital Wallet System backend in the repository root.

## What it connects to

- Default gateway target: `http://localhost:8090`
- In local dev, Vite proxies `/api/*` requests to the gateway
- You can override the gateway with `VITE_API_BASE_URL` in a local `.env.local`

## Commands

```bash
npm install
npm run dev
```

## Expected backend order

1. `eureka-server`
2. `config-server`
3. `api-gateway`
4. `auth-service`
5. `user-service`
6. `wallet-service`
7. `transaction-service`
8. `rewards-service`
9. `notification-service`
10. `admin-service`

## What the UI includes

- Login and signup
- Wallet balance, top up, transfer, and withdraw flows
- Transaction history and ledger balance view
- KYC submission and history
- Rewards summary and redemption catalog
- Profile update and password change
- Admin dashboard, pending KYC review, users, campaigns, and notifications
