# G8 Analytics Integration Testing Plan

This plan is staged on purpose. Each stage assumes you start the required services manually, then run the matching script to verify that stage.

Do not run `service-analytique/docker-compose.yml` for integration tests. Use the root `docker-compose.yml`.

## Stage 1: Start G8 Manually

Start only the G8 runtime and the shared infrastructure it needs:

```powershell
docker compose up -d --build kafka g8-mongo g8-ml-service g8-analytics-service
```

Then run the G8 integration checks:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1
```

What this script checks:

| Area | Result |
|---|---|
| `.env` | Reads the effective root `.env`; if `JWT_SECRET` appears more than once, the last value is used. |
| JWT | Generates a fresh HS256 token using the effective shared secret. |
| Compose | Validates the root compose model. |
| G8 runtime | Checks Kafka, private Mongo, private ML service, G8 health, and internal service-to-service connectivity. |
| Security | Confirms unauthenticated access is rejected and authenticated access works. |
| Ingestion | Sends REST ingestion payloads with current timestamps. |
| Kafka | Publishes direct compatibility messages to G8 topics and verifies G8 consumes them. |
| Analytics | Runs the analytics job and verifies snapshots/metrics. |
| Prometheus | Checks the G8 `/actuator/prometheus` endpoint. If global Prometheus is not running, the global target check is skipped. |


If you want to verify the global Prometheus target in the same stage, start Prometheus after G8 is already running:

```powershell
docker compose up -d --no-deps prometheus
```

`--no-deps` is important here because the current root compose dependency graph can otherwise start extra services that G8 does not need for Stage 1.


## Stage 2: Start G3 Manually

After Stage 1 passes, start G3:

```powershell
docker compose up -d --build g3-users-db redis user-service
```

Then run the G3-to-G8 Kafka test:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g3-user-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| G3 readiness | Checks G3 database and `user-service`. |
| G8 readiness | Checks Kafka, G8 Mongo, and G8 Analytics. |
| Real G3 action | Creates a user, reads the verification code from G3 logs, calls `POST /api/auth/verify-email`. |
| Kafka proof | Searches `g8-user-events` for that user ID with `action: active`. |
| G8 proof | Checks whether G8 stored the user event in Mongo. |
| Diagnosis | Separates "G3 did not publish" from "G3 published but G8 did not consume/persist." |

Current G3 behavior:

```text
POST /api/users creates an inactive account only.
POST /api/auth/verify-email publishes active to g8-user-events.
PUT /api/users/{id}/deactivate publishes inactive.
PUT /api/users/{id}/activate publishes active.

Payload format (G3 sends, without schemaVersion):
{
  "userId": "3",
  "action": "active",
  "timestamp": "2026-06-01T05:15:42Z"
}

G8 receives and automatically adds schemaVersion=1 for validation compatibility.
```

**Status: PASSING** — verify-email flow confirmed end-to-end.

## Stage 3: Start G5 Manually

After Stage 2 passes, start G5:

```powershell
docker compose up -d --build mysql-notification notification-service
```

Then run the G8-to-G5 alert test:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g5-alert-integration.ps1
```

What this script checks:

| Area | Result |
|---|---|
| G5 readiness | Checks `mysql-notification` and `notification-service`. |
| Baseline | Counts existing G5 notifications from `G8_ANALYTICS`. |
| Threshold data | Sends delayed vehicle events and repeated critical incidents to G8. |
| Alert trigger | Runs the G8 analytics job immediately. |
| G8 proof | Checks `sgitu_alerts_triggered_total` for expected alerts. |
| G5 proof | Checks the G5 MySQL `notifications` table before and after. |
| Diagnosis | Tells whether the failure is G8 not triggering, or G5 not accepting/persisting. |

Expected alert types:

```text
PUNCTUALITY_ALERT
HIGH_INCIDENT_VOLUME
INCIDENT_ZONE_RISK
```

Local G5 startup fixes applied for root compose integration:

```text
- Root compose no longer mounts a missing firebase-adminsdk.json path as a directory.
- FIREBASE_CREDENTIALS_PATH defaults to empty; FCM stays disabled unless a real file is provided.
- FirebaseConfig skips startup when the credentials path is missing or not a readable file.
```

G8 alert delivery fix:

```text
G8 AlertSender now attaches a shared JWT (same JWT_SECRET as G5) when calling POST /api/notifications/send.
Without this header G5 returns 401 and increments G8 metrics without persisting notifications.
```

Rebuild G8 and G5 after pulling these changes:

```powershell
docker compose up -d --build g8-analytics-service mysql-notification notification-service
```

## Stage 4: Start One Sender At A Time

Stage 4 follows the same sequence used for G3:

```text
1. Start G8 and shared infrastructure.
2. Start exactly one sender service and its private database.
3. Run that sender's PowerShell script.
4. Read the script diagnosis before starting the next sender.
```

Keep Stage 4 one-sender-at-a-time on purpose. It makes topic drift, schema drift, and missing producer wiring much easier to isolate.

### Stage 4.1 Ticketing

Start the sender:

```powershell
docker compose up -d --build service-billetterie billetterie-mongo
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g1-ticketing-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks `service-billetterie` and `billetterie-mongo`. |
| Real action | Creates a ticket, then calls the validation endpoint. |
| Kafka proof | Searches `ticket.validated` for the created ticket ID. |
| G8 proof | Checks `incoming_events` for `sourceType: TICKETING`. |
| Analytics proof | Runs G8 analytics and checks `FREQ_TOTAL_VALIDATIONS`. |

**Status: FAIL (Sender Side)** — `service-billetterie` Docker build may fail on Java version mismatch.

### Stage 4.2 Subscriptions

Start G3 and G2:

```powershell
docker compose up -d g3-users-db user-service db-abonnement abonnement-service
```

Ensure root `.env` uses:

```text
G3_BASE_URL=http://g3-user-service:8083
G2_BASE_URL=http://service-abonnement:8082
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g2-subscription-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks G3, G8, Kafka, `service-abonnement`, and `db-abonnement`. |
| Real G3 user | Creates and verifies a real G3 passenger. |
| Real action | Creates a plan (admin JWT), then calls `/abonnements/souscrire` (passenger JWT + email). |
| G2 trace proof | Reads `analytique_trace` from G2 MySQL after souscription. |
| G8 batch proof | Relays the trace through G8 `POST /api/events/batch` (G2 `AnalyseClient` contract). |
| G8 proof | Checks `incoming_events` for `sourceType: SUBSCRIPTION`. |
| Analytics proof | Runs G8 analytics and checks `SUB_NEW`. |

Current G2 behavior:

```text
Initial souscription does NOT publish a G8-shaped Kafka event.
G2 stores AnalytiqueTrace, then AnalyseClient sends batches to POST /api/events/batch.
Kafka topic abonnement.souscription is G5 notification-shaped and fires on confirmation only.

G8 exposes POST /api/events/batch as a compatibility endpoint for G2.
```

### Stage 4.3 Vehicle Tracking

Start the sender:

```powershell
docker compose up -d --build g7-service db-g7
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-vehicle-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks `g7-service` and `db-g7`. |
| Real action | Creates a vehicle, sets it `EN_SERVICE`, then posts a GPS position. |
| Kafka proof | Searches `g8.vehicule.status`, the G8 analytics topic. |
| Extra sender proof | Also searches `vehicule-positions`, because current G7 position flow publishes telemetry there. |
| G8 proof | Checks `incoming_events` for `sourceType: VEHICLE`. |
| Analytics proof | Runs G8 analytics and checks `VEH_PUNCTUALITY`. |
| Alert observation | Reads the Prometheus alert counter for `PUNCTUALITY_ALERT` as non-blocking context. |

Known risk to watch:

```text
G7 implements KafkaProducerService.envoyerStatusG8, but VehiculeService never calls it
on create, updateStatut, or position flows. Only vehicule-positions receives telemetry.
```

**Status: PATCHED** — G7 now publishes to `g8.vehicule.status` from `updateStatut`, GPS ingestion, and `deleteVehicule`. Rebuild `g7-service` before running the test.

### Stage 4.4 Incidents

Start G9 in root compose:

```powershell
docker compose up -d --build g9-service db-g9
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-incident-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| G8 readiness | Checks Kafka, G8 Mongo, and G8 Analytics. |
| Real action | Calls `POST /api/incidents/signaler` with a passenger JWT and user headers. |
| Kafka proof | Searches `incident.analytique.topic`. |
| G8 proof | Checks `incoming_events` for `sourceType: INCIDENT`. |
| Analytics proof | Runs G8 analytics and checks `INC_TOTAL`. |
| Alert observation | Reads the Prometheus alert counter for `INCIDENT_ZONE_RISK` as non-blocking context. |

### Stage 4 Interpretation Rules

Use the same diagnosis model for every sender:

| Result | Likely owner |
|---|---|
| Real action fails | Sender service or its own dependency/precondition. |
| Real action succeeds, expected Kafka topic is empty | Sender-side producer wiring or topic name drift. |
| Expected Kafka topic has the test event, but G8 Mongo is empty | G8 consumer config, G8 validation, or payload contract drift. |
| G8 Mongo has the event, but snapshot does not change | G8 aggregation logic, event timestamp/window, or event status semantics. |
| Snapshot changes, alert missing | Threshold not crossed, alert rule window mismatch, or G5 unavailable. |

Current G8 compatibility behavior:

```text
Kafka listeners auto-add schemaVersion=1 before validation.
Kafka listeners accept both a single JSON object and a batch/list.
G8 normalizes known legacy sender fields for ticketing, vehicles, payments, subscriptions, and incidents.
G8 exposes POST /api/events/batch for G2 AnalytiqueTrace batches.
REST ingestion still expects callers to respect the documented contract.
```

### Out of scope: G6 Payments

G6 payment analytics is excluded from this plan. Current class-repo G6 code publishes notification events to `payment.notification` only and does not publish G8 analytics events to `payment.transaction.completed`.
