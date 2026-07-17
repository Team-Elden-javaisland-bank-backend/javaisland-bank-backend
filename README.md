# EldenBank — Backend

API REST per gestione servizi bancari digitali.

**Spring Boot 3.4** · **Spring Security 6** · **Keycloak JWT** · **PostgreSQL 15** · **Springdoc OpenAPI 2.8**

---

## Indice

1. [Architettura](#1-architettura)
2. [Stack Tecnologico](#2-stack-tecnologico)
3. [Struttura Progetto](#3-struttura-progetto)
4. [Schema Database](#4-schema-database)
5. [Prerequisiti](#5-prerequisiti)
6. [Avvio Locale](#6-avvio-locale)
7. [Autenticazione](#7-autenticazione)
8. [API Endpoints](#8-api-endpoints)
9. [Gestione Errori](#9-gestione-errori)
10. [Feature Overview](#10-feature-overview)
11. [Esempi Test](#11-esempi-test)

---

## 1. Architettura

- **Monolite** Spring Boot con esposizione REST
- **Autenticazione**: JWT Bearer tramite Keycloak (OAuth2 Resource Server)
- **Database**: PostgreSQL 15 con schema normalizzato (tabelle lookup referenziate via FK)
- **Documentazione API**: Springdoc OpenAPI 2.8 (Swagger UI integrata)
- **Organizzazione**: Screaming Architecture — package per dominio (`account`, `user`, `card`, `transaction`)
- **Container**: Docker Compose per PostgreSQL e Keycloak

### Decisioni Architetturali

| Scelta | Motivazione |
|---|---|
| Screaming Architecture | Ogni feature ha model, repository, service, DTO e controller propri. Riflette il dominio bancario. |
| Tabelle di Dominio (Lookup) | Stati e tipologie su tabelle dedicate referenziate via FK. Integrità referenziale, no stringhe duplicate, estensibile senza modificare codice Java. |
| DataInitializer | Seed automatico tabelle di dominio al primo avvio. Idempotente (conta righe prima di inserire). |
| JWT via Keycloak | OAuth2 standard, JWK rotation automatica, SSO-ready, ruoli mappati su realm roles. |
| GlobalExceptionHandler | Gestione centralizzata errori business (`ApiBankException`), validazione (`MethodArgumentNotValidException`), vincoli JPA e fallback generico. |
| DTO con Validazione | Ogni endpoint usa DTO dedicati con vincoli `jakarta.validation`. Nessuna entity esposta direttamente. |
| Lombok | Riduzione boilerplate (getter, setter, builder, costruttori). |

---

## 2. Stack Tecnologico

| Componente | Tecnologia | Versione |
|---|---|---|
| Framework | Spring Boot | 3.4.2 |
| Language | Java | 21+ |
| Security | Spring Security + OAuth2 Resource Server | 6.4.2 |
| Identity Provider | Keycloak | 24.0 |
| ORM | Spring Data JPA + Hibernate | 6.6.5 |
| Database | PostgreSQL | 15 (Alpine) |
| Validation | Jakarta Validation | 3.0.2 |
| API Docs | Springdoc OpenAPI | 2.8.5 |
| Utilities | Lombok | 1.18.36 |
| Container | Docker Compose | 3.8 |

---

## 3. Struttura Progetto

```
bank-backend/
├── docker-compose.yml                  # PostgreSQL + Keycloak
├── pom.xml                             # Maven dependencies
├── mvnw / mvnw.cmd                     # Maven Wrapper
├── database-schema.txt                 # Schema DB in formato DBML
├── endpoints.txt                       # Elenco API endpoints
├── admin-jwt.txt                       # JWT pre-generato per admin
│
└── src/main/java/com/javaisland/bank_backend/
    ├── BankBackendApplication.java
    │
    ├── account/                         # Conti correnti
    │   ├── controller/
    │   │   └── AccountController.java
    │   ├── dto/
    │   │   ├── AccountLimitResponseDto.java
    │   │   ├── AccountResponseDto.java
    │   │   ├── CloseAccountRequestDto.java
    │   │   ├── OpenAccountRequestDto.java
    │   │   └── SetLimitRequestDto.java
    │   ├── model/
    │   │   ├── Account.java
    │   │   ├── AccountLimit.java
    │   │   ├── AccountStatus.java
    │   │   └── LimitType.java
    │   ├── repository/
    │   │   ├── AccountLimitRepository.java
    │   │   ├── AccountRepository.java
    │   │   ├── AccountStatusRepository.java
    │   │   └── LimitTypeRepository.java
    │   └── service/
    │       ├── AccountLimitService.java
    │       └── AccountService.java
    │
    ├── auth/                            # Autenticazione
    │   ├── controller/
    │   │   └── AuthController.java
    │   ├── dto/
    │   │   ├── LoginRequestDto.java
    │   │   ├── LoginResponseDto.java
    │   │   └── RegisterRequestDto.java
    │   └── service/
    │       ├── KeycloakAdminService.java
    │       └── RegistrationService.java
    │
    ├── beneficiary/                     # Rubrica beneficiari
    │   ├── controller/
    │   │   └── BeneficiaryController.java
    │   ├── dto/
    │   │   ├── BeneficiaryRequestDto.java
    │   │   └── BeneficiaryResponseDto.java
    │   ├── model/
    │   │   └── Beneficiary.java
    │   ├── repository/
    │   │   └── BeneficiaryRepository.java
    │   └── service/
    │       └── BeneficiaryService.java
    │
    ├── card/                            # Carte
    │   ├── controller/
    │   │   ├── CardController.java
    │   │   ├── CustomerCardController.java
    │   │   └── EmployeeCardController.java
    │   ├── dto/
    │   │   └── CardResponseDto.java
    │   ├── model/
    │   │   ├── Card.java
    │   │   ├── CardStatus.java
    │   │   └── CardType.java
    │   ├── repository/
    │   │   ├── CardRepository.java
    │   │   ├── CardStatusRepository.java
    │   │   └── CardTypeRepository.java
    │   └── service/
    │       └── CardService.java
    │
    ├── common/
    │   └── PageResponseDto.java         # Wrapper paginazione generico
    │
    ├── config/
    │   ├── DataInitializer.java         # Seed tabelle di dominio
    │   └── OpenAPIConfig.java           # Swagger/OpenAPI config
    │
    ├── employee/                        # Endpoint dipendenti
    │   └── controller/
    │       ├── EmployeeAccountController.java
    │       └── EmployeeUserController.java
    │
    ├── exception/                       # Error handling
    │   ├── ApiBankException.java
    │   └── GlobalExceptionHandler.java
    │
    ├── security/                        # Sicurezza JWT
    │   ├── AppRoleConverter.java
    │   └── SecurityConfig.java
    │
    ├── transaction/                     # Transazioni
    │   ├── controller/
    │   │   └── TransactionController.java
    │   ├── dto/
    │   │   ├── TransactionRequestDto.java
    │   │   ├── TransactionResponseDto.java
    │   │   └── TransferRequestDto.java
    │   ├── model/
    │   │   ├── Transaction.java
    │   │   ├── TransactionStatus.java
    │   │   └── TransactionType.java
    │   ├── repository/
    │   │   ├── TransactionRepository.java
    │   │   ├── TransactionSpecifications.java
    │   │   ├── TransactionStatusRepository.java
    │   │   └── TransactionTypeRepository.java
    │   └── service/
    │       └── TransactionService.java
    │
    ├── user/                            # Utenti
    │   ├── controller/
    │   │   └── UserController.java
    │   ├── dto/
    │   │   ├── CustomerListItemDto.java
    │   │   ├── PendingRegistrationDto.java
    │   │   └── UserResponseDto.java
    │   ├── model/
    │   │   ├── RoleType.java
    │   │   ├── User.java
    │   │   └── UserStatus.java
    │   ├── repository/
    │   │   ├── RoleTypeRepository.java
    │   │   ├── UserRepository.java
    │   │   └── UserStatusRepository.java
    │   └── service/
    │       └── UserService.java
    │
    └── validation/
        ├── Adult.java                   # Validazione anagrafica maggiorenne
        └── AdultValidator.java
```

---

## 4. Schema Database

### Tabelle di Dominio (Lookup)

Popolate automaticamente da `DataInitializer` al primo avvio.
Schema completo in [database-schema.txt](database-schema.txt).

| Tabella | Valori Seed |
|---|---|
| `user_statuses` | PENDING, ACTIVE, ANNULLED, SUSPENDED |
| `role_types` | C (customer), D (dipendente) |
| `account_statuses` | INACTIVE, ACTIVE, FROZEN, CLOSED |
| `card_statuses` | INACTIVE, ACTIVE, BLOCKED |
| `card_types` | DEBIT, CREDIT |
| `limit_types` | DAILY_TRANSFER, SINGLE_TRANSFER, INSTANT_TRANSFER_SINGLE, MONTHLY_TRANSFER, ATM_WITHDRAWAL, POS_SPENDING |
| `transaction_types` | DEPOSIT, WITHDRAWAL, TRANSFER, INITIAL_TRANSFER |
| `transaction_statuses` | PENDING, COMPLETED, FAILED, REJECTED |

### Tabelle Principali

| Tabella | Descrizione |
|---|---|
| `users` | Correntisti e dipendenti. FK → `user_statuses`, `role_types` |
| `accounts` | Conti correnti. FK → `users` |
| `account_limits` | Massimali operativi per conto. FK → `accounts`, `limit_types`, unique(account_id, limit_type_id) |
| `cards` | Carte di debito/credito. FK → `card_statuses`, `card_types` |
| `beneficiaries` | Rubrica beneficiari per bonifici. FK → `users`, unique(user_id, destination_account_number) |
| `transactions` | Movimenti contabili. FK → `transaction_types`, `transaction_statuses` |

---

## 5. Prerequisiti

- **Java 21+** (JDK)
- **Maven 3.9+** (wrapper `mvnw` incluso)
- **Docker Desktop** (per PostgreSQL e Keycloak)

---

## 6. Avvio Locale

### 6.1. Avvia infrastruttura

```bash
docker compose up -d
```

Avvia:
- **PostgreSQL** su `localhost:5432` (db: `javaisland_backend`, user: `bank_admin`, password: `bank_password`)
- **Keycloak** su `localhost:8080` (admin / admin)

### 6.2. Importa realm Keycloak

1. Accedi a `http://localhost:8080` (admin / admin)
2. **Create Realm** → nome: `javaisland-realm`
3. **Clients** → **Create client**:
   - Client ID: `bank-backend`
   - Client authentication: `OFF`
   - Standard flow: `OFF`
   - Direct access grants: `ON`
4. **Realm roles** → Create: `C`, `D`
5. Crea utenti e mappa i ruoli

### 6.3. Avvia applicazione

```bash
./mvnw spring-boot:run
```

Applicazione su **`http://localhost:8081`**.

Al primo avvio: tabelle di dominio create e popolate automaticamente.

---

## 7. Autenticazione

JWT Bearer via Keycloak OAuth2 Direct Access Grant.

### Flow

```
Client → POST /api/v1/auth/keycloak-login → Keycloak → access_token
Client → Bearer token → App → Keycloak JWK → validazione → ruoli
```

### Login

```http
POST /api/v1/auth/keycloak-login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

### Risposta

```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "role": "D",
  "userId": 1,
  "firstName": "Admin",
  "lastName": "Bank",
  "email": "admin@javaisland.com"
}
```

### Ruoli

| Ruolo | Descrizione | Endpoint |
|---|---|---|
| `C` | Customer (correntista) | Account, Transazioni, Carte (lettura) |
| `D` | Employee (dipendente) | Gestione utenti, conti, carte, admin |

### Configurazione

```yaml
app:
  jwt:
    issuer-uri: http://localhost:8080/realms/javaisland-realm

keycloak:
  realm: javaisland-realm
  auth-server-url: http://localhost:8080
  client-id: bank-backend
  admin-username: admin
  admin-password: admin
```

Variabili d'ambiente disponibili: `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_AUTH_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD`.

---

## 8. API Endpoints

Totale: **36 endpoints** (2 pubblici, 15 customer, 18 employee, 1 export)

### Pubblici — `/api/v1/auth`

| Metodo | Path | Descrizione |
|---|---|---|
| POST | `/register` | Registrazione nuovo utente (stato PENDING) |
| POST | `/keycloak-login` | Login Keycloak, ritorna JWT + profilo |

### Customer — Account — `/api/v1/customer/accounts` `[C]`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/` | Lista tutti i miei conti |
| POST | `/open` | Apri conto aggiuntivo (con bonifico iniziale) |
| POST | `/closure-request` | Richiedi chiusura conto |
| GET | `/{accountNumber}` | Dettaglio conto (balance, stato, data) |
| GET | `/{accountNumber}/limits` | Visualizza massimali del conto |

### Customer — Transazioni — `/api/v1/customer/transactions` `[C]`

| Metodo | Path | Descrizione |
|---|---|---|
| POST | `/deposit` | Versamento su proprio conto |
| POST | `/withdraw` | Prelievo da proprio conto |
| POST | `/transfer` | Bonifico verso altro conto (o beneficiario salvato) |
| GET | `/recent/{accountNumber}` | Ultime 10 transazioni |
| GET | `/all?start=&end=&page=&size=` | Storico paginato con filtri data |

### Customer — Carte — `/api/v1/customer/cards` `[C]`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/` | Lista tutte le mie carte |
| GET | `/{cardId}` | Dettaglio carta |

### Customer — Beneficiari — `/api/v1/customer/beneficiaries` `[C]`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/` | Lista miei beneficiari salvati |
| POST | `/` | Salva nuovo beneficiario (nickname + IBAN) |
| DELETE | `/{id}` | Rimuovi beneficiario |

### Employee — Account — `/api/v1/employee/accounts` `[D]`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/` | Lista tutti i conti (`?status=` opzionale) |
| GET | `/user/{userId}` | Conti per utente specifico |
| GET | `/{accountNumber}` | Dettaglio conto |
| PUT | `/{accountNumber}/activate` | Attiva conto (INACTIVE → ACTIVE) |
| PUT | `/{accountNumber}/freeze` | Congela conto (ACTIVE → FROZEN) |
| PUT | `/{accountNumber}/closure/validate` | Valida chiusura (FROZEN → CLOSED) |
| PUT | `/{accountNumber}/closure/reject` | Rifiuta chiusura (FROZEN → ACTIVE) |
| GET | `/{accountNumber}/limits` | Visualizza massimali del conto |
| PUT | `/{accountNumber}/limits/{limitType}` | Imposta massimale (es. DAILY_TRANSFER, SINGLE_TRANSFER) |

### Employee — Utenti — `/api/v1/employee/users` `[D]`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/registrations/pending` | Registrazioni in attesa di validazione |
| PUT | `/registrations/{userId}/validate` | Valida registrazione + attiva conto + emetti carta |
| PUT | `/registrations/{userId}/reject` | Rifiuta registrazione |
| GET | `/customers` | Lista tutti i clienti ordinata per nome |

### Employee — Carte — `/api/v1/employee` `[D]`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/cards` | Lista tutte le carte del sistema |
| GET | `/cards/{cardId}` | Dettaglio carta |
| GET | `/accounts/{accountNumber}/cards` | Carte associate a un conto |

### Admin — `/api/cards` `[D]`

| Metodo | Path | Descrizione |
|---|---|---|
| PATCH | `/{cardId}/status?status=` | Aggiorna stato carta (ACTIVE/INACTIVE/BLOCKED) |

### Export — `/api/users`

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/export?filePath=` | Esporta anagrafica clienti su file |

---

## 9. Gestione Errori

Formato unico risposta errori:

```json
{
  "timestamp": "2026-07-09T17:00:00.000",
  "status": 400,
  "errorCode": "FORBIDDEN",
  "message": "Account IT... does not belong to the current user."
}
```

### Errori Business

| Error Code | HTTP | Descrizione |
|---|---|---|
| `USER_NOT_FOUND` | 400 | Utente non trovato |
| `ACCOUNT_NOT_FOUND` | 400 | Conto inesistente |
| `ACCOUNT_INACTIVE` | 400 | Account non attivo |
| `INVALID_ACCOUNT_STATE` | 400 | Operazione non permessa sullo stato attuale |
| `INSUFFICIENT_FUNDS` | 400 | Saldo insufficiente |
| `LIMIT_EXCEEDED` | 400 | Superato massimale operativo (giornaliero, mensile, singolo) |
| `NON_ZERO_BALANCE` | 400 | Chiusura conto con saldo non zero |
| `FORBIDDEN` | 400 | Conto non appartiene all'utente |
| `EMAIL_ALREADY_REGISTERED` | 400 | Email già registrata |
| `INVALID_CREDENTIALS` | 400 | Credenziali non valide |
| `TRANSACTION_TYPE_NOT_FOUND` | 400 | Tipo transazione non configurato |
| `TRANSACTION_STATUS_NOT_FOUND` | 400 | Stato transazione non configurato |

### Errori Validazione

| Error Code | HTTP | Descrizione |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Violazione vincoli `jakarta.validation` sui campi DTO |

### Errori Tecnici

| Error Code | HTTP | Descrizione |
|---|---|---|
| `INTERNAL_ERROR` | 500 | Errore imprevisto (nessun dettaglio esposto al client) |

---

## 10. Feature Overview

### 10.1. Registrazione e Onboarding

1. Customer si registra → stato `PENDING`, conto `INACTIVE`, carta non emessa
2. Employee valida registrazione → stato `ACTIVE`, conto `ACTIVE`, carta DEBIT `ACTIVE` emessa automaticamente
3. Employee può rifiutare o annullare la registrazione

### 10.2. Conti Correnti

- Conto iniziale creato automaticamente alla registrazione
- Apertura conti aggiuntivi (bonifico da conto esistente)
- Richiesta chiusura → conto congelato → employee valida (saldo deve essere 0) o rifiuta
- Employee può congelare/attivare conti

### 10.3. Transazioni

- Deposito e prelievo con ownership check
- Validazione stato conto (solo `ACTIVE`)
- Validazione saldo (prelievo non può superare saldo)
- Storico paginato con filtri data (max 30 giorni)
- Ultime 10 transazioni per conto
- Type e status risolti per nome dal DB (no hardcoded ID)

### 10.4. Carte

- Carta DEBIT emessa automaticamente alla validazione registrazione
- Stati: `INACTIVE` → `ACTIVE` → `BLOCKED` (irreversibile)
- Numero carta unico generato, CVV random, scadenza 5 anni
- Customer: lista e dettaglio proprie carte
- Employee: lista tutte le carte, dettaglio, carte per conto, aggiorna stato

### 10.5. Gestione Dipendenti

- Lista registrazioni pendenti
- Validazione/rifiuto registrazione
- Lista clienti ordinata
- Gestione conti (attiva, congela, chiudi)
- Gestione carte (stato)

---

## 11. Esempi Test

### Login e ottenimento token

```bash
# Login customer
curl -X POST http://localhost:8081/api/v1/auth/keycloak-login \
  -H "Content-Type: application/json" \
  -d '{"username": "mario.rossi@example.com", "password": "password123"}'

# Login admin
curl -X POST http://localhost:8081/api/v1/auth/keycloak-login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}'
```

### Lista conti (customer)

```bash
curl -X GET http://localhost:8081/api/v1/customer/accounts \
  -H "Authorization: Bearer <customer-token>"
```

### Dettaglio conto

```bash
curl -X GET http://localhost:8081/api/v1/customer/accounts/IT... \
  -H "Authorization: Bearer <customer-token>"
```

### Deposito

```bash
curl -X POST http://localhost:8081/api/v1/customer/transactions/deposit \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "IT...", "amount": 500}'
```

### Bonifico

```bash
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "destinationAccountNumber": "IT...", "amount": 250, "description": "Bonifico"}

# Usando beneficiario salvato
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "beneficiaryId": 1, "amount": 250}'
```

### Gestione beneficiari

```bash
# Salva beneficiario
curl -X POST http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "Mamma", "destinationAccountNumber": "IT..."}'

# Lista beneficiari
curl -X GET http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>"

# Rimuovi beneficiario
curl -X DELETE http://localhost:8081/api/v1/customer/beneficiaries/1 \
  -H "Authorization: Bearer <customer-token>"
```

### Storico transazioni

```bash
curl -X GET "http://localhost:8081/api/v1/customer/transactions/all?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59&page=0&size=20" \
  -H "Authorization: Bearer <customer-token>"
```

### Validazione registrazione (employee)

```bash
# Lista pendenti
curl -X GET http://localhost:8081/api/v1/employee/users/registrations/pending \
  -H "Authorization: Bearer <admin-token>"

# Valida
curl -X PUT http://localhost:8081/api/v1/employee/users/registrations/{userId}/validate \
  -H "Authorization: Bearer <admin-token>"
```

### Lista conti per utente (employee)

```bash
curl -X GET http://localhost:8081/api/v1/employee/accounts/user/{userId} \
  -H "Authorization: Bearer <admin-token>"
```

### Freeze conto (employee)

```bash
curl -X PUT http://localhost:8081/api/v1/employee/accounts/{accountNumber}/freeze \
  -H "Authorization: Bearer <admin-token>"
```

---

## Swagger UI

- **UI**: `http://localhost:8081/swagger-ui.html`
- **JSON**: `http://localhost:8081/v3/api-docs`

Endpoint pubblici: Swagger UI, `/api/v1/auth/register`, `/api/v1/auth/keycloak-login`. Tutti gli altri richiedono Bearer token.
