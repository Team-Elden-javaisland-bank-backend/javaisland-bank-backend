# JavaIsland Bank - Backend

Modulo backend per la gestione dei servizi bancari digitali, sviluppato con Spring Boot, Spring Security, OAuth2 e Keycloak.

---

## 1. Decisioni Architetturali (Architectural Decisions)

Per garantire la manutenibilità, la leggibilità e la scalabilità dell'applicazione, sono state adottate le seguenti scelte di design:

* **Screaming Architecture**: Il progetto è strutturato per funzionalità di dominio (package `user`, `card`, `request`) e non per layer tecnici. Questa organizzazione permette di comprendere le responsabilità dell'applicazione direttamente dall'albero delle directory.
* **Tabelle di Lookup tramite Enum**: Gli stati dell'utente (`UserStatus`), le tipologie di carta (`CardType`) e gli stati delle carte (`CardStatus`) sono stati implementati come Enum Java e mappati sul database come stringhe tramite l'annotazione `@Enumerated(EnumType.STRING)`. Questo approccio previene rallentamenti dovuti a operazioni di JOIN sul database e riduce il rischio di errori di battitura.
* **Configurazione Unificata**: È stato adottato un unico file `application.yml` in sostituzione del classico `application.properties`, consentendo una configurazione strutturata ad albero più ordinata e una riduzione dei conflitti di configurazione.

---

## 2. Funzionalità Sviluppate

Le recenti attività di sviluppo hanno riguardato il consolidamento del sistema di gestione utenti, dell'emissione delle carte e del modulo per le richieste amministrative e i prestiti.

### Data Transfer Objects (DTO) e Validazione
Al fine di evitare l'esposizione diretta delle entità di persistenza e proteggere gli endpoint da input non conformi, è stata integrata la validazione tramite `jakarta.validation`:
* **`CardIssueDTO`**: Gestisce i parametri di richiesta per l'emissione di una nuova carta di credito o debito.
* **`UserRegisterDTO`**: Consente la mappatura sicura dei dati sensibili durante la registrazione dell'utente.
* **Validazione via Espressione Regolare (Regex)**: Il campo `holderName` della carta è stato vincolato tramite l'espressione regolare `^[a-zA-Z\s]+$`. L'endpoint accetta esclusivamente lettere e spazi, bloccando preventivamente l'inserimento di numeri, simboli o caratteri speciali.

### Gestione Globale delle Eccezioni
Il meccanismo di gestione degli errori è stato centralizzato all'interno della classe `GlobalExceptionHandler` tramite l'annotazione `@RestControllerAdvice`:
* **Gestione degli errori di validazione (`MethodArgumentNotValidException`)**: In caso di input non conforme ai vincoli del DTO, l'applicazione risponde con uno stato `400 Bad Request` e un payload strutturato contenente l'elenco puntuale dei campi invalidi e i relativi messaggi di errore.
* **Gestione dell'eccezione personalizzata (`ApiBankException`)**: Consente di intercettare e gestire formalmente le violazioni delle regole di business del dominio bancario.

### Controllo degli Accessi basato sui Ruoli (RBAC)
La sicurezza degli endpoint è stata configurata all'interno di `SecurityConfig` e integrata nei controller mediante l'annotazione `@PreAuthorize`:
* **`POST /api/cards/issue`**: Endpoint riservato al personale interno provvisto del ruolo `EMPLOYEE`.
* **`POST /api/requests/loans`**: Endpoint riservato ai correntisti provvisti del ruolo `CUSTOMER`.

---

## 3. Linee Guida per il Test delle API tramite Swagger UI

Di seguito vengono descritti i passaggi necessari per verificare i flussi di sicurezza e di validazione in ambiente locale.

### Configurazione dell'Utenza di Test su Keycloak
1. Accedere alla Keycloak Administration UI all'indirizzo `http://localhost:8080`.
2. Selezionare il realm di riferimento del progetto (`javaisland-realm`).
3. Navigare nella sezione **Users** -> **Add user** e creare un'utenza con username `impiegato_test`.
4. Accedere alla scheda **Credentials** dell'utente, impostare una password e disattivare l'opzione **Temporary** per evitare la richiesta di cambio password obbligatorio al primo accesso.
5. Accedere alla scheda **Role mapping**, fare clic su **Assign role**, selezionare il ruolo `EMPLOYEE` precedentemente configurato a livello di Realm e assegnarlo all'utente.

### Autenticazione in Swagger UI
1. Aprire l'interfaccia di Swagger UI all'indirizzo configurato del server locale.
2. Fare clic sul pulsante **Authorize** presente in alto a destra.
3. Compilare il modulo relativo al flusso OAuth2 Password:
    * **username**: `impiegato_test`
    * **password**: *La password impostata nella console di Keycloak*
    * **client_id**: `bank-backend`
4. Fare clic su **Authorize** e successivamente su **Close**. Lo stato dell'autenticazione sarà confermato dall'icona del lucchetto chiuso.

### Esecuzione dei Casi di Test per l'Emissione Carte
Espandere la sezione relativa al `CardController` ed eseguire i seguenti test sull'endpoint `POST /api/cards/issue`.

#### Caso di Test 1: Fallimento della Validazione (Presenza di caratteri speciali)
Inviare un payload il cui nome del titolare contenga caratteri non alfabetici:
```json
{
  "accountId": 1,
  "holderName": "Andrea96!",
  "cardType": "DEBIT"
}