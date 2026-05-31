# A-to-Z Integration Testing Guide

This guide explains how to fully test the Analytics Microservice (both automated and manually) using the Docker container ecosystem.

## Prerequisites
- Docker & Docker Compose must be installed.
- No local Java, Maven, Kafka, or MongoDB installations are required. Everything runs inside containers.

---

## 1. Run the Build (JUnit Tests)

Before starting the ecosystem, ensure the code builds successfully by running all tests inside a disposable Maven container:
```powershell
docker run --rm `
  -v "${PWD}:/app" `
  -w /app `
  maven:3.9.6-eclipse-temurin-17 `
  mvn test
```
**Expected Output:** `BUILD SUCCESS` (0 failures, 0 errors).

---

## 2. Start the Environment

Spin up all dependencies (MongoDB, Zookeeper, Kafka, ML Service, Prometheus, Grafana, and Analytics Service) in a unified network.

```powershell
docker compose build
docker compose up -d
docker compose ps
```
Wait for all services to show as `Up (healthy)` or `Started`.

---

## 3. Automated End-to-End Test Suite

1. Start the Docker Compose environment.
2. Ensure you have the `sgitu-internal` network created (the docker-compose file handles this).
3. Run the dashboard seed script to inject realistic historical data (optional, but recommended for visual testing):
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\seed-dashboard-data.ps1
   ```
4. Execute the automated test harness script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
   ```
*Note: This script will display a clear green/red scorecard detailing the success or failure of each step.*

---

## 4. Manual Verification Steps

The recommended way to manually test the API is using the **Postman Collection** located in `docs/G8_Analytics_Postman_Collection.json`.

### How to use the Postman Collection:

1. Import `docs/G8_Analytics_Postman_Collection.json` into Postman.
2. Ensure your local environment is running via Docker Compose.
3. Open the **Phase 0: Security** folder:
   - Run `00a - No Token` to verify the system is secure (Expect 401).
   - Run `00b - Generate JWT Token`. This script will generate a valid JWT locally using the dev secret and save it to the collection variables.
4. Once `00b` is run, all subsequent requests in Phase 1, Phase 2, and Phase 3 will automatically use the generated token!
5. Run the requests in order to ingest data, view dashboards, and generate reports.

## 5. View Grafana Dashboards
Navigate to Grafana in your web browser to see live metrics:
- **URL:** `http://localhost:3000`
- **User:** `admin`
- **Password:** `sgitu2026`
