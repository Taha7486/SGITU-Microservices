# 📊 Rapport d'Avancement : Microservice Analytique (G8)

## 1. Présentation du Microservice
### Description du sous-système
Le **Service Analytique** est le "cerveau" décisionnel du système SGITU. Sa mission principale est de collecter, agréger et analyser les données provenant des autres microservices (Billetterie, Incidents, Suivi Véhicule, Abonnements) pour fournir des indicateurs de performance (KPI) et des prédictions intelligentes.

### Fonctionnalités métier implémentées
*   **Agrégation de données (Batch processing) :** Calcul automatique de statistiques (fréquentation, revenus, ponctualité, incidents) à partir des événements bruts.
*   **Système d'Alertes :** Surveillance en temps réel des seuils critiques (ex: chute de ponctualité < 80%, pic d'incidents > 10/jour).
*   **Prédictions ML :** Intégration avec un moteur de Machine Learning pour prédire les heures de pointe et les zones à risque d'incidents.
*   **Génération de Rapports :** Création de rapports consolidés sur des périodes spécifiques pour les gestionnaires.

---

## 2. Conception (API REST)
Le service expose une interface REST structurée pour consommer les statistiques calculées.

### Identification des endpoints
| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/dashboard` | Vue globale de tous les indicateurs. |
| `GET` | `/api/v1/analytics/trips/summary` | Statistiques de fréquentation et voyages. |
| `GET` | `/api/v1/analytics/revenue/summary` | Statistiques financières et revenus. |
| `GET` | `/api/v1/analytics/incidents/stats` | Analyse des incidents par type et zone. |
| `GET` | `/api/v1/analytics/vehicles/activity` | État de la flotte et ponctualité. |
| `POST` | `/api/v1/analytics/reports/generate` | Déclenche la génération d'un rapport PDF/JSON. |
| `GET` | `/api/v1/analytics/reports/{id}` | Récupère un rapport généré par son identifiant. |

### Choix techniques API
*   **Formats d’échange :** JSON (standardisé via DTO).
*   **Gestion des erreurs :** Codes HTTP standards (200 OK, 404 Not Found, 400 Bad Request).

---

## 3. Implémentation & Architecture
### Architecture en couches
Le projet suit une architecture modulaire pour assurer la séparation des préoccupations :
1.  **Controller :** Gestion des requêtes REST (`AnalyticsController`, `IngestionController`).
2.  **Service :** Orchestration de la logique métier (`AnalyticsService`, `MlPredictionService`).
3.  **Aggregation :** Modules spécialisés par domaine (Incident, Revenue, Vehicle...).
4.  **Repository :** Accès aux données MongoDB via Spring Data.
5.  **Scheduler :** Exécution cyclique (toutes les 60s) des tâches d'analyse.

### Technologies utilisées
*   **Backend Core :** Spring Boot 3.3.5 / Java 17.
*   **Persistence :** MongoDB (Stockage des événements et des snapshots).
*   **ML Integration :** Python FastAPI (Microservice externe).
*   **Documentation :** SpringDoc OpenAPI (Swagger).

---

## 4. Documentation API (Swagger/OpenAPI)
L'intégration de **SpringDoc OpenAPI** permet une documentation auto-générée et interactive.
*   **URL locale :** `http://localhost:8088/swagger-ui.html`
*   **Usage :** Permet de tester les requêtes en temps réel et de visualiser les modèles de données (JSON).

---

## 5. Tests
### Validation de la robustesse
*   **Tests Automatisés :** Suite de tests JUnit 5 couvrant les contrôleurs et les services (`AnalyticsControllerTest.java`).
*   **Tests via Postman :** Collection complète simulant des scénarios réels (Ingestion -> Agrégation -> Reporting).
*   **Gestion des cas d'erreur :** Tests sur les timeouts ML et les données manquantes dans MongoDB.

---

## 6. Conteneurisation (Docker)
Le microservice est packagé sous forme d'image Docker pour garantir la portabilité.
*   **Dockerfile :** Utilise une image JRE légère (`eclipse-temurin:17-jre-alpine`).
*   **Optimisation :** Build multi-étape possible pour réduire l'empreinte de l'image.

---

## 7. Orchestration (Docker Compose)
Le fichier `docker-compose.yml` définit l'écosystème complet :
*   **`g8-analytics-service`** : Le cœur de l'analyse (Java).
*   **`g8-ml-service`** : Le moteur de prédiction (Python/FastAPI).
*   **`g8-mongo`** : Base de données NoSQL.
*   **Interaction :** Réseau Docker isolé permettant aux services de communiquer par noms d'hôtes.

---

## 8. Sécurité
*   **Authentification JWT :** Intégration de Spring Security pour la validation des jetons porteurs (Bearer Tokens).
*   **Protection des endpoints :** Restriction des accès aux endpoints de reporting aux utilisateurs avec le rôle `ADMIN` ou `MANAGER`.
*   **Filtrage :** Sécurisation des communications entre microservices via l'API Gateway.

---

## 9. État d’avancement & Perspectives
### ✅ Finalisé
*   Pipeline d'agrégation complet pour tous les domaines.
*   Interface REST robuste avec documentation Swagger.
*   Intégration fonctionnelle avec le service ML Python.
*   Conteneurisation et orchestration Docker Compose.

### 🚧 En cours
*   Optimisation des performances sur les agrégations de gros volumes.
*   Finalisation de l'intégration avec le service de notification (G5).

### 🚩 Difficultés rencontrées
*   Complexité de la synchronisation des schémas de données entre le service Java et le service ML Python.
*   Gestion des cas limites lors de l'absence de données historiques pour les prédictions.
