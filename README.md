# 🏦 JavaIsland Bank - Backend

Questo è il modulo backend del progetto JavaIsland_Bank sviluppato con **Spring Boot 3** e **Java 21**.
Il sistema gestisce le operazioni principali di correntisti, carte e richieste di prestito.

---

## 🧠 1. Decisioni Architetturali (Architectural Decisions)

Per mantenere il codice pulito, leggibile e scalabile, abbiamo adottato le seguenti scelte:

*   🏛️ **Screaming Architecture**: Il progetto è diviso per **funzionalità/dominio** (package `user`, `card`, `request`) e non per layer tecnici. Questo permette a chiunque di capire cosa fa l'applicazione semplicemente guardando le cartelle.
*   🗂️ **Tabelle di Lookup tramite Enum**: Gli stati dell'utente (`UserStatus`), i tipi di carta (`CardType`) e gli stati delle carte (`CardStatus`) sono stati implementati come **Enum Java** mappati nel database come stringhe (`@Enumerated(EnumType.STRING)`). Questo evita JOIN lenti sul DB e previene bug di battitura.
*   📝 **Configurazione Unica**: È stato rimosso `application.properties` in favore di un unico file **`application.yml`**, visivamente più ordinato e strutturato ad albero per evitare conflitti di configurazione.

---

## 🚀 2. Come Avviare il Progetto (Quick Start)

### 📌 Prerequisiti
*   Java 21 installato.
*   Docker (per il database PostgreSQL e Keycloak).

### ⚙️ Passi per l'avvio:

1.  **Avviare i container Docker** (PostgreSQL e Keycloak) usando il file `docker-compose.yml` nella radice del progetto:
    ```bash
    docker compose up -d
    ```
2.  **Verificare la configurazione**: Controllare che le credenziali nel file `src/main/resources/application.yml` corrispondano a quelle del proprio database locale.
3.  **Compilare il progetto**:
    ```bash
    ./mvnw clean compile
    ```
4.  **Avviare l'applicazione**:
    ```bash
    ./mvnw spring-boot:run
    ```

Il server partirà sulla porta `8080`.

---

## 🛠️ 3. Linee Guida per gli altri Sviluppatori (Onboarding Dev)

Se devi aggiungere una nuova funzionalità o modificare il codice esistente, segui queste regole per mantenere l'ordine:

1.  **Lavora per Dominio**: Se crei una nuova entità (es. `Transaction`), non sparpagliare i file. Crea un nuovo package `com.javaisland.bank_backend.transaction` e inserisci lì dentro Entity, Repository, Service e Controller.
2.  **Non usare ID numerici per gli Stati**: Se devi inserire nuovi stati fissi, crea un Enum dedicato. Nel database ricordati di mapparlo come Stringa usando l'annotazione:
    ```java
    @Enumerated(EnumType.STRING)
    ```
3.  **Non toccare la branch develop direttamente**: Crea sempre una branch di feature (es. `feature-nome-funzione`) partendo da `develop`, lavora lì e poi apri una Pull Request per la revisione.

📝 Registro delle Modifiche:
Allineamento Sicurezza e Test API
1. Correzione Configurazione Keycloak (application.yml)
   🔍 Problema: L'applicazione lanciava un errore 401 Unauthorized su Swagger nonostante il token JWT inviato fosse formalmente corretto.

💡 Causa: Le proprietà issuer-uri e jwk-set-uri puntavano al realm errato (real-bank-realm), non allineato con il realm effettivo configurato su Keycloak (javaisland-realm).

🛠️ Soluzione: Aggiornati gli URL nel file application.yml per puntare al realm corretto. La SecurityConfig è stata mantenuta intatta poiché le regole di autorizzazione generali erano già corrette; il problema risiedeva esclusivamente nell'endpoint di validazione dell'emettitore del token.

2. Validazione Dati e Vincoli DB su Registro Utenti (POST /api/users/register)
   🔍 Problema: Durante i test di registrazione si verificava un errore 500 Internal Server Error.

💡 Causa: Il payload di esempio ometteva il campo keycloakId. Nel database PostgreSQL, la colonna keycloak_id della tabella users possiede un vincolo di integrità NOT NULL.

🛠️ Soluzione: Il payload della richiesta è stato corretto includendo una stringa identificativa univoca per il keycloakId. Per i test locali è sufficiente valorizzarlo con un ID fittizio per soddisfare il vincolo del database.

3. Gestione Scadenza Sessioni (Token TTL)
   🔍 Problema: Ricorrenza di errori 401 Unauthorized con messaggio Jwt expired dopo periodi di inattività.

💡 Causa: I token di accesso emessi da Keycloak hanno una durata di validità predefinita di 5 minuti per ragioni di sicurezza.

🛠️ Soluzione: Non sono state necessarie modifiche al codice. La procedura corretta prevede semplicemente di effettuare il logout e una nuova autenticazione tramite il pulsante Authorize su Swagger per rigenerare un token valido.