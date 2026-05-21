# Alignement des rôles G3 ↔ G4 ↔ G10

Les rôles sont **définis et stockés dans G3** (`service-utilisateur`). G10 émet le JWT avec la liste `roles` (ex. `ROLE_OPERATOR`). G4 **valide le même JWT** et applique les règles ci-dessous.

## Catalogue G3 (source de vérité)

| Rôle G3 | Usage typique | Accès G4 |
|---------|---------------|----------|
| `ROLE_PASSENGER` | Client / voyageur (G1, G2) | Aucun (back-office G4) |
| `ROLE_STUDENT` | Étudiant (G2) | Aucun |
| `ROLE_DRIVER` | Conducteur (référence `chauffeurId`) | Lecture seule possible via gateway ; pas de CRUD réseau/flotte |
| `ROLE_OPERATOR` | Gestionnaire **réseau** (lignes, trajets, arrêts, horaires) | CRUD offre + lecture |
| `ROLE_DISPATCHER` | Gestionnaire **flotte** (missions, affectations, événements) | CRUD flotte + lecture |
| `ROLE_TECHNICIAN` | Technicien incidents (G9) | Pas d’écriture G4 par défaut |
| `ROLE_ADMIN` | Administrateur **global** SI | Tous droits G4 |
| `ROLE_ADMIN_G1` … `ROLE_ADMIN_G9` | Admin **par microservice** | `ROLE_ADMIN_G4` = admin technique G4 |
| `ROLE_ADMIN_G4` | Admin dédié coordination transports | Supervision + tous droits G4 |

> Anciens rôles locaux G4 (`G4_OPERATOR`, `G4_DISPATCHER`, `G4_ADMIN`) : **remplacés** par les rôles G3 ci-dessus.

## Correspondance acteurs démo G4 (login local `/api/auth/login`)

| Compte démo | Mot de passe | Rôle G3 dans le JWT |
|-------------|--------------|---------------------|
| `gestionnaire.reseau` | `password` | `ROLE_OPERATOR` |
| `gestionnaire.flotte` | `password` | `ROLE_DISPATCHER` |
| `admin.technique` | `password` | `ROLE_ADMIN_G4` |
| `g10.integration` | `password` | `ROLE_ADMIN` (passerelle / intégration) |

## Matrice des endpoints G4

| Périmètre | Rôles autorisés |
|-----------|-----------------|
| Lecture `GET /api/g4/**`, `GET /api/v1/**` | `OPERATOR`, `DISPATCHER`, `ADMIN_G4`, `ADMIN` |
| CRUD lignes / trajets / arrêts / horaires | `OPERATOR`, `ADMIN_G4`, `ADMIN` |
| CRUD missions / affectations / events / notifications | `DISPATCHER`, `ADMIN_G4`, `ADMIN` |
| Supervision `GET /api/v1/operator/status` | `ADMIN_G4`, `ADMIN` |
| Public sans token | `/api/auth/login`, `/api/g4/health`, **`/api/g4/logs`**, Swagger, `GET /actuator/health` |

## Chaîne JWT (avant certificats / prod)

1. **G3** : création utilisateur + attribution des rôles (`ROLE_*` en base).
2. **G10** : login → JWT signé, claim `roles` = liste exacte G3 (`ROLE_OPERATOR`, …).
3. **G4** : filtre JWT → `hasRole("OPERATOR")` etc. (Spring ajoute le préfixe `ROLE_`).

Secret JWT : **même valeur** que G3/G10 (`jwt.secret` dans `application.yml`).

## Évolution

- Validation **live** du `chauffeurId` via `GET /users/{id}/exists` (G3) à la création de mission.
- Comptes démo in-memory : **dev uniquement** ; en intégration, token émis par G10 uniquement.
