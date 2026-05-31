# Kafka Testing Guide - Service Analytique (G8)

## Overview
This guide covers testing the Kafka implementation in the analytics service, including consumer, producer, DLT (Dead Letter Topic), and retry mechanisms.

## Prerequisites

- Docker Desktop running
- Kafka container (`sgitu-kafka`) started
- MongoDB running (local or container)
- Maven installed

## Quick Start

### 1. Start Infrastructure

```bash
# Start Kafka
docker compose up -d kafka

# Start MongoDB (if not running)
docker run -d --name mongo-g8 -p 27017:27017 mongo:7
```

### 2. Verify Kafka Health

```bash
# Check Kafka is running
docker ps | grep kafka

# Verify broker API
docker exec sgitu-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### 3. Start Analytics Service

```powershell
# Windows PowerShell
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS="localhost:29093"
$env:MONGO_URI="mongodb://localhost:27017/g8_analytics"
mvn spring-boot:run
```

```bash
# Linux/Mac
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29093
export MONGO_URI=mongodb://localhost:27017/g8_analytics
mvn spring-boot:run
```

## Test Scenarios

### Scenario 1: Valid Event Processing

**Purpose**: Verify valid events are consumed and stored

```bash
# Send valid ticketing event
echo '{"schemaVersion":1,"timestamp":"2024-01-15T10:00:00Z","userId":"u-01","status":"validated","line":"L1"}' | docker exec -i sgitu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic g2-ticketing-events
```

**Expected Result**:
- Message consumed by `g8-analytics-group`
- Event stored in MongoDB (`incoming_events` collection)
- Consumer lag = 0

**Verify**:
```bash
# Check consumer group lag
docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group | grep g2-ticketing-events

# Expected: CURRENT-OFFSET increases, LAG = 0
```

### Scenario 2: Schema Validation Failure

**Purpose**: Verify events with invalid/missing schemaVersion are rejected

```bash
# Send event without schemaVersion
echo '{"timestamp":"2024-01-15T10:05:00Z","userId":"u-bad","status":"expired"}' | docker exec -i sgitu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic g2-ticketing-events
```

**Expected Result**:
- Message consumed but rejected by `IngestionService`
- **NOT** sent to DLT (validation errors are handled gracefully)
- Response status: REJECTED

**Verify**:
```bash
# Check application logs for validation error
# Look for: "Missing required field: schemaVersion"
```

### Scenario 3: Retry and Dead Letter Topic (DLT)

**Purpose**: Verify failed events after retries go to DLT

**Precondition**: Stop MongoDB to force processing errors

```bash
# Stop MongoDB
docker stop mongo-g8

# Send valid event
echo '{"schemaVersion":1,"timestamp":"2024-01-15T10:00:00Z","userId":"u-dlt-test"}' | docker exec -i sgitu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic g2-ticketing-events

# Wait 10 seconds (3 retries with 1s backoff each)
sleep 10

# Check DLT
docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic g8-analytics-dlt --from-beginning --max-messages 5

# Restart MongoDB
docker start mongo-g8
```

**Expected Result**:
- Event fails after 3 retry attempts
- Event sent to `g8-analytics-dlt` topic
- DLT message contains: original event, failure reason, exception details, timestamp

### Scenario 4: Analytics Results Publishing

**Purpose**: Verify service publishes analytics results to Kafka

**Precondition**: Service running with data in MongoDB

```bash
# Trigger analytics (via REST API or scheduled job)
curl -X POST http://localhost:8088/api/v1/analytics/refresh

# Or wait for scheduled job (runs every hour by default)

# Check analytics results topic
docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic g8-analytics-results --from-beginning --max-messages 10
```

**Expected Result**:
- Aggregations published to `g8-analytics-results`
- ML predictions published to `g8-ml-predictions`
- Alerts published to `g8-analytics-results` with type="ALERT"

### Scenario 5: Consumer Lag Monitoring

**Purpose**: Verify consumer is keeping up with message rate

```bash
# Check consumer group status
docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group

# Look for:
# - CURRENT-OFFSET (last committed offset)
# - LOG-END-OFFSET (last message in topic)
# - LAG (difference - should be 0 or minimal)

# For all topics
docker exec sgitu-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Scenario 6: Circuit Breaker on G5 Alerts

**Purpose**: Verify circuit breaker opens when G5 notification service is down

**Precondition**: G5 notification service stopped or wrong URL

```bash
# Check circuit breaker status (requires Actuator)
curl http://localhost:8088/actuator/health

# Force threshold breach by sending many incidents
echo '{"schemaVersion":1,"timestamp":"2024-01-15T10:00:00Z","incidentId":"i-01","type":"delay","severity":"high"}' | docker exec -i sgitu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic g7-incident-events
```

**Expected Result**:
- After 50% failure rate (default), circuit opens
- Subsequent alerts use fallback method
- Check logs for: "G5 circuit breaker OPEN"

## Automated Testing

### Run Unit Tests

```bash
# All Kafka tests
mvn test -Dtest="Kafka*Test,DeadLetter*Test"

# Specific test class
mvn test -Dtest=KafkaIntegrationTest
```

### Run Integration Tests with Embedded Kafka

```bash
mvn test -Dtest=KafkaIntegrationTest
```

**Note**: Uses `@EmbeddedKafka` - no external Kafka required

## Troubleshooting

### Issue: Service won't start (Port 8088 in use)

```powershell
# Find and kill process
netstat -ano | findstr :8088
taskkill /F /PID <PID>
```

### Issue: Docker connection error

```powershell
# Restart Docker Desktop
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Wait 30 seconds, then verify
docker ps
```

### Issue: Consumer not receiving messages

```bash
# Check consumer group exists
docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check topic exists
docker exec sgitu-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Verify consumer is subscribed
docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group
```

### Issue: DLT not receiving messages

- DLT only receives messages after **3 retry attempts**
- Validation errors (missing schemaVersion, invalid timestamp) do **NOT** trigger DLT
- DLT requires **exceptions** (e.g., database errors)

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `kafka.topics.dead-letter` | `g8-analytics-dlt` | DLT topic name |
| `kafka.topics.analytics-results` | `g8-analytics-results` | Results topic |
| `kafka.retry.interval` | `1000` | Retry interval in ms |
| `kafka.retry.max-attempts` | `3` | Max retry attempts |
| `kafka.consumer.max-poll-records` | `500` | Batch size for analytics |

## Kafka Topics

| Topic | Purpose | Partitions |
|-------|---------|------------|
| `g2-ticketing-events` | Ticketing events from G2 | 3 |
| `g3-subscription-events` | Subscription events from G3 | 3 |
| `g4-payment-events` | Payment events from G4 | 3 |
| `g6-vehicle-events` | Vehicle events from G6 | 3 |
| `g7-incident-events` | Incident events from G7 | 3 |
| `g1-user-events` | User events from G1 | 3 |
| `g8-analytics-dlt` | Dead letter topic | 1 |
| `g8-analytics-results` | Analytics aggregations | 3 |
| `g8-ml-predictions` | ML predictions | 3 |

## Useful Commands

```bash
# List all topics
docker exec sgitu-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Describe topic
docker exec sgitu-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic g2-ticketing-events

# View messages in topic (from beginning)
docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic <TOPIC> --from-beginning

# View messages (max 10)
docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic <TOPIC> --from-beginning --max-messages 10

# Delete topic (use with caution)
docker exec sgitu-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic <TOPIC>

# Reset consumer offset (use with caution)
docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group g8-analytics-group --topic g2-ticketing-events --reset-offsets --to-earliest --execute
```

## Health Checks

```bash
# Service health
curl http://localhost:8088/actuator/health

# Prometheus metrics
curl http://localhost:8088/actuator/prometheus | grep kafka
```
