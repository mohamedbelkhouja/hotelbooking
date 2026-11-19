# 🏨 HotelBooking - Mini-Projet Architecture SOA & Services Web

> **Système distribué de gestion et de réservation d'hôtel basé sur une architecture microservices polyglotte en protocoles : REST (OpenAPI 3), gRPC (Protobuf v3) et SOAP (Contract-First XSD/WSDL).**  
> *3ème année Licence Génie Logiciel et SI - Architecture SOA & Services Web.*

---

## 📑 Table des matières
1. [Vue d'ensemble et Architecture](#-vue-densemble-et-architecture)
2. [Découpage des Services et Justification](#-découpage-des-services-et-justification)
3. [Cycle de vie d'une réservation](#-cycle-de-vie-dune-réservation)
4. [Socle Technique & Prérequis](#-socle-technique--prérequis)
5. [Structure du Projet](#-structure-du-projet)
6. [Configuration et Lancement Rapide (Docker Compose)](#-configuration-et-lancement-rapide-docker-compose)
7. [Contrats d'interface & Spécifications](#-contrats-dinterface--spécifications)
   - [REST (OpenAPI 3)](#contrat-rest-openapi-3---reservation-service)
   - [gRPC (room.proto)](#contrat-grpc-roomproto---room-service)
   - [SOAP (billing.xsd & billing.wsdl)](#contrat-soap-billingxsd--billingwsdl---billing-service)
8. [Résilience & Gestion des Défaillances (Resilience4j)](#-résilience--gestion-des-défaillances-resilience4j)
9. [Scénario de Test de Bout en Bout](#-scénario-de-test-de-bout-en-bout)
10. [Endpoints & Outils de Diagnostic](#-endpoints--outils-de-diagnostic)

---

## 🏛 Vue d'ensemble et Architecture

Le système permet aux clients de consulter les chambres disponibles, de réserver, de suivre leurs réservations et de recevoir une facture officielle.  
La disponibilité est vérifiée en temps réel, le cycle de vie des réservations est strictement contrôlé, et chaque microservice possède sa propre base de données dédiée (**Database per Service**, aucune base partagée).

```
                      +-----------------------------+
                      |     Client Web / Mobile     |
                      +-----------------------------+
                                     | HTTP / JSON
                                     v
                      +-----------------------------+
                      |     api-gateway :8080       |
                      |  Routage, Rate Limiter      |
                      +-----------------------------+
                                     | REST /api/v1
                                     v
                      +-----------------------------+       +-------------------+
                      |  reservation-service :8081  | <---> |  DB reservations  |
                      |  REST OpenAPI /api/v1       |       |  PostgreSQL 16    |
                      +-----------------------------+       +-------------------+
                         /                       \
        gRPC + deadline /                         \ SOAP + Circuit Breaker
       + retry backoff /                           \ (Resilience4j)
                      v                             v
       +-------------------------+       +-------------------------+
       |   room-service :9090    |       |  billing-service :8083  |
       |   gRPC - Disponibilité  |       |  SOAP - Facturation     |
       +-------------------------+       +-------------------------+
                    |                                 |
                    v                                 v
         +--------------------+             +--------------------+
         |   DB chambres      |             |   DB factures      |
         |   PostgreSQL 16    |             |   MySQL 8.4        |
         +--------------------+             +--------------------+
```

---

## 🧩 Découpage des Services et Justification

| Service | Port | Responsabilités / Ressources | Protocole / Style | Justification | Base de Données |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **`api-gateway`** | `8080` | Point d'entrée unique, routage `/api/v1/**`, limitation de débit (*RequestRateLimiter* Redis). | Spring Cloud Gateway | Masque le découpage interne et protège l'infrastructure contre les surcharges. | Redis 7 (Cache / Rate limit) |
| **`reservation-service`** | `8081` | Gestion des clients et cycle de vie des réservations (`CREEE`, `CONFIRMEE`, `ANNULEE`). Orchestrateur métier. | REST (OpenAPI 3, `/api/v1`) | Exposé aux clients web et mobiles : JSON lisible, cache HTTP, API publique standard. | PostgreSQL 16 (`reservations`) |
| **`room-service`** | `9090` (gRPC)<br>`8082` (HTTP) | Catalogue des chambres, tarification et calcul de disponibilité en temps réel. | gRPC (`room.proto`) | Échanges internes fréquents et hautement performants : RPC unaire `CheckAvailability`, RPC streaming serveur `StreamAvailableRooms`. | PostgreSQL 16 (`chambres`) |
| **`billing-service`** | `8083` | Génération et consultation des factures comptables. | SOAP Contract-First (`billing.xsd`, `billing.wsdl`) | Simule l'interopérabilité avec un système comptable ou financier hérité imposant un contrat XML/SOAP strict avec SOAP Faults. | MySQL 8.4 (`factures`) |

---

## 🔄 Cycle de vie d'une réservation

Le cycle de vie d'une réservation est soumis aux règles de transition d'états suivantes :

| Transition | Déclencheur HTTP | Statut Résultat | Action déclenchée |
| :--- | :--- | :---: | :--- |
| **`CREEE → CONFIRMEE`** | `POST /api/v1/reservations/{id}/confirm` | `CONFIRMEE` | Appel SOAP vers `billing-service` pour générer la facture (statut facture `EMISE`). En cas de panne SOAP, repli avec statut facture `EN_ATTENTE`. |
| **`CREEE → ANNULEE`** | `DELETE /api/v1/reservations/{id}` | `ANNULEE` | Annulation logique de la réservation. |
| **`CONFIRMEE → ANNULEE`** | `DELETE /api/v1/reservations/{id}` | `ANNULEE` | Annulation logique d'une réservation confirmée. |
| **`Toute autre transition`** | Requête invalide (ex: modifier une annulée) | **409 Conflict** | Rejet automatique avec message explicite. |

> **Règle de modification (`PUT /api/v1/reservations/{id}`)** : Autorisée **exclusivement** lorsque la réservation est au statut `CREEE`. Toute tentative sur une réservation confirmée ou annulée renvoie un code **409 Conflict**.

---

## 🛠 Socle Technique & Prérequis

- **Java 17 LTS**
- **Spring Boot 3.3.5** & **Spring Cloud 2023.0.3**
- **Docker** et **Docker Compose v2+**
- **Maven 3.9+** (ou Maven Wrapper)
- **Bases de données** : PostgreSQL 16, MySQL 8.4
- **Cache / NoSQL** : Redis 7 (Alpine)
- **Migrations de schéma** : Flyway (intégré au démarrage de chaque service)
- **Résilience** : Resilience4j (Circuit Breaker, TimeLimiter)
- **CI / CD** : Workflow GitHub Actions (`.github/workflows/ci.yml`)

---

## 📁 Structure du Projet

```text
hotelbooking/
├── .env                                # Variables d'environnement pour bases de données et services
├── docker-compose.yml                  # Définition des 8 conteneurs (4 microservices + 3 DB + Redis)
├── pom.xml                             # Parent POM multi-modules Maven
├── test-scenario.ps1                   # Script automatisé de test de bout en bout (PowerShell)
├── test-scenario.sh                    # Script automatisé de test de bout en bout (Bash)
├── .github/
│   └── workflows/ci.yml                # Pipeline CI d'intégration continue
├── api-gateway/
│   ├── Dockerfile                      # Multi-stage build Docker
│   ├── pom.xml
│   └── src/main/
│       ├── java/tn/hotel/gateway/      # Spring Cloud Gateway + IP KeyResolver
│       └── resources/application.yml   # Routage /api/v1 et Redis RequestRateLimiter
├── reservation-service/
│   ├── Dockerfile
│   ├── openapi.yaml                    # Contrat OpenAPI 3 officiel
│   ├── pom.xml
│   └── src/main/
│       ├── proto/room.proto            # Contrat gRPC pour génération client
│       ├── resources/
│       │   ├── application.yml         # Config client gRPC (retry, deadline) + Resilience4j
│       │   ├── xsd/billing.xsd         # Contrat SOAP pour génération WebServiceTemplate
│       │   └── db/migration/V1__init_reservations.sql
│       └── java/tn/hotel/reservation/  # Contrôleurs REST, services métier, clients gRPC & SOAP
├── room-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── proto/room.proto            # Contrat gRPC (CheckAvailability & StreamAvailableRooms)
│       ├── resources/
│       │   ├── application.yml
│       │   └── db/migration/V1__init_rooms.sql # Seed des chambres (101, 102, 201, 301...)
│       └── java/tn/hotel/room/         # RoomGrpcService, JPA Entities & Repositories
└── billing-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── resources/
        │   ├── xsd/billing.xsd         # Schéma XML contrat-first (types Invoice, Fault)
        │   ├── wsdl/billing.wsdl       # Contrat WSDL officiel avec SOAP Fault
        │   ├── application.yml
        │   └── db/migration/V1__init_invoices.sql
        └── java/tn/hotel/billing/      # Endpoint Spring-WS, gestion SOAP Faults
```

---

## 🚀 Configuration et Lancement Rapide (Docker Compose)

### 1. Variables d'environnement (`.env`)
Le fichier `.env` est préconfiguré à la racine :
```env
# Reservation DB (PostgreSQL)
RESERVATION_DB_NAME=reservations
RESERVATION_DB_USER=reservation
RESERVATION_DB_PASSWORD=change-me

# Room DB (PostgreSQL)
ROOM_DB_NAME=chambres
ROOM_DB_USER=room
ROOM_DB_PASSWORD=change-me

# Billing DB (MySQL)
BILLING_DB_NAME=factures
BILLING_DB_USER=billing
BILLING_DB_PASSWORD=change-me
BILLING_DB_ROOT_PASSWORD=change-me-root
```

### 2. Démarrage complet en une seule commande
Exécutez dans le répertoire du projet :
```bash
docker compose up --build
```
Docker Compose construira les 4 images microservices avec leurs Dockerfiles multi-stage et démarrera l'ensemble des conteneurs avec vérification des *healthchecks* :
- `api-gateway` sur `http://localhost:8080`
- `reservation-service` sur `http://localhost:8081`
- `room-service` sur `http://localhost:8082` (Actuator) et port `9090` (gRPC)
- `billing-service` sur `http://localhost:8083` (SOAP `/ws`)
- `reservation-db` sur le port `5432`
- `room-db` sur le port `5433`
- `billing-db` sur le port `3306`
- `redis` sur le port `6379`

Pour arrêter le système :
```bash
docker compose down
```

---

## 📜 Contrats d'interface & Spécifications

### Contrat REST (OpenAPI 3) - `reservation-service`
Exposé via l'API Gateway : `http://localhost:8080/api/v1`

| Méthode | Chemin | Description | Codes Réponse |
| :--- | :--- | :--- | :--- |
| `GET` | `/rooms/available` | Consulter les chambres disponibles sur une période (`checkIn`, `checkOut`, `type`, `minCapacity`). Délègue à `room-service` via **gRPC StreamAvailableRooms**. | 200, 400, 503 |
| `POST` | `/clients` | Créer un client. | 201, 400, 409 |
| `GET` | `/clients/{clientId}` | Consulter un client par son UUID. | 200, 404 |
| `PUT` | `/clients/{clientId}` | Modifier les informations d'un client. | 200, 400, 404 |
| `GET` | `/clients/{clientId}/reservations` | Suivre les réservations d'un client (filtre optionnel `status`). | 200, 404 |
| `POST` | `/reservations` | Créer une réservation. Vérifie la disponibilité via **gRPC CheckAvailability** (deadline 2s, retry backoff). Statut initial `CREEE`. | 201, 400, 404, 409, 503 |
| `GET` | `/reservations/{id}` | Consulter une réservation. | 200, 404 |
| `PUT` | `/reservations/{id}` | Modifier dates / chambre (statut `CREEE` uniquement). | 200, 400, 404, 409 |
| `DELETE` | `/reservations/{id}` | Annuler une réservation (suppression logique, statut `ANNULEE`). | 200, 404, 409 |
| `POST` | `/reservations/{id}/confirm` | Confirmer la réservation et déclencher la facturation via **SOAP generateInvoice** (Circuit Breaker). | 200, 404, 409 |
| `GET` | `/reservations/{id}/invoice` | Obtenir la facture d'une réservation via **SOAP getInvoice**. | 200, 404 |

---

### Contrat gRPC (`room.proto`) - `room-service`
```protobuf
syntax = "proto3";
package hotel.room.v1;
option java_multiple_files = true;
option java_package = "tn.hotel.room.v1";

service RoomService {
  // RPC unaire
  rpc CheckAvailability (CheckAvailabilityRequest) returns (CheckAvailabilityResponse);
  // RPC streaming serveur
  rpc StreamAvailableRooms (AvailableRoomsRequest) returns (stream Room);
}
```

---

### Contrat SOAP (`billing.xsd` & `billing.wsdl`) - `billing-service`
Le service SOAP est accessible sur `http://localhost:8083/ws`.  
Le contrat WSDL est généré dynamiquement et disponible sur `http://localhost:8083/ws/billing.wsdl`.

Exemple d'enveloppe SOAP pour `generateInvoiceRequest` :
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:bil="http://hotel.tn/billing">
   <soapenv:Header/>
   <soapenv:Body>
      <bil:generateInvoiceRequest>
         <bil:reservationId>a0000000-0000-0000-0000-000000000001</bil:reservationId>
         <bil:clientId>c0000000-0000-0000-0000-000000000001</bil:clientId>
         <bil:nights>4</bil:nights>
         <bil:pricePerNight>120.00</bil:pricePerNight>
      </bil:generateInvoiceRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

En cas de demande d'une facture pour une réservation inexistante via `getInvoiceRequest`, le service génère un **SOAP Fault** typé :
```xml
<SOAP-ENV:Fault>
    <faultcode>SOAP-ENV:Client</faultcode>
    <faultstring>No invoice found for reservation ID: 99999999</faultstring>
    <detail>
        <bil:ReservationNotFoundFault xmlns:bil="http://hotel.tn/billing">
            <bil:reservationId>99999999</bil:reservationId>
        </bil:ReservationNotFoundFault>
    </detail>
</SOAP-ENV:Fault>
```

---

## 🛡 Résilience & Gestion des Défaillances (Resilience4j)

Lors de la confirmation d'une réservation (`POST /reservations/{id}/confirm`), l'appel SOAP vers `billing-service` est protégé par un **Circuit Breaker** et un **TimeLimiter** Resilience4j :

```yaml
resilience4j:
  circuitbreaker:
    instances:
      billing:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 3
  timelimiter:
    instances:
      billing:
        timeout-duration: 3s
```

### Mécanisme de Repli (Fallback)
Si le service de facturation SOAP est indisponible ou si le Circuit Breaker est ouvert :
1. La réservation n'est **pas perdue** : son statut passe bien à `CONFIRMEE`.
2. Son statut de facturation `invoiceStatus` est basculé sur `EN_ATTENTE`.
3. Une facture de repli temporaire est associée, prête à être régénérée une fois le service de facturation rétabli.

---

## 🧪 Scénario de Test de Bout en Bout

Deux scripts prêts à l'emploi sont fournis pour exécuter le scénario complet en direct :

### Sous Windows (PowerShell) :
```powershell
.\test-scenario.ps1
```

### Sous Linux / macOS / Git Bash :
```bash
chmod +x ./test-scenario.sh
./test-scenario.sh
```

### Étapes couvertes par le scénario de test :
1. **Consultation des chambres disponibles** via l'API Gateway (`REST -> gRPC StreamAvailableRooms`).
2. **Création d'un client** (`POST /api/v1/clients`).
3. **Consultation des informations du client**.
4. **Création d'une réservation** (`REST -> gRPC CheckAvailability` avec validation des dates et tarification automatique).
5. **Confirmation de la réservation** (`REST -> SOAP generateInvoice` avec Circuit Breaker).
6. **Récupération de la facture générée** (`REST -> SOAP getInvoice`).
7. **Consultation du tableau des réservations du client**.
8. **Annulation de la réservation** (`CONFIRMEE -> ANNULEE`).
9. **Test d'une transition interdite** : tentative de modification d'une réservation annulée $\rightarrow$ validation de la réponse HTTP **409 Conflict**.

---

## 🔎 Endpoints & Outils de Diagnostic

| Module / Outil | URL / Port | Description |
| :--- | :--- | :--- |
| **API Gateway** | `http://localhost:8080/api/v1` | Point d'entrée unique de toutes les requêtes |
| **Swagger UI (OpenAPI)** | `http://localhost:8081/swagger-ui.html` | Documentation interactive de l'API REST |
| **OpenAPI Spec (JSON)** | `http://localhost:8081/v3/api-docs` | Spécification OpenAPI 3 brute |
| **WSDL SOAP Billing** | `http://localhost:8083/ws/billing.wsdl` | Contrat WSDL publié par Spring-WS |
| **Actuator Gateway** | `http://localhost:8080/actuator/health` | État de santé de l'API Gateway |
| **Actuator Reservation**| `http://localhost:8081/actuator/health` | Santé, métriques et statut du Circuit Breaker |
| **Actuator Room** | `http://localhost:8082/actuator/health` | État de santé de Room Service |
| **Actuator Billing** | `http://localhost:8083/actuator/health` | État de santé de Billing Service |
| **Serveur gRPC Room** | `localhost:9090` | Endpoint binaire gRPC Protobuf |
