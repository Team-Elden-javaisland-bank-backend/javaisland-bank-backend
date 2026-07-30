# EldenBank — Backend

REST API for digital banking services.

**Spring Boot 3.4** · **Spring Security 6** · **Keycloak JWT** · **PostgreSQL 15** · **Springdoc OpenAPI 2.8**

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [Database Schema](#4-database-schema)
5. [Prerequisites](#5-prerequisites)
6. [Local Setup](#6-local-setup)
7. [Authentication](#7-authentication)
8. [API Endpoints](#8-api-endpoints)
9. [Error Handling](#9-error-handling)
10. [Feature Overview](#10-feature-overview)
11. [Test Examples](#11-test-examples)

---

## 1. Architecture

- **Monolith** Spring Boot with REST endpoints
- **Authentication**: JWT Bearer via Keycloak (OAuth2 Resource Server)
- **Database**: PostgreSQL 15 with normalized schema (lookup tables referenced via FK, JPA DDL auto-update)
- **API Docs**: Springdoc OpenAPI 2.8 (Swagger UI integrated)
- **Organization**: Screaming Architecture — packages per domain (`account`, `user`, `card`, `transaction`, etc.)
- **Containers**: Docker Compose for PostgreSQL and Keycloak
- **i18n**: Server-side translations for error messages and notifications via `MessageSource` with locale from `Accept-Language` header
- **Scheduling**: `ScheduledTransferProcessor` processes pending scheduled transfers every minute (configurable via cron)

### Architectural Decisions

| Choice | Rationale |
|---|---|
| Screaming Architecture | Each feature has its own model, repository, service, DTOs, and controller. Reflects the banking domain. |
| Lookup Tables | Statuses and types in dedicated tables referenced via FK. Referential integrity, no duplicate strings, extensible without changing Java code. |
| DataInitializer | Automatic domain table seeding on first startup. Idempotent (checks row count before inserting). Migrates legacy role names (CUSTOMER/EMPLOYEE → C/D). |
| JPA DDL auto-update | Hibernate `ddl-auto: update` manages schema evolution — no Flyway/Liquibase. |
| JWT via Keycloak | OAuth2 standard, automatic JWK rotation, SSO-ready, roles mapped via realm roles. |
| GlobalExceptionHandler | Centralized error handling for business errors (`ApiBankException`), validation failures, JPA constraint violations, and generic fallback. Supports server-side i18n via `MessageSource`. |
| DTOs with Validation | Every endpoint uses dedicated DTOs with `jakarta.validation` constraints. No entity exposed directly. |
| Lombok | Boilerplate reduction (getter, setter, builder, constructors). |
| Optimistic Locking | `@Version` field on `Account` entity prevents concurrent balance modification conflicts. |
| Server-side i18n | Error messages and notifications translated server-side using `MessageSource` with `Accept-Language` header. Error codes used as translation keys. |
| SecurityUtil | Injectable helper resolving `userId` and `user` from `Jwt` token across all customer controllers. |

---

## 2. Tech Stack

| Component | Technology | Version |
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
| i18n | Spring MessageSource | (built-in) |

---

## 3. Project Structure

```
bank-backend/
├── docker-compose.yml                  # PostgreSQL + Keycloak
├── pom.xml                             # Maven dependencies
├── mvnw / mvnw.cmd                     # Maven Wrapper
│
└── src/main/java/com/javaisland/bank_backend/
    ├── BankBackendApplication.java
    │
    ├── account/                         # Bank accounts
    │   ├── controller/
    │   │   └── AccountController.java
    │   ├── dto/
    │   │   ├── AccountHolderDto.java
    │   │   ├── AccountLimitResponseDto.java
    │   │   ├── AccountResponseDto.java
    │   │   ├── CloseAccountRequestDto.java
    │   │   ├── MonthlySummaryDto.java
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
    ├── admin/                           # Admin management
    │   ├── controller/
    │   │   ├── AdminAccountController.java
    │   │   ├── AdminAuditLogController.java
    │   │   ├── AdminCustomerController.java
    │   │   ├── AdminDashboardController.java
    │   │   ├── AdminEmployeeController.java
    │   │   ├── AdminLimitController.java
    │   │   └── AdminTransactionController.java
    │   ├── dto/
    │   │   ├── AdminDashboardDto.java
    │   │   ├── CreateEmployeeRequestDto.java
    │   │   ├── EmployeeDetailDto.java
    │   │   └── EmployeeListItemDto.java
    │   └── service/
    │       ├── AdminDashboardService.java
    │       └── AdminEmployeeService.java
    │
    ├── audit/                           # Audit logging
    │   ├── dto/
    │   │   └── AuditLogDto.java
    │   ├── model/
    │   │   └── AuditLog.java
    │   ├── repository/
    │   │   └── AuditLogRepository.java
    │   └── service/
    │       └── AuditLogService.java
    │
    ├── auth/                            # Authentication
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
    ├── beneficiary/                     # Beneficiary contacts
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
    ├── card/                            # Bank cards
    │   ├── controller/
    │   │   ├── CardController.java
    │   │   ├── CustomerCardController.java
    │   │   └── EmployeeCardController.java
    │   ├── dto/
    │   │   ├── CardResponseDto.java
    │   │   └── CardSensitiveDto.java
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
    ├── comuni/                          # Italian municipality lookup
    │   ├── controller/
    │   │   └── ComuniController.java
    │   ├── dto/
    │   │   └── ComuneDto.java
    │   └── service/
    │       └── ComuniService.java
    │
    ├── common/
    │   └── PageResponseDto.java         # Generic pagination wrapper
    │
    ├── config/
    │   ├── DataInitializer.java         # Domain table seeding
    │   ├── OpenAPIConfig.java           # Swagger/OpenAPI config
    │   └── WebMvcConfig.java            # CORS + static resource config
    │
    ├── employee/                        # Employee endpoints
    │   ├── controller/
    │   │   ├── EmployeeAccountController.java
    │   │   └── EmployeeUserController.java
    │   ├── dto/
    │   │   ├── EmployeeRequestDto.java
    │   │   └── EmployeeUserDetailDto.java
    │   └── service/
    │       └── EmployeeUserService.java
    │
    ├── exception/                       # Error handling
    │   ├── ApiBankException.java
    │   └── GlobalExceptionHandler.java
    │
    ├── notification/                    # Notifications
    │   ├── controller/
    │   │   └── NotificationController.java
    │   ├── dto/
    │   │   └── NotificationDto.java
    │   ├── model/
    │   │   └── Notification.java
    │   ├── repository/
    │   │   └── NotificationRepository.java
    │   └── service/
    │       └── NotificationService.java
    │
    ├── security/                        # JWT security
    │   ├── AppRoleConverter.java
    │   ├── JwtPasswordChangeFilter.java
    │   ├── SecurityConfig.java
    │   └── SecurityUtil.java
    │
    ├── transaction/                     # Transactions
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
    │   ├── scheduler/
    │   │   └── ScheduledTransferProcessor.java
    │   └── service/
    │       └── TransactionService.java
    │
    ├── user/                            # Users
    │   ├── controller/
    │   │   ├── CustomerProfileController.java
    │   │   ├── CustomerRequestController.java
    │   │   ├── PasswordChangeController.java
    │   │   ├── ProfilePictureController.java
    │   │   ├── UserController.java
    │   │   └── UserPinController.java
    │   ├── dto/
    │   │   ├── CustomerListItemDto.java
    │   │   ├── CustomerProfileDto.java
    │   │   ├── CustomerRequestDto.java
    │   │   ├── PasswordChangeRequestCreateDto.java
    │   │   ├── PasswordChangeRequestDto.java
    │   │   ├── PendingRegistrationDto.java
    │   │   ├── PinSetupRequestDto.java
    │   │   ├── PinStatusResponseDto.java
    │   │   ├── PinVerifyRequestDto.java
    │   │   └── UserResponseDto.java
    │   ├── model/
    │   │   ├── PasswordChangeRequest.java
    │   │   ├── RoleType.java
    │   │   ├── User.java
    │   │   ├── UserPin.java
    │   │   └── UserStatus.java
    │   ├── repository/
    │   │   ├── PasswordChangeRequestRepository.java
    │   │   ├── RoleTypeRepository.java
    │   │   ├── UserPinRepository.java
    │   │   ├── UserRepository.java
    │   │   └── UserStatusRepository.java
    │   └── service/
    │       ├── PasswordChangeService.java
    │       ├── UserPinService.java
    │       └── UserService.java
    │
    └── validation/
        ├── Adult.java                   # Adult age validation
        └── AdultValidator.java
```

---

## 4. Database Schema

Schema managed via **JPA/Hibernate DDL auto-update** (`ddl-auto: update`). No Flyway/Liquibase.
Lookup tables auto-populated by `DataInitializer` on first startup.

### Entity Relationship Diagram (DBML)

```
// EldenBank Database Schema
// Source: JPA entity annotations

Table role_types {
  id integer [primary key, increment]
  role_name varchar(30) [unique, not null]
}

Table user_statuses {
  id integer [primary key, increment]
  user_status varchar(30) [unique, not null]
}

Table users {
  id bigint [primary key, increment]
  keycloak_id varchar(255) [unique]
  username varchar(50) [unique, not null]
  first_name varchar(100) [not null]
  last_name varchar(100) [not null]
  birth_date date
  email varchar(150) [unique, not null]
  role_type_id integer [not null, ref: > role_types.id]
  status_id integer [not null, ref: > user_statuses.id]
  branch_code varchar(20)
  profile_picture_url varchar(500)
  password_changed_at timestamp
  gender varchar(1)
  fiscal_code varchar(16) [unique]
  phone varchar(20)
  residence varchar(200)
  birth_place varchar(100)
  birth_province varchar(2)
  profession varchar(100)
  limits_setup_complete boolean [not null, default: false]
  created_at timestamp
}

Table account_statuses {
  id integer [primary key, increment]
  status_name varchar(30) [unique, not null]
}

Table accounts {
  id bigint [primary key, increment]
  account_number varchar(50) [unique, not null]
  balance numeric(15,2) [not null]
  status_id integer [not null, ref: > account_statuses.id]
  user_id bigint [not null, ref: > users.id]
  created_at timestamp
  closed_at timestamp
  closure_requested_at timestamp
  closure_rejected_at timestamp
  source_account_number varchar(50)
  initial_amount numeric(15,2)
  version bigint
}

Table limit_types {
  id integer [primary key, increment]
  limit_name varchar(30) [unique, not null]
  notes varchar(255)
  change_policy varchar(20) [not null]
}

Table account_limits {
  id bigint [primary key, increment]
  account_id bigint [not null, ref: > accounts.id]
  limit_type_id integer [not null, ref: > limit_types.id]
  max_amount numeric(15,2) [not null]
  updated_at timestamp
  indexes: unique (account_id, limit_type_id)
}

Table transaction_types {
  id integer [primary key, increment]
  type_name varchar(30) [unique, not null]
}

Table transaction_statuses {
  id integer [primary key, increment]
  status_name varchar(30) [unique, not null]
}

Table transactions {
  id bigint [primary key, increment]
  amount numeric(15,2) [not null]
  type_id integer [not null, ref: > transaction_types.id]
  status_id integer [not null, ref: > transaction_statuses.id]
  description varchar(255)
  source_balance_after numeric(15,2)
  dest_balance_after numeric(15,2)
  created_at timestamp
  scheduled_date timestamp
  source_account_id bigint [ref: > accounts.id]
  destination_account_id bigint [ref: > accounts.id]
}

Table card_types {
  id integer [primary key, increment]
  type_name varchar(30) [unique, not null]
}

Table card_statuses {
  id integer [primary key, increment]
  status_name varchar(30) [unique, not null]
}

Table cards {
  id bigint [primary key, increment]
  account_id bigint [not null]
  card_number varchar(16) [unique, not null]
  cvv varchar(3) [not null]
  expiration_date date [not null]
  holder_name varchar(150) [not null]
  card_type_id integer [not null, ref: > card_types.id]
  status_id integer [ref: > card_statuses.id]
}

Table beneficiaries {
  id bigint [primary key, increment]
  user_id bigint [not null, ref: > users.id]
  nickname varchar(100) [not null]
  beneficiary_name varchar(150)
  destination_account_number varchar(50) [not null]
  created_at timestamp
  indexes: unique (user_id, destination_account_number)
}

Table notifications {
  id bigint [primary key, increment]
  user_id bigint [not null]
  type varchar(50) [not null]
  message varchar(500) [not null]
  message_key varchar(100)
  message_params varchar(500)
  is_read boolean [not null, default: false]
  created_at timestamp [not null]
}

Table audit_logs {
  id bigint [primary key, increment]
  entity_type varchar(50) [not null]
  entity_id bigint
  action varchar(50) [not null]
  performed_by varchar(100) [not null]
  performed_by_user_id bigint
  details varchar(500)
  performed_at timestamp
}

Table user_pins {
  id bigint [primary key, increment]
  user_id bigint [unique, not null, ref: > users.id]
  pin_hash varchar(255) [not null]
  created_at timestamp
}

Table password_change_requests {
  id bigint [primary key, increment]
  user_id bigint [not null]
  new_plain_password varchar(100) [not null]
  status varchar(20) [not null]
  created_at timestamp [not null]
  processed_at timestamp
}
```

### Lookup Tables — Seed Values

| Table | Column | Seed Values |
|---|---|---|
| `user_statuses` | `user_status` | PENDING, ACTIVE, ANNULLED, SUSPENDED |
| `role_types` | `role_name` | C (customer), D (employee), A (admin) |
| `account_statuses` | `status_name` | INACTIVE (1), ACTIVE (2), FROZEN (3), CLOSED (4) |
| `card_statuses` | `status_name` | INACTIVE, ACTIVE, BLOCKED, CLOSED |
| `card_types` | `type_name` | DEBIT |
| `limit_types` | `limit_name` | DAILY_TRANSFER, SINGLE_TRANSFER, INSTANT_TRANSFER_SINGLE, MONTHLY_TRANSFER, ATM_WITHDRAWAL, POS_SPENDING |
| `transaction_types` | `type_name` | DEPOSIT (1), WITHDRAWAL (2), TRANSFER (3), INITIAL_TRANSFER (4), INSTANT_TRANSFER (5) |
| `transaction_statuses` | `status_name` | PENDING (1), COMPLETED (2), FAILED (3), REJECTED (4), CANCELLED (5) |

### Limit Types — Change Policies

| limit_name | change_policy | Description |
|---|---|---|
| DAILY_TRANSFER | USER_LOWER_ONLY | Cumulative daily transfer limit (user can only lower) |
| SINGLE_TRANSFER | USER_LOWER_ONLY | Max per single transfer (user can only lower) |
| INSTANT_TRANSFER_SINGLE | BANK_ONLY | Max per instant transfer (bank-only setting) |
| MONTHLY_TRANSFER | BANK_ONLY | Cumulative monthly transfer limit (bank-only) |
| ATM_WITHDRAWAL | USER_FULL | Max ATM withdrawal per transaction (user full control) |
| POS_SPENDING | USER_FULL | Max POS spending per transaction (user full control) |

### Main Tables — Column Details

| Table | Columns |
|---|---|
| `users` | id, keycloak_id (unique), username (unique), first_name, last_name, birth_date, email (unique), role_type_id → `role_types`, status_id → `user_statuses`, branch_code, profile_picture_url, password_changed_at, gender, fiscal_code (unique), phone, residence, birth_place, birth_province, profession, limits_setup_complete, created_at |
| `accounts` | id, account_number (unique), balance, status_id → `account_statuses`, user_id → `users`, created_at, closed_at, closure_requested_at, closure_rejected_at, source_account_number, initial_amount, version (optimistic lock) |
| `account_limits` | id, account_id → `accounts`, limit_type_id → `limit_types`, max_amount, updated_at — unique(account_id, limit_type_id) |
| `cards` | id, account_id, card_number (unique), cvv, expiration_date, holder_name, card_type_id → `card_types`, status_id → `card_statuses` |
| `transactions` | id, amount, type_id → `transaction_types`, status_id → `transaction_statuses`, description, source_balance_after, dest_balance_after, created_at, scheduled_date, source_account_id → `accounts`, destination_account_id → `accounts` |
| `beneficiaries` | id, user_id → `users`, nickname, beneficiary_name, destination_account_number, created_at — unique(user_id, destination_account_number) |
| `notifications` | id, user_id, type, message, message_key (i18n key), message_params (JSON array), is_read, created_at |
| `audit_logs` | id, entity_type, entity_id, action, performed_by, performed_by_user_id, details, performed_at |
| `user_pins` | id, user_id → `users` (unique, effectively 1:1), pin_hash, created_at |
| `password_change_requests` | id, user_id, status (PENDING/APPROVED/REJECTED), created_at, processed_at |

### Status Constants (Java)

| Class | Values |
|---|---|
| `AccountStatus` | INACTIVE=1, ACTIVE=2, FROZEN=3, CLOSED=4 |
| `CardStatus` | (DB: INACTIVE, ACTIVE, BLOCKED, CLOSED) |
| `TransactionType` | DEPOSIT=1, WITHDRAWAL=2, TRANSFER=3, INITIAL_TRANSFER=4, INSTANT_TRANSFER=5 |
| `TransactionStatus` | PENDING=1, COMPLETED=2, FAILED=3, REJECTED=4, CANCELLED=5 |
| `UserStatus` | PENDING=1, ACTIVE=2, ANNULLED=3, SUSPENDED=4 |
| `RoleType` | CUSTOMER=1, EMPLOYEE=2, ADMIN=3 |

---

## 5. Prerequisites

- **Java 21+** (JDK)
- **Maven 3.9+** (wrapper `mvnw` included)
- **Docker Desktop** (for PostgreSQL and Keycloak)

---

## 6. Local Setup

### 6.1. Start infrastructure

```bash
docker compose up -d
```

Starts:
- **PostgreSQL** on `localhost:5433` (db: `javaisland_backend`, user: `bank_admin`, password: `bank_password`)
- **Keycloak** on `localhost:8080` (admin / admin)

### 6.2. Import Keycloak realm

1. Go to `http://localhost:8080` (admin / admin)
2. **Create Realm** → name: `javaisland-realm`
3. **Clients** → **Create client**:
   - Client ID: `bank-backend`
   - Client authentication: `OFF`
   - Standard flow: `OFF`
   - Direct access grants: `ON`
4. **Realm roles** → Create: `C`, `D`, `A`
5. Create users and assign roles

### 6.3. Start application

```bash
./mvnw spring-boot:run
```

Application runs on **`http://localhost:8081`**.
On first startup: all tables are created and lookup tables populated automatically.

---

## 7. Authentication

JWT Bearer via Keycloak OAuth2 Direct Access Grant.

### Flow

```
Client → POST /api/v1/auth/keycloak-login → Keycloak → access_token
Client → Bearer token → App → Keycloak JWK → validation → roles
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

### Response

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

### Roles

| Role | Description | Endpoints |
|---|---|---|
| `C` | Customer | Accounts, Transactions, Cards (read), Beneficiaries, Saved Beneficiaries, Profile, Notifications, PIN, Limits |
| `D` | Employee | User management (registrations, password requests, limit requests), Account management (activate, freeze, close), Card management (block, unblock, sensitive) |
| `A` | Admin | Dashboard, Employee CRUD, Account overview, Customer overview, Audit logs, Global limits, Transactions |

### Additional Security Mechanisms

- **Password change token invalidation**: `JwtPasswordChangeFilter` rejects tokens issued before `password_changed_at`
- **User status check**: SUSPENDED/ANNULLED users are force-logged out
- **Last active account check**: Prevents closure of the only remaining active account
- **Optimistic locking**: `@Version` on `Account` prevents concurrent balance modification

### Configuration

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

Environment variables: `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_AUTH_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD`.

---

## 8. API Endpoints

### Public — `/api/v1/auth`

| Method | Path | Description |
|---|---|---|
| POST | `/register` | Register new user (status PENDING) |
| POST | `/keycloak-login` | Keycloak login, returns JWT + profile |

### Customer — Accounts — `/api/v1/customer/accounts` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all my accounts |
| POST | `/open` | Open additional account (with initial transfer from existing account) |
| POST | `/closure-request` | Request account closure (blocked if last active account) |
| GET | `/last-active-check` | Check if current user has only one active account |
| GET | `/{accountNumber}` | Account detail (balance, status, dates) |
| GET | `/{accountNumber}/holder-info` | Account holder info for dashboard |
| GET | `/{accountNumber}/monthly-summary` | Monthly transaction summary |
| GET | `/{accountNumber}/limits` | View account limits |
| PUT | `/{accountNumber}/limits/{limitType}` | Update account limit (respects change policy) |
| PUT | `/limits-setup-complete` | Mark account limits setup as complete |

### Customer — Transactions — `/api/v1/customer/transactions` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/deposit` | Deposit to own account |
| POST | `/withdraw` | Withdraw from own account |
| POST | `/transfer` | Transfer (standard, instant, scheduled, or to beneficiary) |
| GET | `/recent/{accountNumber}` | Last 10 transactions |
| GET | `/all?start=&end=&page=&size=` | Paginated history with date filters |
| DELETE | `/{transactionId}/cancel` | Cancel pending transfer |

### Customer — Cards — `/api/v1/customer/cards` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all my cards |
| GET | `/{cardId}` | Card detail |
| GET | `/{cardId}/sensitive` | Card detail with sensitive data (CVV, full number, expiry) |

### Customer — Beneficiaries — `/api/v1/customer/beneficiaries` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List my beneficiaries |
| POST | `/` | Save new beneficiary (nickname + IBAN) |
| DELETE | `/{id}` | Remove beneficiary |
| GET | `/check?accountNumber=` | Check if beneficiary already exists for user |
| PUT | `/{id}/rename` | Rename beneficiary |

### Customer — Profile — `/api/v1/customer/profile` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | Get customer profile (name, email, fiscal code, address, etc.) |

### Customer — Profile Picture — `/api/v1/profile-picture` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Upload profile picture (multipart, max 5MB, JPG/PNG/GIF/WebP) |
| DELETE | `/` | Delete profile picture |

### Customer — Notifications — `/api/v1/customer/notifications` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List my notifications (translated server-side via i18n keys) |
| GET | `/unread-count` | Count unread notifications |
| PUT | `/{id}/read` | Mark notification as read |
| PUT | `/read-all` | Mark all notifications as read |

### Customer — PIN — `/api/v1/customer/pin` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/setup` | Set up 6-digit PIN (hashed, stored securely) |
| GET | `/status` | Check if PIN is set up |
| POST | `/verify` | Verify PIN (for sensitive operations) |

### Customer — Password Change — `/api/v1/customer/password-change` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Request password change (→ employee approval) |

### Customer — Limit Change Request — `/api/v1/customer/limit-change` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Request limit change (→ employee approval) |

### Customer — Requests — `/api/v1/customer/requests` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all my requests (password changes, limit changes) with statuses |

### Employee — Accounts — `/api/v1/employee/accounts` `[D]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all accounts (`?status=` optional filter) |
| GET | `/user/{userId}` | Accounts for specific user |
| GET | `/{accountNumber}` | Account detail |
| GET | `/{accountNumber}/user-detail` | Account + owner detail |
| PUT | `/{accountNumber}/activate` | Activate account (INACTIVE → ACTIVE) |
| PUT | `/{accountNumber}/reject` | Reject account opening (INACTIVE → CLOSED) |
| PUT | `/{accountNumber}/freeze` | Freeze account (ACTIVE → FROZEN) |
| PUT | `/{accountNumber}/unfreeze` | Unfreeze account (FROZEN → ACTIVE) |
| PUT | `/{accountNumber}/closure/validate` | Validate closure (FROZEN → CLOSED) |
| PUT | `/{accountNumber}/closure/reject` | Reject closure (FROZEN → ACTIVE) |
| GET | `/{accountNumber}/limits` | View account limits |
| PUT | `/{accountNumber}/limits/{limitType}` | Set account limit |

### Employee — Users — `/api/v1/employee/users` `[D]`

| Method | Path | Description |
|---|---|---|
| GET | `/registrations/pending` | Pending registrations |
| PUT | `/registrations/{userId}/validate` | Validate registration + activate account + issue DEBIT card |
| PUT | `/registrations/{userId}/reject` | Reject registration |
| GET | `/registrations/refused` | List refused registrations |
| PUT | `/registrations/{userId}/reopen` | Reopen refused registration (status → PENDING) |
| DELETE | `/registrations/{userId}` | Delete refused user + all related data + Keycloak user |
| GET | `/customers` | List all active customers sorted by name |
| GET | `/{userId}/detail` | User detail (profile, accounts, cards) |
| GET | `/password-requests/pending` | Pending password change requests |
| PUT | `/password-requests/{requestId}/approve` | Approve password change (updates Keycloak + DB) |
| PUT | `/password-requests/{requestId}/reject` | Reject password change |
| GET | `/limit-requests/pending` | Pending limit change requests |
| PUT | `/limit-requests/{requestId}/approve` | Approve limit change |
| PUT | `/limit-requests/{requestId}/reject` | Reject limit change |
| GET | `/all-requests` | All pending requests (password + limit) combined |

### Employee — Cards — `/api/v1/employee` `[D]`

| Method | Path | Description |
|---|---|---|
| GET | `/cards` | List all cards |
| GET | `/cards/{cardId}` | Card detail |
| GET | `/cards/{cardId}/sensitive` | Card detail with sensitive data |
| PUT | `/cards/{cardId}/block` | Block card (ACTIVE → BLOCKED) |
| PUT | `/cards/{cardId}/unblock` | Unblock card (BLOCKED → ACTIVE) |
| GET | `/accounts/{accountNumber}/cards` | Cards linked to account |

### Admin — Dashboard — `/api/v1/admin/dashboard` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | Aggregated statistics (customers, employees, accounts, balance, transactions) |

### Admin — Accounts — `/api/v1/admin/accounts` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all accounts (paginated) |
| GET | `/{accountNumber}` | Account detail |
| GET | `/{accountNumber}/limits` | View account limits |
| PUT | `/{accountNumber}/limits/{limitType}` | Set account limit |

### Admin — Customers — `/api/v1/admin/customers` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all customers |
| GET | `/{userId}` | Customer detail |

### Admin — Employees — `/api/v1/admin/employees` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all employees |
| GET | `/{userId}` | Employee detail |
| POST | `/` | Create new employee (Keycloak + DB) |
| PUT | `/{userId}/suspend` | Suspend employee (force logout + disable, Keycloak + DB) |
| PUT | `/{userId}/activate` | Activate suspended employee |

### Admin — Transactions — `/api/v1/admin/transactions` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all transactions (paginated, filterable) |

### Admin — Limits — `/api/v1/admin/limits` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all limit types with global defaults |
| PUT | `/{limitType}` | Update global limit configuration |

### Admin — Audit Logs — `/api/v1/admin/audit-logs` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List audit logs (paginated, filterable by action) |

### Comuni — `/api/v1/comuni`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List Italian municipalities (paginated, searchable by name) |
| GET | `/{nome}` | Search municipality by name |

### Export — `/api/users`

| Method | Path | Description |
|---|---|---|
| GET | `/export?filePath=` | Export customer data to file |

---

## 9. Error Handling

Standard error response format:

```json
{
  "timestamp": "2026-07-09T17:00:00.000",
  "status": 400,
  "errorCode": "FORBIDDEN",
  "message": "Account IT... does not belong to the current user."
}
```

Messages are translated server-side based on `Accept-Language` header (Italian default, English supported via `messages_en.properties`).

### Business Errors

| Error Code | HTTP | Description |
|---|---|---|
| `USER_NOT_FOUND` | 400 | User not found |
| `ACCOUNT_NOT_FOUND` | 400 | Account not found |
| `ACCOUNT_INACTIVE` | 400 | Account not active |
| `ACCOUNT_SUSPENDED` | 401 | Account suspended (force logout) |
| `INVALID_ACCOUNT_STATE` | 400 | Operation not allowed on current state |
| `INSUFFICIENT_FUNDS` | 400 | Insufficient balance |
| `LIMIT_EXCEEDED` | 400 | Operational limit exceeded (daily, monthly, single) |
| `NON_ZERO_BALANCE` | 400 | Account closure with non-zero balance |
| `LAST_ACTIVE_ACCOUNT` | 400 | Cannot close the only active account |
| `FORBIDDEN` | 400 | Account does not belong to user |
| `EMAIL_ALREADY_REGISTERED` | 400 | Email already registered |
| `INVALID_CREDENTIALS` | 400 | Invalid credentials |
| `REGISTRATION_NOT_FOUND` | 400 | Pending registration not found |
| `TRANSACTION_TYPE_NOT_FOUND` | 400 | Transaction type not configured |
| `TRANSACTION_STATUS_NOT_FOUND` | 400 | Transaction status not configured |
| `PENDING_TRANSFER` | 400 | Account has a pending source transfer |
| `USER_PIN_NOT_SET` | 400 | PIN not yet configured |
| `USER_PIN_INVALID` | 400 | PIN verification failed |

### Validation Errors

| Error Code | HTTP | Description |
|---|---|---|
| `VALIDATION_ERROR` | 400 | `jakarta.validation` constraint violation on DTO fields |

### Technical Errors

| Error Code | HTTP | Description |
|---|---|---|
| `INTERNAL_ERROR` | 500 | Unexpected error (no details exposed to client) |

---

## 10. Feature Overview

### 10.1. Registration & Onboarding

1. Customer registers → user status `PENDING`, account `INACTIVE`, no card issued
2. Employee validates → user status `ACTIVE`, account `ACTIVE`, DEBIT card `ACTIVE` issued automatically with generated 16-digit number, random CVV, 5-year expiry
3. Employee can reject registration (status → `ANNULLED`)
4. Employee can reopen refused registrations (status → `PENDING`)
5. Employee can delete refused users (cascading cleanup: cards, limits, beneficiaries, notifications, Keycloak user, etc.)

### 10.2. Bank Accounts

- Initial account created automatically on registration validation
- Open additional accounts with initial transfer from existing account
- Request closure → account `FROZEN`, cards blocked, employee validates/rejects
- **Last active account protection**: blocks closure if only 1 active account remains (both API and frontend guard)
- Employee can freeze/unfreeze, activate, and reject accounts
- Holder info and monthly summary endpoints for dashboard
- Optimistic locking (`@Version`) prevents double-spend on concurrent operations
- Accounts track closure request/rejection timestamps

### 10.3. Transactions

- Deposit and withdrawal with ownership check and account status validation (only `ACTIVE`)
- Balance validation (withdrawal cannot exceed balance)
- Transfers to IBAN or beneficiary with limit enforcement (daily, single, monthly)
- **Instant transfers**: processed immediately with INSTANT_TRANSFER type
- **Scheduled transfers**: future-dated, processed by `ScheduledTransferProcessor` (cron every minute)
- Transaction cancellation: pending transfers can be cancelled by the sender
- Paginated history with date filters (max 30 days via `TransactionSpecifications`)
- Last 10 transactions per account
- Type and status resolved by name from DB (no hardcoded IDs)

### 10.4. Cards

- DEBIT card issued automatically on registration validation
- States: `INACTIVE` → `ACTIVE` → `BLOCKED` (blocked state can be reverted to ACTIVE by employee)
- Unique 16-digit card number generated, random 3-digit CVV, 5-year expiry
- Customer: list, detail, and sensitive detail (with PIN verification on frontend)
- Employee: list all, detail, sensitive, block/unblock per card

### 10.5. Account Limits

- Per-account limits by type (daily transfer, single transfer, ATM withdrawal, POS spending, etc.)
- **Change policies**: `USER_FULL` (customer can set any value), `USER_LOWER_ONLY` (customer can only decrease), `BANK_ONLY` (only bank/employee can change)
- Customer: view limits, update where policy allows, submit limit change request for bank-only types
- Employee: view and update any limit per account
- Admin: global limit configuration
- `limits_setup_complete` flag on user tracks initial limit setup

### 10.6. Employee Management

- Pending registration list with validation/rejection
- Refused registrations: list, reopen, delete with cascading cleanup
- Customer list sorted by name with detail view (accounts, cards)
- Account management: activate, reject, freeze, unfreeze, validate/reject closure
- Card management: block, unblock, view sensitive
- **Password change requests**: approve/reject customer password reset requests (updates Keycloak + DB)
- **Limit change requests**: approve/reject customer limit modification requests
- Unified pending requests view (`/all-requests`)

### 10.7. Admin Dashboard

- Aggregated statistics (customers, employees, accounts, total balance, transaction counts)
- Employee CRUD management with Keycloak sync (create, suspend with force logout, activate)
- Account overview (all accounts, detail, limits)
- Customer overview (list, detail)
- Transaction viewer (all transactions, paginated, filterable)
- Global limit configuration
- Audit log viewer with action filtering

### 10.8. Beneficiaries & Saved Beneficiaries

- **Beneficiaries**: contact list for transfers, verified by IBAN existence in the bank. Unique per user + destination IBAN. Rename support.
- **Saved Beneficiaries**: external IBANs saved for quick transfers (cross-bank). CRUD operations.
- Both used in transfer flow as `beneficiaryId` parameter.

### 10.9. Profile Picture

- Upload and delete profile pictures
- Supported formats: JPG, PNG, GIF, WebP (max 5 MB)
- Stored locally in `uploads/profile-pictures/`

### 10.10. PIN Management

- 6-digit PIN, stored as BCrypt hash
- Status check endpoint (is PIN set up?)
- Verification endpoint for sensitive operations
- One PIN per user (1:1 relationship)

### 10.11. Password Management

- Customer requests password change → PENDING status
- Employee approves → new password applied to Keycloak + DB, `password_changed_at` updated
- `JwtPasswordChangeFilter` invalidates all tokens issued before the change
- Employee can reject password change requests
- User status check (SUSPENDED/ANNULLED → force logout independent of token validity)

### 10.12. Notifications

- Customer notifications for account events (deposit, withdrawal, transfer, closure request, etc.)
- Mark as read / mark all as read
- Unread count for badge display
- Server-side i18n: notifications stored with `message_key` + `message_params` (JSON array), translated on retrieval based on `Accept-Language` header

### 10.13. Audit Logging

- Automatic logging via `AuditLogService` for key operations (account creation, closure, card actions, employee actions)
- Admin can view paginated audit logs filtered by action type

### 10.14. Internationalization (i18n)

- Error messages translated server-side via `MessageSource`
- Notification messages stored as translation keys with parameters
- Locale resolved from `Accept-Language` header
- Default: Italian (`messages.properties`), English (`messages_en.properties`)

---

## 11. Test Examples

### Login and token retrieval

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

### List accounts (customer)

```bash
curl -X GET http://localhost:8081/api/v1/customer/accounts \
  -H "Authorization: Bearer <customer-token>"
```

### Account detail

```bash
curl -X GET http://localhost:8081/api/v1/customer/accounts/IT... \
  -H "Authorization: Bearer <customer-token>"
```

### Deposit

```bash
curl -X POST http://localhost:8081/api/v1/customer/transactions/deposit \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "IT...", "amount": 500}'
```

### Transfer

```bash
# Standard transfer
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "destinationAccountNumber": "IT...", "amount": 250, "description": "Transfer"}'

# Using saved beneficiary
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "beneficiaryId": 1, "amount": 250}'

# Instant transfer
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "destinationAccountNumber": "IT...", "amount": 100, "instant": true}'

# Scheduled transfer
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "destinationAccountNumber": "IT...", "amount": 100, "scheduledDate": "2026-08-01T10:00:00"}'
```

### Cancel pending transfer

```bash
curl -X DELETE http://localhost:8081/api/v1/customer/transactions/1/cancel \
  -H "Authorization: Bearer <customer-token>"
```

### Account limits

```bash
# View limits
curl -X GET http://localhost:8081/api/v1/customer/accounts/IT.../limits \
  -H "Authorization: Bearer <customer-token>"

# Update limit (respects change policy)
curl -X PUT http://localhost:8081/api/v1/customer/accounts/IT.../limits/ATM_WITHDRAWAL \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

### PIN management

```bash
# Setup PIN
curl -X POST http://localhost:8081/api/v1/customer/pin/setup \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"pin": "123456"}'

# Check PIN status
curl -X GET http://localhost:8081/api/v1/customer/pin/status \
  -H "Authorization: Bearer <customer-token>"

# Verify PIN
curl -X POST http://localhost:8081/api/v1/customer/pin/verify \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"pin": "123456"}'
```

### Beneficiary management

```bash
# Save internal beneficiary (bank account must exist)
curl -X POST http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "Mom", "destinationAccountNumber": "IT..."}'

# Save external beneficiary (any IBAN)
curl -X POST http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "Dad ext", "beneficiaryName": "Dad", "destinationAccountNumber": "IT..."}'

# List beneficiaries
curl -X GET http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>"

# Remove beneficiary
curl -X DELETE http://localhost:8081/api/v1/customer/beneficiaries/1 \
  -H "Authorization: Bearer <customer-token>"
```

### Transaction history

```bash
curl -X GET "http://localhost:8081/api/v1/customer/transactions/all?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59&page=0&size=20" \
  -H "Authorization: Bearer <customer-token>"
```

### Notifications

```bash
# List notifications
curl -X GET http://localhost:8081/api/v1/customer/notifications \
  -H "Authorization: Bearer <customer-token>"

# Unread count
curl -X GET http://localhost:8081/api/v1/customer/notifications/unread-count \
  -H "Authorization: Bearer <customer-token>"

# Mark as read
curl -X PUT http://localhost:8081/api/v1/customer/notifications/1/read \
  -H "Authorization: Bearer <customer-token>"

# Mark all as read
curl -X PUT http://localhost:8081/api/v1/customer/notifications/read-all \
  -H "Authorization: Bearer <customer-token>"
```

### Registration validation (employee)

```bash
# List pending
curl -X GET http://localhost:8081/api/v1/employee/users/registrations/pending \
  -H "Authorization: Bearer <employee-token>"

# Validate
curl -X PUT http://localhost:8081/api/v1/employee/users/registrations/{userId}/validate \
  -H "Authorization: Bearer <employee-token>"

# List refused
curl -X GET http://localhost:8081/api/v1/employee/users/registrations/refused \
  -H "Authorization: Bearer <employee-token>"

# Reopen refused
curl -X PUT http://localhost:8081/api/v1/employee/users/registrations/{userId}/reopen \
  -H "Authorization: Bearer <employee-token>"

# Delete refused user
curl -X DELETE http://localhost:8081/api/v1/employee/users/registrations/{userId} \
  -H "Authorization: Bearer <employee-token>"
```

### Password change request management (employee)

```bash
# List pending password change requests
curl -X GET http://localhost:8081/api/v1/employee/users/password-requests/pending \
  -H "Authorization: Bearer <employee-token>"

# Approve
curl -X PUT http://localhost:8081/api/v1/employee/users/password-requests/{requestId}/approve \
  -H "Authorization: Bearer <employee-token>"

# Reject
curl -X PUT http://localhost:8081/api/v1/employee/users/password-requests/{requestId}/reject \
  -H "Authorization: Bearer <employee-token>"
```

### Account list per user (employee)

```bash
curl -X GET http://localhost:8081/api/v1/employee/accounts/user/{userId} \
  -H "Authorization: Bearer <employee-token>"
```

### Freeze account (employee)

```bash
curl -X PUT http://localhost:8081/api/v1/employee/accounts/{accountNumber}/freeze \
  -H "Authorization: Bearer <employee-token>"
```

### Admin dashboard

```bash
curl -X GET http://localhost:8081/api/v1/admin/dashboard \
  -H "Authorization: Bearer <admin-token>"
```

### Suspend employee (admin)

```bash
curl -X PUT http://localhost:8081/api/v1/admin/employees/{id}/suspend \
  -H "Authorization: Bearer <admin-token>"
```

### Audit logs (admin)

```bash
curl -X GET "http://localhost:8081/api/v1/admin/audit-logs?page=0&size=20&action=USER_REGISTRATION_VALIDATED" \
  -H "Authorization: Bearer <admin-token>"
```

---

## Swagger UI

- **UI**: `http://localhost:8081/swagger-ui.html`
- **JSON**: `http://localhost:8081/v3/api-docs`

Public endpoints: Swagger UI, `/api/v1/auth/register`, `/api/v1/auth/keycloak-login`. All others require Bearer token.
