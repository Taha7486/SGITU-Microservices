# Rapport d'Avancement : Microservice Analytique (G8)

**Équipe :** Service Analytique (`service-analytique`)  
**Dernière mise à jour :** 7 juin 2026  
**Rôle dans SGITU :** Collecte, agrégation et exposition des indicateurs (KPI) et prédictions à partir des événements des autres microservices (utilisateurs G3, abonnements G2, véhicules G7, incidents G9, alertes G5).

---

## 1. Présentation du microservice

### Description du sous-système

Le **service analytique (G8)** constitue la couche décisionnelle du système SGITU. Il ingère des événements métier (REST et/ou Kafka), les persiste dans MongoDB, exécute des agrégations planifiées, déclenche des alertes sur seuils, interroge un microservice ML Python pour les prédictions, et expose les résultats via une API REST documentée (OpenAPI).

### Fonctionnalités métier implémentées

| Domaine | Description |
| :--- | :--- |
| **Ingestion** | Réception par lots (max 1 000 événements) via REST (`/api/v1/ingestion/*`) et consommation Kafka sur les topics des groupes intégrés. Validation `schemaVersion`, parsing des timestamps, statuts `SUCCESS` / `PARTIAL` / `REJECTED`. |
| **Agrégation batch** | Job planifié toutes les **60 s** (`ScheduledAnalyticsJob`) : 6 modules d’agrégation + détection d’alertes + 2 prédictions ML. |
| **Alertes** | `ThresholdAlertService` : 5 règles métier, envoi HTTP vers le service de notification **G5** avec **circuit breaker** Resilience4j. |
| **Prédictions ML** | `MlPredictionService` : heures de pointe (PRED_01) et zones à risque incidents (PRED_02), snapshots `PREDICTION` en base. |
| **Rapports** | Génération de rapports **JSON** consolidés (snapshots par type et période), stockés en MongoDB (`reports`). |
| **Consultation** | Endpoints REST pour lire les snapshots par domaine et le tableau de bord global. |

---

## 2. Conception (API REST)

### 2.1 Analytics — lecture des indicateurs

Base : `/api/v1/analytics` — contrôleur : `AnalyticsController`

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/dashboard` | Retourne l’ensemble des snapshots (`stat_snapshots`). |
| `GET` | `/trips/summary` | Snapshots **TRIPS** (fréquentation / billetterie, IDs `FREQ_*`). |
| `GET` | `/revenue/summary` | Snapshots **REVENUE** (`REV_*`). |
| `GET` | `/incidents/stats` | Snapshots **INCIDENTS** (`INC_*`). |
| `GET` | `/vehicles/activity` | Snapshots **VEHICLES** (`VEH_*`). |
| `GET` | `/users/stats` | Snapshots **USERS** (`USER_*`). |
| `GET` | `/subscriptions/stats` | Snapshots **SUBSCRIPTIONS** (`SUB_*`). |
| `POST` | `/reports/generate` | Corps : `{ "period", "types": [...] }` → rapport JSON persisté. |
| `GET` | `/reports/{id}` | Récupération d’un rapport par identifiant MongoDB. |

### 2.2 Ingestion — réception des événements bruts

Base : `/api/v1/ingestion` — contrôleur : `IngestionController`

| Méthode | Endpoint | Source métier |
| :--- | :--- | :--- |
| `POST` | `/tickets` | Billetterie (G1) — `TICKETING` |
| `POST` | `/subscriptions` | Abonnements (G2) — `SUBSCRIPTION` |
| `POST` | `/payments` | Paiements (G6) — `PAYMENT` |
| `POST` | `/vehicles` | Suivi véhicules (G7) — `VEHICLE` |
| `POST` | `/incidents` | Incidents (G9) — `INCIDENT` |
| `POST` | `/users` | Utilisateurs (G3) — `USER` |

Réponses HTTP : `201` (succès), `207` (partiel), `400` (rejet), `503` (erreur MongoDB).

### 2.3 Choix techniques API

- **Format d’échange :** JSON.
- **Documentation :** annotations OpenAPI + SpringDoc.
- **Intégration passerelle :** l’API Gateway route `/api/analytics/**` vers le port **8088**.

---

## 3. Implémentation et architecture

### 3.1 Vue d’ensemble

```text
[ G2 G3 G7 G9 ] ──Kafka / REST batch──► [ G8 Analytics (Spring Boot) ]
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
              MongoDB                   ML Service (FastAPI)      G5 Notifications
         incoming_events                  :5000                    :8085
         stat_snapshots
         reports
```

### 3.2 Couches applicatives (Java)

| Couche | Composants principaux |
| :--- | :--- |
| **Controllers** | `AnalyticsController`, `IngestionController`, `TestController` |
| **Services** | `AnalyticsService`, `IngestionService`, `SnapshotService`, `MlPredictionService` |
| **Agrégation** | `IncidentAggregation`, `VehicleAggregation`, `TicketAggregation`, `RevenueAggregation`, `SubscriptionAggregation`, `UserAggregation` |
| **Messaging** | `KafkaIngestionConsumer`, `KafkaConsumerConfig` (ack manuel) |
| **Alertes** | `ThresholdAlertService`, `AlertSender` (Resilience4j `@CircuitBreaker`) |
| **Planification** | `ScheduledAnalyticsJob` (`@Scheduled(fixedRate = 60000)`) |

### 3.3 Système d’alertes (G5)

| Règle | Condition | `eventType` |
| :--- | :--- | :--- |
| Ponctualité | `VEH_PUNCTUALITY` &lt; 80 % | `PUNCTUALITY_ALERT` |
| Volume incidents | `INC_TOTAL` &gt; 10 / jour | `HIGH_INCIDENT_VOLUME` |
| Churn abonnements | `SUB_CHURN` &gt; 15 % | `HIGH_CHURN_RATE` |
| Revenu journalier | `REV_TOTAL` &lt; 70 % de la moyenne 30 j | `LOW_DAILY_REVENUE` |
| Zones à risque | `INC_REPEAT_ZONES` ≥ 1 | `INCIDENT_ZONE_RISK` |

En cas d’indisponibilité de G5, le circuit breaker ouvre et les alertes sont journalisées (`G5 circuit breaker OPEN — alert dropped`).

---

## 4. Documentation et démonstration

| Ressource | Description |
| :--- | :--- |
| Swagger UI | `http://localhost:8088/swagger-ui.html` |
| Collection Postman complète | `docs/G8_Analytics_Postman_Collection.json` |
| **Démo live (Kafka)** | `docs/G8_LIVE_DEMO.postman_collection.json` + `docs/G8_LIVE_DEMO.md` |
| Plan de tests d’intégration | `docs/G8_INTEGRATION_TESTING_PLAN.md` |
| Guide des scripts PowerShell | `docs/TEST_SCRIPTS_GUIDE.md` |
| Dashboard Grafana | `monitoring/grafana/dashboards/sgitu_g8.json` |

### Démo live en classe (juin 2026)

Scénario documenté dans `G8_LIVE_DEMO.md` :

1. **Avant la démo** — build des conteneurs uniquement (pas de requêtes, Mongo vide).
2. **Phase 0** — santé G8, G5, Prometheus, G2, G3, G7, G9.
3. **Phase 1** — sécurité JWT.
4. **Phase 2** — appels aux APIs des senders Kafka (G3 verify-email, G7 statut/GPS, G9 annulation, G2 souscription + batch).
5. **Phase 3** — agrégation et KPIs G8.
6. **Phase 4** — alertes vers G5 (seuils + preuve MySQL).
7. **Phase 5** — résilience (arrêt G5, circuit breaker).
8. **Phase 6** — dashboard Grafana.

**Hors périmètre démo :** G1 (billetterie) et G6 (paiements) — non intégrés dans ce dépôt.

---

## 5. Tests

### 5.1 Tests automatisés (JUnit 5)

Couverture : controllers, ingestion REST, consommateur Kafka, job planifié, ML, circuit breaker G5, validation de schéma.

### 5.2 Scripts d’intégration PowerShell (compose racine)

| Script | Groupe | Résultat (juin 2026) |
| :--- | :--- | :--- |
| `test-root-integration.ps1` | G8 smoke | **24/24 PASS** |
| `test-g3-user-events.ps1` | G3 → Kafka `g8-user-events` | **12/12 PASS** |
| `test-g7-vehicle-events.ps1` | G7 → Kafka `g8.vehicule.status` | **15/15 PASS** |
| `test-g7-incident-events.ps1` | G9 → Kafka `incident.analytique.topic` | **12/12 PASS** |
| `test-g2-subscription-events.ps1` | G2 → batch REST | **17/17 PASS** |
| `test-g5-alert-integration.ps1` | G8 → G5 HTTP alertes | **20/20 PASS** |

```powershell
# Séquence complète (environnement conteneurisé) :
docker compose up -d --build kafka g8-mongo g8-ml-service g8-analytics-service
docker compose up -d --build mysql-notification notification-service
docker compose up -d --build g3-users-db redis user-service db-g7 g7-service db-g9 g9-service db-abonnement abonnement-service
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1
# … puis chaque script sender ci-dessus
```

---

## 6. Conteneurisation et orchestration

Stack Docker Compose (racine du monorepo) :

| Service | Conteneur | Port | Rôle |
| :--- | :--- | :--- | :--- |
| `g8-analytics-service` | G8 Java | 8088 | Application analytique |
| `g8-mongo` | MongoDB | 27017 | Persistance |
| `g8-ml-service` | FastAPI | 5000 | Prédictions ML |
| `g8-kafka` | Kafka | 9092 | Broker partagé |
| `notification-service` | G5 | 8085 | Réception alertes |
| `user-service` | G3 | 8083 | Événements utilisateurs |
| `g7-service` | G7 | 8087 | Statut véhicules |
| `g9-service` | G9 | 8089 | Incidents |
| `abonnement-service` | G2 | 8082 | Abonnements |
| `prometheus` / `grafana` | Observabilité | 9090 / 3000 | Métriques et dashboards |

---

## 7. Sécurité et résilience

| Sujet | État |
| :--- | :--- |
| **JWT / Spring Security** | ✅ Filtre `JwtAuthenticationFilter` — routes protégées |
| **Validation des événements** | ✅ `schemaVersion`, champs requis, normalisation inter-services |
| **Circuit breaker G5** | ✅ Resilience4j — dégradation gracieuse validée en démo |
| **Kafka** | ✅ Consommateur batch multi-topics opérationnel |

---

## 8. Intégration avec les autres groupes

| Groupe | Mécanisme | Topic / endpoint | Statut |
| :--- | :--- | :--- | :--- |
| **G3** Utilisateurs | Kafka | `g8-user-events` (verify-email / activate) | ✅ **Validé E2E** |
| **G7** Véhicules | Kafka | `g8.vehicule.status` (statut + GPS) | ✅ **Validé E2E** |
| **G9** Incidents | Kafka | `incident.analytique.topic` (annulation / clôture) | ✅ **Validé E2E** |
| **G2** Abonnements | Batch REST | `POST /api/events/batch` (AnalytiqueTrace) | ✅ **Validé E2E** |
| **G5** Notifications | HTTP POST alertes | JWT + `X-Source-Group: G8` | ✅ **Validé E2E** |
| **G1** Billetterie | Kafka `ticket.validated` | — | ❌ **Hors périmètre** (service non fonctionnel) |
| **G6** Paiements | Kafka `payment.transaction.completed` | — | ❌ **Hors périmètre** (non implémenté) |
| **G10** Gateway | Routage `/api/analytics/**` | Port 8088 | Partiel |

### Corrections d’intégration apportées (juin 2026)

- **G7** — branchement `envoyerStatusG8` sur changement de statut, ingestion GPS et suppression véhicule.
- **G8** — `AlertSender` : JWT jjwt 0.11.5, headers `Authorization` + `X-Source-Group`.
- **G5** — correction `Notification.type` null à la persistance (`ensureNotificationType`).
- **G8 consumer** — injection automatique `schemaVersion=1` et normalisation des champs hérités (`vehiculeId`, `retardMinutes`, etc.).

---

## 9. État d’avancement

### Finalisé

- [x] Pipeline d'ingestion REST (6 sources) avec validation batch.
- [x] Consommateurs Kafka pour G3, G7, G9 (+ batch G2).
- [x] Agrégations planifiées (6 domaines) + prédictions ML.
- [x] API REST consultation + rapports JSON.
- [x] Alertes seuils + circuit breaker Resilience4j vers G5.
- [x] Filtre JWT — routes sécurisées.
- [x] Stack Docker Compose : MongoDB, Kafka, ML, Prometheus, Grafana.
- [x] Intégration E2E **G2, G3, G5, G7, G9** — scripts PowerShell 100 % PASS.
- [x] **Démo live Kafka** — collection Postman + runbook (`G8_LIVE_DEMO.*`).

### En cours / perspectives

- [ ] Intégration **G1** billetterie (dépend de l’équipe G1).
- [ ] Intégration **G6** paiements (topic non publié dans ce dépôt).
- [ ] Alignement schémas `schemaVersion` > 1 avec les équipes.
- [ ] Export PDF des rapports.
- [ ] Optimisation agrégations sur gros volumes (index MongoDB).

### Difficultés rencontrées et résolues

- **Drift Kafka sans `schemaVersion`** — G8 injecte et normalise automatiquement.
- **G9** — seule l’annulation/clôture publie vers G8 (documenté dans la démo).
- **G5 500** — type de notification null corrigé côté G5.
- **Prometheus compose** — service `g9-service` retiré de la commande Prometheus (crash au démarrage).
- **JWT PowerShell** — corps batch G2 en tableau JSON explicite dans les scripts.

---

## 10. Structure du dépôt (référence)

```text
service-analytique/
├── src/main/java/ma/sgitu/g8/     # Application Spring Boot
├── src/test/java/                  # Tests JUnit
├── ml-service/                     # FastAPI (prédictions)
├── docs/                           # Rapport, Postman, guides démo
├── monitoring/grafana/             # Dashboards provisionnés
├── test-*.ps1                      # Scripts d'intégration E2E
├── docker-compose.yml              # (legacy local — préférer compose racine)
├── Dockerfile
└── pom.xml
```

---

*Document mis à jour le 7 juin 2026 — intégrations Kafka G2/G3/G7/G9 et démo live documentées.*
