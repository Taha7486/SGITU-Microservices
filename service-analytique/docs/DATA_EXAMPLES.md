# G8 Analytics Service: Data Examples Reference

This document serves as the comprehensive reference for all data structures flowing into, through, and out of the G8 Analytics Service.

## Endpoint Summary

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/v1/ingestion/tickets` | Ingest ticketing events |
| `POST` | `/api/v1/ingestion/subscriptions` | Ingest subscription events |
| `POST` | `/api/v1/ingestion/payments` | Ingest payment events |
| `POST` | `/api/v1/ingestion/vehicles` | Ingest vehicle telemetry and status events |
| `POST` | `/api/v1/ingestion/incidents` | Ingest incident reports |
| `POST` | `/api/v1/ingestion/users` | Ingest user activity events |
| `GET` | `/api/v1/analytics/trips/summary` | Retrieve trips (FREQ) snapshots |
| `GET` | `/api/v1/analytics/revenue/summary` | Retrieve revenue (REV) snapshots |
| `GET` | `/api/v1/analytics/incidents/stats` | Retrieve incidents (INC) snapshots |
| `GET` | `/api/v1/analytics/vehicles/activity` | Retrieve vehicles (VEH) snapshots |
| `GET` | `/api/v1/analytics/users/stats` | Retrieve user statistics snapshots |
| `GET` | `/api/v1/analytics/subscriptions/stats` | Retrieve subscriptions (SUB) snapshots |
| `GET` | `/api/v1/analytics/dashboard` | Retrieve all current dashboard snapshots |
| `POST` | `/api/v1/analytics/reports/generate` | Trigger the generation of a new report |
| `GET` | `/api/v1/analytics/reports/{id}` | Retrieve a generated report by ID |
| `POST` | `/predict/peak-hours` | (ML Service) Predict future peak hours |
| `POST` | `/predict/incidents` | (ML Service) Predict high-risk incident zones |
| `POST` | `/test/kafka/vehicule` | ⚙️ Simulate G7 vehicle Kafka event (dev/test only — disabled in prod) |
| `POST` | `/test/kafka/incident` | ⚙️ Simulate G9 incident Kafka event (dev/test only — disabled in prod) |

---

## Section 1 — Ingestion Inputs (what other groups send us)

### TICKETING
**POST `/api/v1/ingestion/tickets`**
```json
[
  {
    "timestamp": "2026-05-03T08:30:00Z",
    "userId": "USR-98765",
    "status": "validated",
    "line": "L1",
    "stationId": "ST-05"
  },
  {
    "timestamp": "2026-05-03T08:31:12Z",
    "userId": "USR-11223",
    "status": "expired",
    "line": "L1",
    "stationId": "ST-05"
  },
  {
    "timestamp": "2026-05-03T08:35:00Z",
    "userId": "USR-55443",
    "status": "validated",
    "line": "L2",
    "stationId": "ST-10"
  }
]
```

### SUBSCRIPTION
**POST `/api/v1/ingestion/subscriptions`**
```json
[
  {
    "timestamp": "2026-05-03T09:00:00Z",
    "userId": "USR-456",
    "action": "created",
    "planType": "MONTHLY_STUDENT"
  },
  {
    "timestamp": "2026-05-03T09:15:00Z",
    "userId": "USR-789",
    "action": "renewed",
    "planType": "YEARLY_STANDARD"
  },
  {
    "timestamp": "2026-05-03T09:20:00Z",
    "userId": "USR-101",
    "action": "cancelled",
    "planType": "MONTHLY_STANDARD"
  }
]
```

### PAYMENT
**POST `/api/v1/ingestion/payments`**
```json
[
  {
    "timestamp": "2026-05-03T10:05:00Z",
    "transactionId": "TXN-001",
    "status": "completed",
    "amount": 25.50,
    "method": "CARD"
  },
  {
    "timestamp": "2026-05-03T10:10:00Z",
    "transactionId": "TXN-002",
    "status": "completed",
    "amount": 10.00,
    "method": "CASH"
  },
  {
    "timestamp": "2026-05-03T10:15:00Z",
    "transactionId": "TXN-003",
    "status": "failed",
    "amount": 15.00,
    "method": "CARD"
  }
]
```

### VEHICLE
**POST `/api/v1/ingestion/vehicles`**

> [!NOTE]
> In production, vehicle data arrives exclusively via **Kafka** (topic `g8.vehicule.status`) from G7.
> This REST endpoint is retained for backward compatibility and direct testing.

```json
[
  {
    "timestamp": "2026-05-03T11:00:00Z",
    "vehicleId": "BUS_404",
    "status": "in_service",
    "line": "L1",
    "delayMinutes": 2,
    "speed": 45.2
  },
  {
    "timestamp": "2026-05-03T11:05:00Z",
    "vehicleId": "TRAM_05",
    "status": "out_of_service"
  },
  {
    "timestamp": "2026-05-03T11:10:00Z",
    "vehicleId": "BUS_112",
    "status": "in_service",
    "line": "L4",
    "delayMinutes": 5,
    "speed": 30.5
  }
]
```

### INCIDENT
**POST `/api/v1/ingestion/incidents`**

> [!NOTE]
> In production, incident data arrives exclusively via **Kafka** (topic `incident.analytique.topic`) from G9.
> This REST endpoint is retained for backward compatibility and direct testing.

```json
[
  {
    "timestamp": "2026-05-03T12:00:00Z",
    "incidentId": "INC-1001",
    "type": "accident",
    "zone": "Z_NORTH",
    "resolutionMinutes": 45
  },
  {
    "timestamp": "2026-05-03T12:45:00Z",
    "incidentId": "INC-0998",
    "type": "delay",
    "zone": "Z_CENTER",
    "resolutionMinutes": 15
  },
  {
    "timestamp": "2026-05-03T13:00:00Z",
    "incidentId": "INC-1002",
    "type": "breakdown",
    "zone": "Z_EAST",
    "resolutionMinutes": 120
  }
]
```

### USER
**POST `/api/v1/ingestion/users`**
```json
[
  {
    "timestamp": "2026-05-03T14:00:00Z",
    "userId": "USR-111",
    "action": "active",
    "deviceOS": "IOS"
  },
  {
    "timestamp": "2026-05-03T14:05:00Z",
    "userId": "USR-112",
    "action": "active",
    "deviceOS": "ANDROID"
  },
  {
    "timestamp": "2026-05-03T14:10:00Z",
    "userId": "USR-222",
    "action": "inactive",
    "deviceOS": "WEB"
  }
]
```

### Kafka Integration — G7 Vehicle Events & G9 Incident Events

G8 is a **Kafka consumer only** — we never produce to Kafka. We consume from two topics:

| Topic | Producer | Purpose |
| :--- | :--- | :--- |
| `g8.vehicule.status` | G7 (Suivi des véhicules) | Vehicle telemetry (high-frequency, time-sensitive) |
| `incident.analytique.topic` | G9 (Gestion des incidents) | Resolved/cancelled incident reports |

> **⚠️ Kafka Configuration for Production**
>
> Kafka listeners are **disabled by default** (`auto-startup: false`). This prevents log flooding when no broker is available during local development.
>
> When connecting to a real Kafka broker (e.g., when integrating with G7/G9 in the root `docker-compose`), set **both** environment variables in the G8 service:
> ```yaml
> environment:
>   KAFKA_BOOTSTRAP_SERVERS: kafka:9092          # real broker address
>   KAFKA_LISTENER_AUTO_START: true              # activate the Kafka consumers
> ```
> - `KAFKA_BOOTSTRAP_SERVERS` — points to the real Kafka broker (default: `localhost:9092`)
> - `KAFKA_LISTENER_AUTO_START` — enables the `@KafkaListener` consumer threads (default: `false`)
>
> **Zero code changes are needed** — only these two environment variables change.
> The mock test endpoints (`/test/kafka/vehicule`, `/test/kafka/incident`) continue to work regardless of this setting.

---

#### G7 Vehicle Kafka Payload
**Topic:** `g8.vehicule.status`

G7 sends individual vehicle events (not arrays):
```json
{
  "vehicleId": "BUS_404",
  "line": "L1",
  "status": "ACTIVE",
  "speed": 45.2,
  "delayMinutes": 2,
  "timestamp": "2026-05-07T10:00:00"
}
```

**Field mapping (G7 → G8 IncomingEvent):**
| G7 Field | G8 Payload Field | Type | Notes |
| :--- | :--- | :--- | :--- |
| `vehicleId` | `vehicleId` | String | Required |
| `line` | `line` | String | Required |
| `status` | `status` | String | Required |
| `speed` | `speed` | Double | Optional |
| `delayMinutes` | `delayMinutes` | Integer | Optional |
| `timestamp` | *(event timestamp)* | String (ISO) | Maps to `IncomingEvent.timestamp` |

**Resulting IncomingEvent in MongoDB:**
```json
{
  "sourceType": "VEHICLE",
  "payload": {
    "vehicleId": "BUS_404",
    "line": "L1",
    "status": "ACTIVE",
    "speed": 45.2,
    "delayMinutes": 2
  },
  "timestamp": { "$date": "2026-05-07T09:00:00.000Z" },
  "receivedAt": { "$date": "2026-05-07T22:58:44.784Z" },
  "processed": false
}
```

**Mock test endpoint:** `POST /test/kafka/vehicule`
```bash
curl -X POST http://localhost:8088/test/kafka/vehicule \
  -H "Content-Type: application/json" \
  -d '{"vehicleId":"BUS_404","line":"L1","status":"ACTIVE","speed":45.2,"delayMinutes":2,"timestamp":"2026-05-07T10:00:00"}'
```
Expected: `200 OK` — `"Vehicle Kafka event simulated successfully"`

---

#### G9 Incident Kafka Payload
**Topic:** `incident.analytique.topic`

G9 sends resolved/cancelled incident events:
```json
{
  "reference": "INC-2026-ABCD",
  "source": "IOT",
  "type": "PANNE_VEHICULE",
  "gravite": "CRITIQUE",
  "statut": "CLOTURE",
  "vehiculeId": "550e8400-e29b-41d4-a716-446655440000",
  "ligneTransport": "Ligne 15",
  "declarantId": 0,
  "responsableId": 42,
  "description": "Surchauffe du moteur détectée par le capteur thermique.",
  "latitude": 33.573110,
  "longitude": -7.589843,
  "dateSignalement": "2026-05-06T14:30:00",
  "dateIncident": "2026-05-06T14:28:00",
  "dateResolution": "2026-05-06T16:45:00",
  "dateLimiteResolution": "2026-05-06T16:30:00"
}
```

**Processing rules:**
- **Only** events with `statut` = `CLOTURE` or `ANNULE` are saved
- Events with any other `statut` (e.g., `EN_COURS`, `OUVERT`) are ignored with a warning log

**Field mapping (G9 → G8 IncomingEvent):**
| G9 Field | G8 Payload Field | Type | Transformation |
| :--- | :--- | :--- | :--- |
| `reference` | `incidentId` | String | Direct copy |
| `type` | `type` | String | Direct copy (PANNE_VEHICULE, ACCIDENT, etc.) |
| `gravite` | `severity` | String | FAIBLE→LOW, MOYEN→MEDIUM, ELEVE→HIGH, CRITIQUE→CRITICAL |
| `latitude` + `longitude` | `zone` | String | Rounded to **3 decimal places**, dots replaced with underscores (e.g., `"33_573,-7_590"`) to form ~110m grid zones compatible with MongoDB map keys |
| `statut` | `status` | String | Direct copy (CLOTURE or ANNULE only) |
| `ligneTransport` | `line` | String | Direct copy |
| `dateIncident` + `dateResolution` | `resolutionMinutes` | Integer | `Duration.between(dateIncident, dateResolution).toMinutes()` |
| `dateIncident` | *(event timestamp)* | String (ISO) | Maps to `IncomingEvent.timestamp` |

**Resulting IncomingEvent in MongoDB:**
```json
{
  "sourceType": "INCIDENT",
  "payload": {
    "incidentId": "INC-2026-ABCD",
    "type": "PANNE_VEHICULE",
    "severity": "CRITICAL",
    "zone": "33_573,-7_590",
    "status": "CLOTURE",
    "line": "Ligne 15",
    "resolutionMinutes": 137
  },
  "timestamp": { "$date": "2026-05-06T13:28:00.000Z" },
  "receivedAt": { "$date": "2026-05-07T22:59:06.257Z" },
  "processed": false
}
```

**Mock test endpoint:** `POST /test/kafka/incident`
```bash
curl -X POST http://localhost:8088/test/kafka/incident \
  -H "Content-Type: application/json" \
  -d '{"reference":"INC-2026-TEST","source":"IOT","type":"PANNE_VEHICULE","gravite":"CRITIQUE","statut":"CLOTURE","vehiculeId":"BUS_404","ligneTransport":"Ligne 15","declarantId":0,"responsableId":42,"description":"Test incident","latitude":33.573110,"longitude":-7.589843,"dateSignalement":"2026-05-06T14:30:00","dateIncident":"2026-05-06T14:28:00","dateResolution":"2026-05-06T16:45:00","dateLimiteResolution":"2026-05-06T16:30:00"}'
```
Expected: `200 OK` — `"Incident Kafka event simulated successfully"`



---

## Section 2 — Raw Events in MongoDB (incoming_events collection)

**TICKETING Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a12" },
  "sourceType": "TICKETING",
  "sourceId": "VALIDATOR_101",
  "eventType": "TICKET_VALIDATED",
  "payload": {
    "ticketId": "TCK-98765",
    "status": "VALID",
    "scanType": "NFC"
  },
  "timestamp": { "$date": "2026-05-03T08:30:00Z" },
  "receivedAt": { "$date": "2026-05-03T08:30:01Z" },
  "lineId": "L1",
  "zoneId": "Z_CENTER",
  "processed": false
}
```

**SUBSCRIPTION Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a13" },
  "sourceType": "SUBSCRIPTION",
  "sourceId": "PORTAL_WEB",
  "eventType": "SUB_CREATED",
  "payload": {
    "userId": "USR-456",
    "planType": "MONTHLY_STUDENT",
    "action": "CREATED"
  },
  "timestamp": { "$date": "2026-05-03T09:00:00Z" },
  "receivedAt": { "$date": "2026-05-03T09:00:02Z" },
  "lineId": null,
  "zoneId": null,
  "processed": true
}
```

**PAYMENT Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a14" },
  "sourceType": "PAYMENT",
  "sourceId": "POS_301",
  "eventType": "PAYMENT_COMPLETED",
  "payload": {
    "transactionId": "TXN-001",
    "amount": 25.50,
    "method": "CARD",
    "status": "COMPLETED"
  },
  "timestamp": { "$date": "2026-05-03T10:05:00Z" },
  "receivedAt": { "$date": "2026-05-03T10:05:01Z" },
  "lineId": "L3",
  "zoneId": null,
  "processed": false
}
```

**VEHICLE Event** *(from Kafka — G7)*
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a15" },
  "sourceType": "VEHICLE",
  "sourceId": "BUS_404",
  "eventType": "VEHICLE_IN_SERVICE",
  "payload": {
    "vehicleId": "BUS_404",
    "line": "L1",
    "status": "ACTIVE",
    "speed": 45.2,
    "delayMinutes": 2
  },
  "timestamp": { "$date": "2026-05-07T10:00:00Z" },
  "receivedAt": { "$date": "2026-05-07T22:58:44.784Z" },
  "lineId": "L1",
  "zoneId": null,
  "processed": false
}
```

**INCIDENT Event** *(from Kafka — G9)*
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a16" },
  "sourceType": "INCIDENT",
  "sourceId": "INC-2026-ABCD",
  "eventType": "PANNE_VEHICULE",
  "payload": {
    "incidentId": "INC-2026-ABCD",
    "type": "PANNE_VEHICULE",
    "severity": "CRITICAL",
    "zone": "33_573,-7_590",
    "status": "CLOTURE",
    "line": "Ligne 15",
    "resolutionMinutes": 137
  },
  "timestamp": { "$date": "2026-05-06T14:28:00Z" },
  "receivedAt": { "$date": "2026-05-07T22:59:06.257Z" },
  "lineId": "Ligne 15",
  "zoneId": "33_573,-7_590",
  "processed": false
}
```

**USER Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a17" },
  "sourceType": "USER",
  "sourceId": "APP_MOBILE",
  "eventType": "USER_LOGIN",
  "payload": {
    "userId": "USR-111",
    "deviceOS": "iOS"
  },
  "timestamp": { "$date": "2026-05-03T14:00:00Z" },
  "receivedAt": { "$date": "2026-05-03T14:00:01Z" },
  "lineId": null,
  "zoneId": null,
  "processed": true
}
```

---

## Section 3 — Computed Snapshots in MongoDB (stat_snapshots collection)

### TRIPS (FREQ_01 to FREQ_07)
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_TOTAL_VALIDATIONS",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 6.0,
  "metadata": { "id": "FREQ_01", "data": { "total_validations": 6, "date": "2026-05-08" } },
  "computedAt": "2026-05-08T01:00:14.862",
  "prediction": false
}
```
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_PEAK_HOUR_DIST",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 6.0,
  "metadata": { "id": "FREQ_02", "data": { "0": 0, "1": 0, "8": 2, "9": 1, "17": 2, "18": 1, "...": "..." } },
  "computedAt": "2026-05-08T01:00:14.868",
  "prediction": false
}
```
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_PEAK_HOURS",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 6.0,
  "metadata": { "id": "FREQ_03", "data": { "17": 3, "8": 2, "18": 1 } },
  "computedAt": "2026-05-08T01:00:14.874",
  "prediction": false
}
```
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_AVG_DAILY",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 0.75,
  "metadata": { "id": "FREQ_04", "data": { "avg_daily_passengers": 0.75, "total_validations": 6 } },
  "computedAt": "2026-05-08T01:00:14.882",
  "prediction": false
}
```
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_LINE_RANKING",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 6.0,
  "metadata": { "id": "FREQ_05", "data": { "L1": 3, "L3": 2, "L2": 1 } },
  "computedAt": "2026-05-08T01:00:14.889",
  "prediction": false
}
```
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_STATION_FOOTFALL",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 6.0,
  "metadata": { "id": "FREQ_06", "data": { "ST-05": 3, "ST-15": 1, "ST-10": 1, "ST-20": 1 } },
  "computedAt": "2026-05-08T01:00:14.896",
  "prediction": false
}
```
```json
{
  "snapshotType": "TRIPS",
  "statId": "FREQ_WEEKEND_RATIO",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 0.0,
  "metadata": { "id": "FREQ_07", "data": { "weekend_vs_weekday_ratio": 0.0, "weekday": 6, "weekend": 0 } },
  "computedAt": "2026-05-08T01:00:14.904",
  "prediction": false
}
```

### REVENUE (REV_01 to REV_05)
```json
{
  "snapshotType": "REVENUE",
  "statId": "REV_TOTAL",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 101.25,
  "metadata": { "id": "REV_01", "data": { "total_revenue": 101.25, "date": "2026-05-08" } },
  "computedAt": "2026-05-08T01:00:14.909",
  "prediction": false
}
```
```json
{
  "snapshotType": "REVENUE",
  "statId": "REV_BY_TYPE",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 101.25,
  "metadata": { "id": "REV_02", "data": { "CARD": 75.5, "CASH": 25.75 } },
  "computedAt": "2026-05-08T01:00:14.917",
  "prediction": false
}
```
```json
{
  "snapshotType": "REVENUE",
  "statId": "REV_AVG_PER_PASSENGER",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 16.875,
  "metadata": { "id": "REV_03", "data": { "avg_revenue_per_passenger": 16.875, "revenue": 101.25, "passengers": 6.0 } },
  "computedAt": "2026-05-08T01:00:14.923",
  "prediction": false
}
```
```json
{
  "snapshotType": "REVENUE",
  "statId": "REV_PAYMENT_METHOD",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 5.0,
  "metadata": { "id": "REV_04", "data": { "total_payments": 5.0, "payment_method_breakdown": { "CARD": 60.0, "CASH": 40.0 } } },
  "computedAt": "2026-05-08T01:00:14.93",
  "prediction": false
}
```
```json
{
  "snapshotType": "REVENUE",
  "statId": "REV_TREND",
  "granularity": "DAY",
  "period": "2026-04-09/2026-05-08",
  "value": 101.25,
  "metadata": { "id": "REV_05", "data": { "revenue_trend": [{"date": "2026-05-07", "amount": 101.25}, {"date": "2026-05-08", "amount": 0.0}] } },
  "computedAt": "2026-05-08T01:00:14.937",
  "prediction": false
}
```

### INCIDENTS (INC_01 to INC_05)
```json
{
  "snapshotType": "INCIDENTS",
  "statId": "INC_TOTAL",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 4.0,
  "metadata": { "id": "INC_01", "data": { "total_incidents": 4, "date": "2026-05-08" } },
  "computedAt": "2026-05-08T01:00:14.777",
  "prediction": false
}
```
```json
{
  "snapshotType": "INCIDENTS",
  "statId": "INC_BY_TYPE",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 3.0,
  "metadata": { "id": "INC_02", "data": { "PANNE_VEHICULE": 2, "ACCIDENT": 1 } },
  "computedAt": "2026-05-08T01:00:14.785",
  "prediction": false
}
```
```json
{
  "snapshotType": "INCIDENTS",
  "statId": "INC_BY_ZONE",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 3.0,
  "metadata": { "id": "INC_03", "data": { "33_573,-7_590": 2, "33_585,-7_612": 1, "33_562,-7_544": 1 } },
  "computedAt": "2026-05-08T01:00:14.797",
  "prediction": false
}
```
```json
{
  "snapshotType": "INCIDENTS",
  "statId": "INC_AVG_RESOLUTION",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 21.67,
  "metadata": { "id": "INC_04", "data": { "unit": "minutes", "avg_resolution_time": 21.67 } },
  "computedAt": "2026-05-08T01:00:14.807",
  "prediction": false
}
```
```json
{
  "snapshotType": "INCIDENTS",
  "statId": "INC_REPEAT_ZONES",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 1.0,
  "metadata": { "id": "INC_05", "data": { "33_573,-7_590": 2 } },
  "computedAt": "2026-05-08T01:00:14.821",
  "prediction": false
}
```

### VEHICLES (VEH_01 to VEH_05)
```json
{
  "snapshotType": "VEHICLES",
  "statId": "VEH_ACTIVE_COUNT",
  "granularity": "REAL_TIME",
  "period": "now",
  "value": 4.0,
  "metadata": { "id": "VEH_01", "data": { "active_vehicles_count": 4 } },
  "computedAt": "2026-05-08T01:00:14.829",
  "prediction": false
}
```
```json
{
  "snapshotType": "VEHICLES",
  "statId": "VEH_PUNCTUALITY",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 75.0,
  "metadata": { "id": "VEH_02", "data": { "avgPunctualityRate": 75.0, "byLine": { "L1": 100.0, "L2": 0.0, "L3": 50.0 } } },
  "computedAt": "2026-05-08T01:00:14.835",
  "prediction": false
}
```
```json
{
  "snapshotType": "VEHICLES",
  "statId": "VEH_DELAY_DIST",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 2.0,
  "metadata": { "id": "VEH_03", "data": { "0-5min": 2, "5-10min": 1, ">10min": 0 } },
  "computedAt": "2026-05-08T01:00:14.84",
  "prediction": false
}
```
```json
{
  "snapshotType": "VEHICLES",
  "statId": "VEH_UTILIZATION",
  "granularity": "DAY",
  "period": "2026-05-08",
  "value": 80.0,
  "metadata": { "id": "VEH_04", "data": { "active": 4, "total": 5, "vehicle_utilization_rate": 80.0 } },
  "computedAt": "2026-05-08T01:00:14.847",
  "prediction": false
}
```
```json
{
  "snapshotType": "VEHICLES",
  "statId": "VEH_AVG_SPEED",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 31.3,
  "metadata": { "id": "VEH_05", "data": { "L1": 40.25, "L2": 35.0, "T1": 0.0, "L3": 50.0 } },
  "computedAt": "2026-05-08T01:00:14.856",
  "prediction": false
}
```

### SUBSCRIPTIONS (SUB_01 to SUB_05)
```json
{
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_ACTIVE",
  "granularity": "REAL_TIME",
  "period": "now",
  "value": 4.0,
  "metadata": { "id": "SUB_01", "data": { "active_subscriptions": 4 } },
  "computedAt": "2026-05-08T01:00:14.948",
  "prediction": false
}
```
```json
{
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_NEW",
  "granularity": "WEEK",
  "period": "2026-05-02/2026-05-08",
  "value": 2.0,
  "metadata": { "id": "SUB_02", "data": { "today": 0, "this_week": 2 } },
  "computedAt": "2026-05-08T01:00:14.963",
  "prediction": false
}
```
```json
{
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_RENEWAL_RATE",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 40.0,
  "metadata": { "id": "SUB_03", "data": { "total": 5, "renewal_rate": 40.0, "renewals": 2 } },
  "computedAt": "2026-05-08T01:00:14.977",
  "prediction": false
}
```
```json
{
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_CHURN",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 20.0,
  "metadata": { "id": "SUB_04", "data": { "churn_rate": 20.0, "total": 5, "cancellations": 1 } },
  "computedAt": "2026-05-08T01:00:14.989",
  "prediction": false
}
```
```json
{
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_TYPE_DIST",
  "granularity": "MONTH",
  "period": "2026-05",
  "value": 5.0,
  "metadata": { "id": "SUB_05", "data": { "total_subscriptions": 5.0, "subscription_type_distribution": { "MONTHLY_STUDENT": 20.0, "YEARLY_STANDARD": 40.0, "MONTHLY_STANDARD": 40.0 } } },
  "computedAt": "2026-05-08T01:00:15.004",
  "prediction": false
}
```

---

## Section 4 — ML Prediction Snapshots (PRED_01 and PRED_02)

**PRED_01: Peak Hours Prediction**
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9051" },
  "snapshotType": "PREDICTION",
  "statId": "PRED_01",
  "granularity": null,
  "period": null,
  "value": null,
  "metadata": {
    "predicted_peak_hours": [17, 8, 18],
    "distribution": [
      { "hour": 8, "score": 0.15 },
      { "hour": 9, "score": 0.10 },
      { "hour": 17, "score": 0.20 },
      { "hour": 18, "score": 0.18 }
    ],
    "generatedAt": "2026-05-03T14:30:00.123"
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": true
}
```

**PRED_02: Incident Zone Prediction**
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9052" },
  "snapshotType": "PREDICTION",
  "statId": "PRED_02",
  "granularity": null,
  "period": null,
  "value": null,
  "metadata": {
    "at_risk_zones": [
      { "zone": "33_573,-7_590", "riskScore": 1.0, "riskLevel": "HIGH" },
      { "zone": "33_585,-7_612", "riskScore": 0.65, "riskLevel": "MEDIUM" },
      { "zone": "33_562,-7_544", "riskScore": 0.2, "riskLevel": "LOW" }
    ],
    "generatedAt": "2026-05-08T12:43:19.091232"
  },
  "computedAt": { "$date": "2026-05-08T12:43:19Z" },
  "prediction": true
}
```

---

## Section 5 — Analytics API Responses (what G10 receives from us)

**GET `/api/v1/analytics/trips/summary`**
*(Note: Omitting internal fields like `_class` for brevity)*
```json
[
  {
    "id": "69fd39d180942120751154ce",
    "snapshotType": "TRIPS",
    "statId": "FREQ_TOTAL_VALIDATIONS",
    "granularity": "DAY",
    "period": "2026-05-08",
    "value": 6.0,
    "metadata": { "id": "FREQ_01", "data": { "total_validations": 6, "date": "2026-05-08" } },
    "computedAt": "2026-05-08T01:00:14.862",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/revenue/summary`**
```json
[
  {
    "id": "69fd39d180942120751154d5",
    "snapshotType": "REVENUE",
    "statId": "REV_TOTAL",
    "granularity": "DAY",
    "period": "2026-05-08",
    "value": 101.25,
    "metadata": { "id": "REV_01", "data": { "total_revenue": 101.25, "date": "2026-05-08" } },
    "computedAt": "2026-05-08T01:00:14.909",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/incidents/stats`**
```json
[
  {
    "id": "69fd39d080942120751154c4",
    "snapshotType": "INCIDENTS",
    "statId": "INC_TOTAL",
    "granularity": "DAY",
    "period": "2026-05-08",
    "value": 4.0,
    "metadata": { "id": "INC_01", "data": { "total_incidents": 4, "date": "2026-05-08" } },
    "computedAt": "2026-05-08T01:00:14.777",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/vehicles/activity`**
```json
[
  {
    "id": "69fd39d180942120751154c9",
    "snapshotType": "VEHICLES",
    "statId": "VEH_ACTIVE_COUNT",
    "granularity": "REAL_TIME",
    "period": "now",
    "value": 4.0,
    "metadata": { "id": "VEH_01", "data": { "active_vehicles_count": 4 } },
    "computedAt": "2026-05-08T01:00:14.829",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/users/stats`**
```json
[
  {
    "id": "69fd39d180942120751154df",
    "snapshotType": "USERS",
    "statId": "USR_DAU",
    "granularity": "DAY",
    "period": "2026-05-08",
    "value": 4.0,
    "metadata": { "id": "USR_01", "data": { "daily_active_users": 4, "date": "2026-05-08" } },
    "computedAt": "2026-05-08T01:00:15.013",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/subscriptions/stats`**
```json
[
  {
    "id": "69fd39d180942120751154da",
    "snapshotType": "SUBSCRIPTIONS",
    "statId": "SUB_ACTIVE",
    "granularity": "REAL_TIME",
    "period": "now",
    "value": 4.0,
    "metadata": { "id": "SUB_01", "data": { "active_subscriptions": 4 } },
    "computedAt": "2026-05-08T01:00:14.948",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/dashboard`**
*(Returns a flat list of all the snapshots above, plus predictions)*
```json
[
  {
    "id": "69fd39d180942120751154ce",
    "snapshotType": "TRIPS",
    "statId": "FREQ_TOTAL_VALIDATIONS",
    "granularity": "DAY",
    "period": "2026-05-08",
    "value": 6.0,
    "metadata": { "id": "FREQ_01", "data": { "total_validations": 6, "date": "2026-05-08" } },
    "computedAt": "2026-05-08T01:00:14.862",
    "prediction": false
  },
  {
    "id": "69fd39d180942120751154d5",
    "snapshotType": "REVENUE",
    "statId": "REV_TOTAL",
    "granularity": "DAY",
    "period": "2026-05-08",
    "value": 101.25,
    "metadata": { "id": "REV_01", "data": { "total_revenue": 101.25, "date": "2026-05-08" } },
    "computedAt": "2026-05-08T01:00:14.909",
    "prediction": false
  }
]
```

---

## Section 6 — Report Generation

**POST `/api/v1/analytics/reports/generate` (Request Body)**
```json
{
  "period": "2026-05-08",
  "types": ["TRIPS", "REVENUE", "INCIDENTS"]
}
```

**POST `/api/v1/analytics/reports/generate` (Response)**
```json
{
  "id": "69fd3b868094212075115504",
  "generatedAt": "2026-05-08T01:25:26.522704353",
  "period": "2026-05-08",
  "requestedTypes": [
    "TRIPS",
    "REVENUE",
    "INCIDENTS"
  ],
  "snapshots": [
    {
      "id": "69fd39d180942120751154ce",
      "snapshotType": "TRIPS",
      "statId": "FREQ_TOTAL_VALIDATIONS",
      "granularity": "DAY",
      "period": "2026-05-08",
      "value": 6.0,
      "metadata": { "id": "FREQ_01", "data": { "date": "2026-05-08", "total_validations": 6 } },
      "computedAt": "2026-05-08T01:25:08.75",
      "prediction": false
    },
    {
      "id": "69fd39d180942120751154d5",
      "snapshotType": "REVENUE",
      "statId": "REV_TOTAL",
      "granularity": "DAY",
      "period": "2026-05-08",
      "value": 101.25,
      "metadata": { "id": "REV_01", "data": { "total_revenue": 101.25, "date": "2026-05-08" } },
      "computedAt": "2026-05-08T01:25:08.816",
      "prediction": false
    }
  ]
}
```

**GET `/api/v1/analytics/reports/{id}`**
*(Returns the exact same response as the generation endpoint above).*

**Report Document in MongoDB (`reports` collection)**
```json
{
  "_id": { "$oid": "6634d8a1c2c1a84f3d1ba111" },
  "generatedAt": { "$date": "2026-05-03T14:40:01Z" },
  "period": "2026-05-03",
  "requestedTypes": ["TRIPS", "REVENUE", "INCIDENTS"],
  "snapshots": [
    {
      "_id": { "$oid": "6634d555c2c1a84f3d1b9001" },
      "snapshotType": "TRIPS",
      "statId": "FREQ_01",
      "granularity": "DAY",
      "period": "2026-05-03",
      "value": 145000.0,
      "metadata": { "id": "FREQ_TOTAL_VALIDATIONS", "data": {} },
      "computedAt": { "$date": "2026-05-03T14:30:00Z" },
      "prediction": false
    }
  ]
}
```

---

## Section 7 — Notifications sent to G5

### PUNCTUALITY_ALERT
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "PUNCTUALITY_ALERT",
  "severity": "WARNING",
  "notificationType": "EMAIL, PUSH",
  "targetAudience": "OPERATORS",
  "subject": "[SGITU] Taux de ponctualité critique",
  "body": "Le taux de ponctualité est tombé à 78.5% (seuil : 80%)",
  "metadata": {
    "statId": "VEH_02",
    "value": 78.5,
    "threshold": 80,
    "period": "today"
  }
}
```

### HIGH_INCIDENT_VOLUME
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "HIGH_INCIDENT_VOLUME",
  "severity": "WARNING",
  "notificationType": "EMAIL, SMS",
  "targetAudience": "SUPERVISORS",
  "subject": "[SGITU] Nombre d'incidents élevé",
  "body": "Incidents aujourd'hui : 12.0 (seuil : 10)",
  "metadata": {
    "statId": "INC_01",
    "value": 12.0,
    "threshold": 10,
    "period": "today"
  }
}
```

### HIGH_CHURN_RATE
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "HIGH_CHURN_RATE",
  "severity": "WARNING",
  "notificationType": "EMAIL",
  "targetAudience": "MANAGEMENT",
  "subject": "[SGITU] Taux d'attrition élevé",
  "body": "Taux de churn : 16.5% (seuil : 15%)",
  "metadata": {
    "statId": "SUB_04",
    "value": 16.5,
    "threshold": 15,
    "period": "this_month"
  }
}
```

### LOW_DAILY_REVENUE
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "LOW_DAILY_REVENUE",
  "severity": "WARNING",
  "notificationType": "EMAIL",
  "targetAudience": "MANAGEMENT",
  "subject": "[SGITU] Revenu journalier bas",
  "body": "Revenu du jour inférieur à 70% de la moyenne",
  "metadata": {
    "statId": "REV_01",
    "value": 25000.0,
    "threshold": 40000.0,
    "period": "today"
  }
}
```

### INCIDENT_ZONE_RISK
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "INCIDENT_ZONE_RISK",
  "severity": "CRITICAL",
  "notificationType": "EMAIL, SMS, PUSH",
  "targetAudience": "OPERATORS, SUPERVISORS",
  "subject": "[SGITU] Zone à risque détectée",
  "body": "4.0 zone(s) avec incidents répétés détectée(s)",
  "metadata": {
    "statId": "INC_05",
    "value": 4.0,
    "threshold": 3,
    "period": "this_month"
  }
}
```

---

## Section 8 — ML Service Inputs and Outputs

### POST `/predict/peak-hours`
**Input**
```json
{
  "data": [
    { "hour": 6, "validationCount": 5000 },
    { "hour": 7, "validationCount": 12000 },
    { "hour": 8, "validationCount": 25000 },
    { "hour": 9, "validationCount": 18000 },
    { "hour": 10, "validationCount": 10000 },
    { "hour": 16, "validationCount": 11000 },
    { "hour": 17, "validationCount": 22000 },
    { "hour": 18, "validationCount": 24000 }
  ]
}
```

**Output**
```json
{
  "predicted_peak_hours": [8, 18, 17],
  "distribution": [
    { "hour": 8, "score": 0.196 },
    { "hour": 18, "score": 0.188 },
    { "hour": 17, "score": 0.173 },
    { "hour": 9, "score": 0.141 },
    { "hour": 7, "score": 0.094 },
    { "hour": 16, "score": 0.086 },
    { "hour": 10, "score": 0.078 },
    { "hour": 6, "score": 0.039 }
  ],
  "generatedAt": "2026-05-03T14:30:00.123"
}
```

### POST `/predict/incidents`
**Input**
```json
{
  "data": [
    { "zone": "33_573,-7_590", "incidentCount": 5, "severity": "CRITICAL" },
    { "zone": "33_585,-7_612", "incidentCount": 2, "severity": "HIGH" },
    { "zone": "33_562,-7_544", "incidentCount": 1, "severity": "MEDIUM" },
    { "zone": "33_600,-7_630", "incidentCount": 3, "severity": "HIGH" }
  ]
}
```

**Output**
```json
{
  "at_risk_zones": [
    { "zone": "33_573,-7_590", "riskScore": 1.0, "riskLevel": "HIGH" },
    { "zone": "33_600,-7_630", "riskScore": 0.533, "riskLevel": "MEDIUM" },
    { "zone": "33_585,-7_612", "riskScore": 0.4, "riskLevel": "MEDIUM" },
    { "zone": "33_562,-7_544", "riskScore": 0.133, "riskLevel": "LOW" }
  ],
  "generatedAt": "2026-05-08T12:43:19.091232"
}
```

---

## Section 9 — Error Responses

**400 BAD REQUEST (Empty batch provided to ingestion)**
```json
{
  "totalReceived": 0,
  "totalAccepted": 0,
  "totalRejected": 0,
  "rejectedReasons": [
    "Request body must contain at least one event."
  ],
  "status": "REJECTED"
}
```

**200 OK — MULTI-STATUS (Mixed valid/invalid batch)**
```json
{
  "totalReceived": 3,
  "totalAccepted": 2,
  "totalRejected": 1,
  "rejectedReasons": [
    "Event 1: Timestamp cannot be more than 5 minutes in the future."
  ],
  "status": "PARTIAL"
}
```

**404 NOT FOUND (Report ID not found)**
```json
{
  "timestamp": "2026-05-03T14:41:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/v1/analytics/reports/6634d8a1c2c1a84f3d1ba999"
}
```

**500 INTERNAL SERVER ERROR (Database unavailable)**
```json
{
  "timestamp": "2026-05-03T14:42:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/v1/ingestion/tickets"
}
```

---

## Data Flow Diagram

**Other groups → Ingestion (REST + Kafka) → MongoDB → Scheduler → Snapshots → Analytics API → G10 → Admin/Directeur**

1. **Other Groups (G1-G7, G9):** External services generate domain-specific data. Most groups send via HTTP POST to G8's `IngestionController`. G7 (vehicles) and G9 (incidents) send data via **Kafka** topics consumed by G8's Kafka listeners.
2. **Ingestion & MongoDB:**
   - **REST path:** The `IngestionService` receives HTTP payloads, maps them to `IncomingEvent` documents, and persists them in the `incoming_events` collection.
   - **Kafka path:** `VehiculeKafkaListener` (topic: `g8.vehicule.status`) and `IncidentKafkaListener` (topic: `incident.analytique.topic`) parse Kafka messages, map G7/G9 fields to G8's `IncomingEvent` schema, and save directly via `EventRepository`.
3. **Scheduler:** A background cron job (`ScheduledAnalyticsJob`) wakes up periodically (e.g., every 60 seconds) to process unprocessed events.
4. **Snapshots (Aggregations & ML):** The scheduler triggers the Aggregation services (Trips, Revenue, etc.) to compute KPIs. It also triggers the `MlPredictionService` which queries MongoDB, formats data, sends it to the Python ML Service endpoints, and retrieves predictions. Both the computed KPIs and ML predictions are saved as `StatSnapshot` documents in the `stat_snapshots` collection via `SnapshotService.upsert()`. The `ThresholdAlertService` also runs here, sending alerts to G5 if any snapshot breaches its designated threshold.
5. **Analytics API:** The `AnalyticsController` exposes REST endpoints (GET) to retrieve these `StatSnapshot` documents and generate aggregated Reports.
6. **G10 (Reporting Interface):** The G10 Reporting module queries the Analytics API to construct dashboards and exportable reports.
7. **Admin/Directeur:** End-users interact with G10 to view the final, aggregated insights and predictions, completing the data lifecycle.
