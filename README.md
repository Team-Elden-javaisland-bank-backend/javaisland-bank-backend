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
- **Database**: PostgreSQL 15 with normalized schema (lookup tables referenced via FK)
- **API Docs**: Springdoc OpenAPI 2.8 (Swagger UI integrated)
- **Organization**: Screaming Architecture — packages per domain (`account`, `user`, `card`, `transaction`)
- **Containers**: Docker Compose for PostgreSQL and Keycloak
- **i18n**: Server-side translations for error messages and notifications via `MessageSource` with locale from `Accept-Language` header

### Architectural Decisions

| Choice | Rationale |
|---|---|
| Screaming Architecture | Each feature has its own model, repository, service, DTOs, and controller. Reflects the banking domain. |
| Lookup Tables | Statuses and types in dedicated tables referenced via FK. Referential integrity, no duplicate strings, extensible without changing Java code. |
| DataInitializer | Automatic domain table seeding on first startup. Idempotent (checks row count before inserting). |
| JWT via Keycloak | OAuth2 standard, automatic JWK rotation, SSO-ready, roles mapped via realm roles. |
| GlobalExceptionHandler | Centralized error handling for business errors (`ApiBankException`), validation (`MethodArgumentNotValidException`), JPA constraint violations, and generic fallback. Supports server-side i18n via `MessageSource`. |
| DTOs with Validation | Every endpoint uses dedicated DTOs with `jakarta.validation` constraints. No entity exposed directly. |
| Lombok | Boilerplate reduction (getter, setter, builder, constructors). |
| Server-side i18n | Error messages and notifications translated server-side using `MessageSource` with `Accept-Language` header. Error codes used as translation keys. |

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
    │   │   ├── EmployeeUserDetailDto.java
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
    │   │   ├── AdminAuditLogController.java
    │   │   ├── AdminDashboardController.java
    │   │   ├── AdminEmployeeController.java
    │   │   └── AdminLimitController.java
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
    │   └── dto/
    │       └── EmployeeRequestDto.java
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
    ├── savedbeneficiary/                # Saved beneficiaries
    │   ├── controller/
    │   │   └── SavedBeneficiaryController.java
    │   ├── dto/
    │   │   ├── SavedBeneficiaryRequestDto.java
    │   │   └── SavedBeneficiaryResponseDto.java
    │   ├── model/
    │   │   └── SavedBeneficiary.java
    │   ├── repository/
    │   │   └── SavedBeneficiaryRepository.java
    │   └── service/
    │       └── SavedBeneficiaryService.java
    │
    ├── security/                        # JWT security
    │   ├── AppRoleConverter.java
    │   ├── JwtPasswordChangeFilter.java
    │   └── SecurityConfig.java
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

### Lookup Tables

Auto-populated by `DataInitializer` on first startup.
Full schema in [database-schema.txt](database-schema.txt).

| Table | Seed Values |
|---|---|
| `user_statuses` | PENDING, ACTIVE, ANNULLED, SUSPENDED |
| `role_types` | C (customer), D (employee), A (admin) |
| `account_statuses` | INACTIVE, ACTIVE, FROZEN, CLOSED |
| `card_statuses` | INACTIVE, ACTIVE, BLOCKED |
| `card_types` | DEBIT, CREDIT |
| `limit_types` | DAILY_TRANSFER, SINGLE_TRANSFER, INSTANT_TRANSFER_SINGLE, MONTHLY_TRANSFER, ATM_WITHDRAWAL, POS_SPENDING |
| `transaction_types` | DEPOSIT, WITHDRAWAL, TRANSFER, INITIAL_TRANSFER |
| `transaction_statuses` | PENDING, COMPLETED, FAILED, REJECTED |

### Main Tables

| Table | Description |
|---|---|
| `users` | Customers and employees. FK → `user_statuses`, `role_types` |
| `accounts` | Bank accounts. FK → `users` |
| `account_limits` | Per-account operational limits. FK → `accounts`, `limit_types`, unique(account_id, limit_type_id) |
| `cards` | Debit/credit cards. FK → `card_statuses`, `card_types` |
| `beneficiaries` | Contact list for transfers. FK → `users`, unique(user_id, destination_account_number) |
| `saved_beneficiaries` | Saved beneficiaries for quick transfers. FK → `users` |
| `transactions` | Account movements. FK → `transaction_types`, `transaction_statuses` |
| `notifications` | User notifications with i18n support. FK → `users`. Columns: `message_key`, `message_params` (JSON array) |
| `audit_logs` | System audit trail |

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

On first startup: domain tables are created and populated automatically.

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
| `C` | Customer | Accounts, Transactions, Cards (read), Beneficiaries, Saved Beneficiaries |
| `D` | Employee | User management, accounts, cards, admin |
| `A` | Admin | Dashboard, employee management, audit logs, limits |

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
| GET | `/holder-info` | Account holder info for dashboard |
| GET | `/monthly-summary` | Monthly transaction summary |
| POST | `/open` | Open additional account (with initial transfer) |
| POST | `/closure-request` | Request account closure |
| GET | `/{accountNumber}` | Account detail (balance, status, date) |
| GET | `/{accountNumber}/limits` | View account limits |

### Customer — Transactions — `/api/v1/customer/transactions` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/deposit` | Deposit to own account |
| POST | `/withdraw` | Withdraw from own account |
| POST | `/transfer` | Transfer to another account (or saved beneficiary) |
| GET | `/recent/{accountNumber}` | Last 10 transactions |
| GET | `/all?start=&end=&page=&size=` | Paginated history with date filters |

### Customer — Cards — `/api/v1/customer/cards` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all my cards |
| GET | `/{cardId}` | Card detail |

### Customer — Beneficiaries — `/api/v1/customer/beneficiaries` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List my saved beneficiaries |
| POST | `/` | Save new beneficiary (nickname + IBAN) |
| DELETE | `/{id}` | Remove beneficiary |

### Customer — Saved Beneficiaries — `/api/v1/customer/saved-beneficiaries` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List saved beneficiaries |
| POST | `/` | Save new beneficiary |
| DELETE | `/{id}` | Remove saved beneficiary |

### Customer — Profile — `/api/v1/profile-picture` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Upload profile picture (multipart) |
| DELETE | `/` | Delete profile picture |

### Customer — Notifications — `/api/v1/customer/notifications` `[C]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List my notifications (translated server-side) |
| PUT | `/{id}/read` | Mark notification as read |

### Customer — PIN — `/pin` `[C]`

| Method | Path | Description |
|---|---|---|
| POST | `/setup` | Set up 6-digit PIN |
| GET | `/status` | Check PIN status |
| POST | `/verify` | Verify PIN |

### Employee — Accounts — `/api/v1/employee/accounts` `[D]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all accounts (`?status=` optional) |
| GET | `/user/{userId}` | Accounts for specific user |
| GET | `/{accountNumber}` | Account detail |
| PUT | `/{accountNumber}/activate` | Activate account (INACTIVE → ACTIVE) |
| PUT | `/{accountNumber}/freeze` | Freeze account (ACTIVE → FROZEN) |
| PUT | `/{accountNumber}/closure/validate` | Validate closure (FROZEN → CLOSED) |
| PUT | `/{accountNumber}/closure/reject` | Reject closure (FROZEN → ACTIVE) |
| GET | `/{accountNumber}/limits` | View account limits |
| PUT | `/{accountNumber}/limits/{limitType}` | Set limit (e.g. DAILY_TRANSFER) |

### Employee — Users — `/api/v1/employee/users` `[D]`

| Method | Path | Description |
|---|---|---|
| GET | `/registrations/pending` | Pending registrations |
| PUT | `/registrations/{userId}/validate` | Validate registration + activate account + issue card |
| PUT | `/registrations/{userId}/reject` | Reject registration |
| GET | `/registrations/refused` | List refused registrations |
| PUT | `/registrations/{userId}/reopen` | Reopen refused registration (status → PENDING) |
| DELETE | `/registrations/{userId}` | Delete refused user + all related data |
| GET | `/customers` | List all customers sorted by name |

### Employee — Cards — `/api/v1/employee` `[D]`

| Method | Path | Description |
|---|---|---|
| GET | `/cards` | List all cards |
| GET | `/cards/{cardId}` | Card detail |
| GET | `/accounts/{accountNumber}/cards` | Cards linked to account |
| PATCH | `/cards/{cardId}/status?status=` | Update card status (ACTIVE/INACTIVE/BLOCKED) |

### Admin — Dashboard — `/api/v1/admin/dashboard` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | Aggregated statistics |

### Admin — Employees — `/api/v1/admin/employees` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all employees |
| POST | `/` | Create new employee |
| GET | `/{id}` | Employee detail |
| PUT | `/{id}/suspend` | Suspend employee (force logout + disable) |

### Admin — Limits — `/api/v1/admin/limits` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all limit types |
| PUT | `/{limitType}` | Update global limit |

### Admin — Audit Logs — `/api/v1/admin/audit-logs` `[A]`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List audit logs (paginated) |

### Comuni — `/api/v1/comuni`

| Method | Path | Description |
|---|---|---|
| GET | `/` | List Italian municipalities |
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

Messages are translated server-side based on `Accept-Language` header (Italian default, English supported).

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
| `FORBIDDEN` | 400 | Account does not belong to user |
| `EMAIL_ALREADY_REGISTERED` | 400 | Email already registered |
| `INVALID_CREDENTIALS` | 400 | Invalid credentials |
| `TRANSACTION_TYPE_NOT_FOUND` | 400 | Transaction type not configured |
| `TRANSACTION_STATUS_NOT_FOUND` | 400 | Transaction status not configured |

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

1. Customer registers → status `PENDING`, account `INACTIVE`, no card issued
2. Employee validates → status `ACTIVE`, account `ACTIVE`, DEBIT card `ACTIVE` issued automatically
3. Employee can reject registration
4. Employee can reopen refused registrations (status → `PENDING`)
5. Employee can delete refused users (cascading cleanup: cards, limits, beneficiaries, notifications, Keycloak user, etc.)

### 10.2. Bank Accounts

- Initial account created automatically on registration
- Open additional accounts (transfer from existing account)
- Request closure → account frozen → employee validates (balance must be 0) or rejects
- Employee can freeze/activate accounts
- Holder info and monthly summary endpoints for dashboard

### 10.3. Transactions

- Deposit and withdrawal with ownership check
- Account status validation (only `ACTIVE`)
- Balance validation (withdrawal cannot exceed balance)
- Paginated history with date filters (max 30 days)
- Last 10 transactions per account
- Type and status resolved by name from DB (no hardcoded IDs)

### 10.4. Cards

- DEBIT card issued automatically on registration validation
- States: `INACTIVE` → `ACTIVE` → `BLOCKED` (irreversible)
- Unique card number generated, random CVV, 5-year expiry
- Customer: list and detail of own cards
- Employee: list all cards, detail, cards per account, update status

### 10.5. Employee Management

- Pending registration list
- Registration validation/rejection
- Refused registrations management (reopen, delete with cascading cleanup)
- Customer list sorted by name
- Account management (activate, freeze, close)
- Card management (status)

### 10.6. Admin Dashboard

- Aggregated statistics (customers, employees, accounts, balance, transactions)
- Employee CRUD management with force logout on suspend
- Global limit configuration
- Audit log viewer with action filtering

### 10.7. Saved Beneficiaries

- Save frequently used beneficiaries for quick transfers
- Customer can add, list, and remove saved beneficiaries
- Endpoint: `/api/v1/customer/saved-beneficiaries`

### 10.8. Profile Picture

- Upload and delete profile pictures
- Supported formats: JPG, PNG, GIF, WebP (max 5 MB)
- Stored locally in `uploads/profile-pictures/`

### 10.9. Password Management

- Password change with automatic token invalidation via `JwtPasswordChangeFilter`
- Old tokens rejected after password change
- User status check (SUSPENDED/ANNULLED → force logout)

### 10.10. Notifications

- Customer notifications for account events
- Mark as read functionality
- Server-side i18n: notifications stored with `message_key` + `message_params`, translated on retrieval based on `Accept-Language` header

### 10.11. Internationalization (i18n)

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
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "destinationAccountNumber": "IT...", "amount": 250, "description": "Transfer"}'

# Using saved beneficiary
curl -X POST http://localhost:8081/api/v1/customer/transactions/transfer \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber": "IT...", "beneficiaryId": 1, "amount": 250}'
```

### Beneficiary management

```bash
# Save beneficiary
curl -X POST http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "Mom", "destinationAccountNumber": "IT..."}'

# List beneficiaries
curl -X GET http://localhost:8081/api/v1/customer/beneficiaries \
  -H "Authorization: Bearer <customer-token>"

# Remove beneficiary
curl -X DELETE http://localhost:8081/api/v1/customer/beneficiaries/1 \
  -H "Authorization: Bearer <customer-token>"
```

### Saved beneficiaries

```bash
# Save
curl -X POST http://localhost:8081/api/v1/customer/saved-beneficiaries \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "Dad", "iban": "IT..."}'

# List
curl -X GET http://localhost:8081/api/v1/customer/saved-beneficiaries \
  -H "Authorization: Bearer <customer-token>"

# Delete
curl -X DELETE http://localhost:8081/api/v1/customer/saved-beneficiaries/1 \
  -H "Authorization: Bearer <customer-token>"
```

### Transaction history

```bash
curl -X GET "http://localhost:8081/api/v1/customer/transactions/all?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59&page=0&size=20" \
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

---

## Swagger UI

- **UI**: `http://localhost:8081/swagger-ui.html`
- **JSON**: `http://localhost:8081/v3/api-docs`

Public endpoints: Swagger UI, `/api/v1/auth/register`, `/api/v1/auth/keycloak-login`. All others require Bearer token.
