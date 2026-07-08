# JavaIsland Bank - Backend

Backend per la gestione dei servizi bancari digitali. Spring Boot 3.4 + Spring Security + OAuth2 (Keycloak) + PostgreSQL.

---

## Indice

1. [Architettura](#1-architettura)
2. [Struttura del Progetto](#2-struttura-del-progetto)
3. [Schema Database](#3-schema-database)
4. [Prerequisiti](#4-prerequisiti)
5. [Avvio dell'Ambiente Locale](#5-avvio-dellambiente-locale)
6. [API Documentation (Swagger)](#6-api-documentation-swagger)
7. [Sicurezza e Ruoli](#7-sicurezza-e-ruoli)
8. [Feature Overview](#8-feature-overview)
9. [Test delle API](#9-test-delle-api)
10. [Contributori](#10-contributori)

---

## 1. Architettura

- **Monolite** Spring Boot con esposizione REST.
- **Autenticazione e Autorizzazione**: OAuth2 / JWT tramite Keycloak. Ruoli: `CUSTOMER`, `EMPLOYEE`.
- **Database**: PostgreSQL 15 con schema normalizzato e tabelle di dominio (lookup).
- **Documentazione API**: Springdoc OpenAPI (Swagger UI).
- **Organizzazione**: Screaming Architecture — package grouping per feature di dominio (`user`, `card`, `account`, `transaction`, `request`), non per layer tecnico.

### Decisioni Architetturali

| Scelta | Motivazione |
|---|---|
| **Screaming Architecture** | La struttura riflette il dominio bancario. Ogni package contiene entità, repository, servizi, DTO e controller della propria feature. |
| **Tabelle di Dominio (Lookup)** | Stati e tipologie sono archiviati in tabelle dedicate (`user_statuses`, `role_types`, `card_statuses`, `card_types`, ecc.) e referenziati via FK. Questo garantisce integrità referenziale, evita duplicazione di stringhe e permette future estensioni senza modifiche al codice Java. |
| **DataInitializer** | Seed automatico delle tabelle di dominio al primo avvio. Se i dati sono già presenti, l'inizializzazione viene saltata (idempotente). |
| **Configurazione Unificata** | Singolo `application.yml` strutturato ad albero. |
| **DTO con Validazione** | Ogni endpoint accetta DTO dedicati con vincoli `jakarta.validation`. Protezione da input malformati. |
| **GlobalExceptionHandler** | Gestione centralizzata di `ApiBankException` (business) e `MethodArgumentNotValidException` (validazione). |

---

## 2. Struttura del Progetto

```
bank-backend/
├── src/main/java/com/javaisland/bank_backend/
│   ├── BankBackendApplication.java          # Entry point Spring Boot
│   ├── account/                             # Feature: Conti e Limiti
│   │   ├── AccountStatus.java               # Entità tabella account_statuses
│   │   ├── AccountStatusRepository.java
│   │   ├── LimitType.java                   # Entità tabella limit_types
│   │   └── LimitTypeRepository.java
│   ├── authentication/                      # Security JWT
│   │   └── KeycloakRoleConverter.java       # Mappa ruoli Keycloak → Spring Security
│   ├── card/                                # Feature: Carte
│   │   ├── Card.java                        # Entità cards
│   │   ├── CardController.java
│   │   ├── CardIssueDTO.java
│   │   ├── CardRepository.java
│   │   ├── CardService.java
│   │   ├── CardStatus.java                  # Entità tabella card_statuses
│   │   ├── CardStatusRepository.java
│   │   ├── CardType.java                    # Entità tabella card_types
│   │   └── CardTypeRepository.java
│   ├── config/                              # Configurazioni
│   │   ├── DataInitializer.java             # Seed dati tabelle di dominio
│   │   └── SecurityConfig.java              # Spring Security + OAuth2 + Swagger
│   ├── exception/                           # Gestione errori
│   │   ├── ApiBankException.java            # Eccezione custom
│   │   └── GlobalExceptionHandler.java      # @RestControllerAdvice
│   ├── request/                             # Feature: Richieste e Prestiti
│   │   ├── AccountClosureDTO.java
│   │   ├── BankRequest.java                 # Entità requests
│   │   ├── BankRequestRepository.java
│   │   ├── CardBlockDTO.java
│   │   ├── GenericRequestDTO.java
│   │   ├── Loan.java                        # Entità loans
│   │   ├── LoanApplyDTO.java
│   │   ├── LoanRepository.java
│   │   ├── ProcessRequestDTO.java
│   │   ├── RequestController.java
│   │   └── RequestService.java
│   ├── transaction/                         # Feature: Transazioni
│   │   ├── TransactionStatus.java           # Entità tabella transaction_statuses
│   │   ├── TransactionStatusRepository.java
│   │   ├── TransactionType.java             # Entità tabella transaction_types
│   │   └── TransactionTypeRepository.java
│   └── user/                                # Feature: Utenti
│       ├── RoleType.java                    # Entità tabella role_types
│       ├── RoleTypeRepository.java
│       ├── User.java                        # Entità users
│       ├── UserController.java
│       ├── UserRegisterDTO.java
│       ├── UserRepository.java
│       ├── UserService.java
│       ├── UserStatus.java                  # Entità tabella user_statuses
│       └── UserStatusRepository.java
└── src/main/resources/
    └── application.yml                      # Configurazione applicativa
```

---

## 3. Schema Database

### Tabelle di Dominio (Lookup)

Vengono popolate automaticamente da `DataInitializer` al primo avvio.

| Tabella | Valori Seed |
|---|---|
| `user_statuses` | PENDING, ACTIVE, ANNULED, SUSPENDED |
| `role_types` | CUSTOMER, EMPLOYEE |
| `account_statuses` | INACTIVE, ACTIVE, FROZEN, CLOSED |
| `card_statuses` | INACTIVE, ACTIVE, BLOCKED |
| `card_types` | DEBIT, CREDIT |
| `limit_types` | DAILY_TRANSFER, MONTHLY_TRANSFER, ATM_WITHDRAWAL, POS_SPENDING |
| `transaction_types` | DEPOSIT, WITHDRAWAL, TRANSFER, INITIAL_TRANSFER |
| `transaction_statuses` | PENDING, COMPLETED, FAILED, REJECTED |

### Tabelle Principali

| Tabella | Descrizione |
|---|---|
| `users` | Correntisti e dipendenti. Riferisce `user_statuses` e `role_types`. |
| `accounts` | Conti correnti. Riferisce `account_statuses`. |
| `account_limits` | Soglie massime per operazioni. Riferisce `limit_types`. |
| `cards` | Carte di debito/credito. Riferisce `card_statuses` e `card_types`. |
| `saved_beneficiaries` | Rubrica IBAN dei correntisti. |
| `transactions` | Movimenti contabili. Riferisce `transaction_types` e `transaction_statuses`. |
| `requests` | Richieste amministrative (chiusura conto, blocco carta, ecc.). |
| `loans` | Richieste di finanziamento. |

---

## 4. Prerequisiti

- **Java 21+**
- **Maven 3.9+** (incluso wrapper `mvnw`)
- **Docker Desktop** (per PostgreSQL e Keycloak)
- **PostgreSQL 15** (containerizzato)

---

## 5. Avvio dell'Ambiente Locale

### 5.1. Avvia i servizi Docker

```bash
docker compose up -d
```

Avvia PostgreSQL (porta `5432`) e Keycloak (porta `8080`).

### 5.2. Configura Keycloak

1. Accedi alla Admin Console: `http://localhost:8080` (admin/admin).
2. Crea un **realm** chiamato `javaisland-realm`.
3. Crea un **client**:
   - Client ID: `bank-backend`
   - Client authentication: `OFF`
   - Standard flow: `OFF`
   - Direct access grants: `ON`
   - Valid redirect URIs: `http://localhost:8081/*`
4. Crea **ruoli realm**: `CUSTOMER`, `EMPLOYEE`.
5. Crea un utente di test (es. `impiegato_test`), assegna password e ruolo `EMPLOYEE`.

### 5.3. Avvia l'applicazione

```bash
./mvnw spring-boot:run
```

L'applicazione parte sulla porta `8081`. Le tabelle di dominio vengono popolate automaticamente al primo avvio.

---

## 6. API Documentation (Swagger)

- **UI**: [`http://localhost:8081/api/swagger-ui.html`](http://localhost:8081/api/swagger-ui.html)
- **JSON**: [`http://localhost:8081/api/v3/api-docs`](http://localhost:8081/api/v3/api-docs)

### Endpoint Pubblici

| Metodo | Path | Descrizione |
|---|---|---|
| `POST` | `/api/users/register` | Registrazione nuova utenza |

### Endpoint per CUSTOMER

| Metodo | Path | Descrizione |
|---|---|---|
| `POST` | `/api/requests` | Crea richiesta generica |
| `POST` | `/api/requests/closure` | Richiesta chiusura conto |
| `POST` | `/api/requests/card-block` | Richiesta blocco carta |
| `POST` | `/api/requests/loans` | Richiesta prestito |

### Endpoint per EMPLOYEE

| Metodo | Path | Descrizione |
|---|---|---|
| `POST` | `/api/cards/issue` | Emissione nuova carta |
| `PATCH` | `/api/cards/{cardId}/status` | Cambio stato carta |
| `PATCH` | `/api/requests/{requestId}/review` | Review rapida richiesta |
| `POST` | `/api/requests/process` | Processo avanzato richiesta |
| `GET` | `/api/users/export` | Esporta correntisti ordinati su file |

---

## 7. Sicurezza e Ruoli

- **Autenticazione**: OAuth2 Password Grant via Keycloak.
- **Autorizzazione**: `@PreAuthorize("hasRole('CUSTOMER')")` / `hasRole('EMPLOYEE')` sui metodi dei controller.
- **Registrazione**: Endpoint pubblico (`POST /api/users/register`). L'utente viene creato con stato `PENDING`. Un dipendente deve convalidare la registrazione per attivare il conto.
- **Endpoint Swagger**: Accesso pubblico (nessun token richiesto per la UI).

---

## 8. Feature Overview

### 8.1. Gestione Utenti (Customer Operations)

- **Registrazione**: Crea utenza con stato `PENDING`, ruolo `CUSTOMER`.
- **Convalida**: Il dipendente approva la richiesta di registrazione → conto attivo.
- **Esportazione**: Stampa su file tutti i correntisti ordinati per nome+cognome.

### 8.2. Richieste e Prestiti

- Richieste generiche, chiusura conto, blocco carta.
- Richiesta prestito con calcolo rata mensile (interesse semplice).
- Workflow di approvazione: PENDING → COMPLETED / REJECTED.

### 8.3. Carte

- Emissione carta DEBIT/CREDIT con generazione numero 16 cifre, CVV, scadenza 5 anni.
- Vincolo: `holderName` solo lettere e spazi.
- Blocco/blocco definitivo tramite richiesta.

### 8.4. Conti e Transazioni (Core Banking)

- Operazioni di versamento, prelievo, bonifico.
- Verifica saldo e limiti.
- Storico movimenti con saldo progressivo.

---

## 9. Test delle API

### Autenticazione in Swagger

1. Apri `http://localhost:8081/api/swagger-ui.html`.
2. Clicca **Authorize**.
3. Inserisci:
   - **username**: `impiegato_test`
   - **password**: `(password impostata su Keycloak)`
   - **client_id**: `bank-backend`
4. Clicca **Authorize** → **Close**.

### Emissione Carta (Test)

```json
POST /api/cards/issue
{
  "accountId": 1,
  "holderName": "Mario Rossi",
  "cardType": "DEBIT"
}
```

### Registrazione Utente (Test)

```json
POST /api/users/register
{
  "keycloakId": "uuid-da-keycloak",
  "username": "mario.rossi",
  "firstName": "Mario",
  "lastName": "Rossi",
  "birthDate": "1990-01-15",
  "email": "mario.rossi@example.com"
}
```

### Richiesta Prestito (Test)

```json
POST /api/requests/loans
{
  "userId": 1,
  "amount": 10000.00,
  "annualRate": 5.5,
  "months": 24,
  "accountId": 1
}
```

---

## 10. Contributori

- **Collaboratore A (Core Banking)**: Flussi finanziari, saldi, transazioni, rubrica beneficiari.
- **Collaboratore B (Customer Operations)**: Anagrafica utenti, richieste amministrative, carte, prestiti.

Per dettagli sulle singole funzionalità, consultare la documentazione Swagger o i Javadoc presenti nel codice.
