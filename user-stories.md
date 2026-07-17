# EldenBank — User Stories Complete

> Backend: Spring Boot 3.4 + Keycloak + PostgreSQL  
> Frontend: Angular 22  
> Ruoli: **C** (Customer), **D** (Employee)

---

## Come leggere questo documento

Ogni user story segue lo standard **"Come / Voglio / Così che"** e contiene:

| Campo | Significato |
|-------|-------------|
| **Come** | Chi usa la funzionalità (ruolo) |
| **Voglio** | Cosa fa l'utente |
| **Così che** | Il beneficio ottenuto |
| **Priorità** | Quanto è importante: **P0** = indispensabile, **P1** = importante, **P2** = utile ma non critico |
| **Punti** | Complessità relativa (scala Fibonacci: 1, 2, 3, 5, 8, 13). Più è alto, più è complessa da implementare |
| **Criteri di accettazione** | Scenari scritti in formato **Dato / Quando / Allora** che definiscono esattamente cosa deve funzionare |

**Scala dei punti:**
- **1-2** = Semplice, poche validazioni
- **3** = Media, logica business moderata
- **5** = Complessa, più endpoint o integrazioni
- **8+** = Molto complessa, da scomporre in sotto-story

---

## FASE 1 — Registrazione e Attivazione

### US-1.1 Registrazione del primo conto
**Come** cliente, **voglio** registrarmi sulla piattaforma inserendo i miei dati anagrafici, **così che** la banca possa ricevere la mia richiesta di apertura conto.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 3 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Registrazione con dati validi
  Dato   che l'utente non esiste nel sistema
  Quando invio POST /api/v1/auth/register con dati completi e validi
  Allora viene creato un utente con stato PENDING
  E     viene creato un conto INACTIVE con saldo 0.00
  E     la password è memorizzata come hash irrecuperabile
  E     la risposta non espone dati sensibili

Scenario: Registrazione con email duplicata
  Dato   che esiste già un utente con email "mario@example.com"
  Quando invio POST /api/v1/auth/register con la stessa email
  Allora ricevo errore 400 con messaggio di duplicazione

Scenario: Registrazione con campi obbligatori mancanti
  Quando invio POST /api/v1/auth/register senza campo obbligatorio
  Allora ricevo errore 400 con dettaglio del campo mancante
```

**Note tecniche:** Password SHA-256. Transazione atomica: utente + conto creati insieme, rollback automatico su fallimento. Plain password conservata temporaneamente per provisioning Keycloak.

---

### US-1.2 Convalida della registrazione da parte del dipendente
**Come** dipendente, **voglio** visualizzare le registrazioni in sospeso e convalidarle o rifiutarle, **così che** solo utenti verificati accedano alla piattaforma.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 5 |
| **Ruolo** | Employee |

**Criteri di accettazione:**

```
Scenario: Visualizzazione registrazioni pendenti
  Dato   che esistono utenti con stato PENDING
  Quando invio GET /api/v1/employee/users/registrations/pending
  Allora ricevo la lista completa di utenti pendenti con i relativi conti

Scenario: Convalida di un utente pendente
  Dato   che l'utente ID 5 ha stato PENDING
  Quando invio PUT /api/v1/employee/users/registrations/5/validate
  Allora lo stato dell'utente diventa ACTIVE
  E     il conto associato diventa ACTIVE
  E     viene creato un utente in Keycloak con ruolo cliente
  E     viene emessa una carta DEBIT con stato ACTIVE

Scenario: Rifiuto di un utente pendente
  Quando invio PUT /api/v1/employee/users/registrations/5/reject
  Allora lo stato dell'utente diventa ANNULLED
  E     il conto associato rimane INACTIVE

Scenario: Errore durante provisioning Keycloak
  Dato   che Keycloak non è raggiungibile
  Quando convalido un utente pendente
  Allora l'utente rimane PENDING
  E     l'errore è registrato nei log di sistema
```

---

## FASE 2 — Autenticazione

### US-2.1 Login e emissione del JWT
**Come** utente, **voglio** accedere con le mie credenziali, **così che** mi venga rilasciato un token per le operazioni successive.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 3 |
| **Ruolo** | Customer / Employee |

**Criteri di accettazione:**

```
Scenario: Login con credenziali corrette
  Dato   che l'utente "mario@example.com" è ACTIVE
  Quando invio POST /api/v1/auth/keycloak-login con credenziali valide
  Allora ricevo un JWT access token
  E     il token contiene il ruolo corretto (C o D)
  E     il token contiene userId e limitsSetupComplete

Scenario: Login con password errata
  Quando invio POST /api/v1/auth/keycloak-login con password errata
  Allora ricevo errore 401

Scenario: Login con utente non attivo
  Dato   che l'utente ha stato PENDING
  Quando provo a fare login
  Allora ricevo errore 403 con messaggio "Account not active"
```

---

### US-2.2 Autorizzazione basata sui ruoli
**Come** sistema, **voglio** che gli endpoint siano protetti in base al ruolo, **così che** un cliente non possa accedere alle funzioni del dipendente e viceversa.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 2 |
| **Ruolo** | System |

**Criteri di accettazione:**

```
Scenario: Cliente accede a endpoint protetto ROLE_C
  Dato   che il token JWT ha ruolo C
  Quando invio una richiesta a /api/v1/customer/accounts
  Allora la richiesta viene completata con successo

Scenario: Cliente tenta di accedere a endpoint ROLE_D
  Quando invio una richiesta a /api/v1/employee/accounts con token ruolo C
  Allora ricevo errore 403 Forbidden

Scenario: Richiesta senza token
  Quando invio una richiesta a un endpoint protetto senza header Authorization
  Allora ricevo errore 401 Unauthorized
```

---

## FASE 3 — Gestione Conti Correnti

### US-3.1 Consultazione dei propri conti
**Come** cliente, **voglio** vedere l'elenco dei miei conti correnti con saldo e stato, **così che** abbia visione complessiva della mia situazione finanziaria.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 2 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Elenco conti dell'utente
  Dato   che l'utente ha 2 conti attivi
  Quando invio GET /api/v1/customer/accounts
  Allora ricevo una lista con 2 elementi
  E     ogni elemento contiene accountNumber, balance, status

Scenario: Utente senza conti attivi
  Quando invio GET /api/v1/customer/accounts
  Allora ricevo una lista vuota
```

---

### US-3.2 Apertura di un conto aggiuntivo
**Come** cliente, **voglio** aprire un secondo conto corrente trasferendo un importo da un conto esistente, **così che** possa diversificare la gestione dei miei risparmi.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 5 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Apertura con saldo sufficiente
  Dato   che l'utente ha un conto con saldo 1500.00
  Quando invio POST /api/v1/customer/accounts/open con importo 500.00
  Allora viene creato un nuovo conto INACTIVE
  E     il saldo del conto sorgente diminuisce di 500.00
  E     viene creata una transazione INITIAL_TRANSFER con stato PENDING
  E     viene emessa una carta DEBIT INACTIVE sul nuovo conto

Scenario: Apertura con saldo insufficiente
  Dato   che l'utente ha un conto con saldo 200.00
  Quando invio POST /api/v1/customer/accounts/open con importo 500.00
  Allora ricevo errore 400 "Insufficient funds"

Scenario: Apertura oltre il limite massimo di conti attivi
  Dato   che l'utente ha già 3 conti attivi
  Quando provo ad aprire un nuovo conto
  Allora ricevo errore 400 "Maximum active accounts reached"

Scenario: Apertura da conto non proprietario
  Dato   che l'utente non possiede il conto sorgente
  Quando provo ad aprire un conto
  Allora ricevo errore 403 "Account does not belong to the current user"
```

---

### US-3.3 Approvazione o rigetto dell'apertura conto
**Come** dipendente, **voglio** convalidare o rifiutare la richiesta di apertura di un nuovo conto, **così che** la banca controlli la procedura prima dell'attivazione.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 5 |
| **Ruolo** | Employee |

**Criteri di accettazione:**

```
Scenario: Approvazione conto
  Dato   che il conto IT... ha stato INACTIVE
  Quando invio PUT /api/v1/employee/accounts/IT.../activate
  Allora lo stato del conto diventa ACTIVE
  E     le carte associate diventano ACTIVE

Scenario: Rigetto conto
  Quando invio PUT /api/v1/employee/accounts/IT.../reject
  Allora lo stato del conto diventa CLOSED
  E     le carte associate vengono eliminate

Scenario: Congelamento conto attivo
  Dato   che il conto ha stato ACTIVE
  Quando invio PUT /api/v1/employee/accounts/IT.../freeze
  Allora lo stato del conto diventa FROZEN
  E     le operazioni sul conto vengono bloccate
```

---

### US-3.4 Consultazione dettaglio conto
**Come** cliente, **voglio** vedere i dettagli di un singolo conto, **così che** conosca saldo aggiornato, IBAN e stato.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 1 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Dettaglio conto esistente
  Quando invio GET /api/v1/customer/accounts/IT...
  Allora ricevo tutti i campi del conto
  E     il saldo è aggiornato all'ultimo movimento

Scenario: Dettaglio conto di proprietà di altro utente
  Quando invio GET /api/v1/customer/accounts/IT... (proprietà altro utente)
  Allora ricevo errore 403
```

---

## FASE 4 — Transazioni

### US-4.1 Deposito su conto proprio
**Come** cliente, **voglio** depositare fondi su un mio conto, **così che** il mio saldo aumenti.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 2 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Deposito valido
  Dato   che il conto ha stato ACTIVE e saldo 500.00
  Quando invio POST /api/v1/customer/transactions/deposit con importo 200.00
  Allora il saldo del conto diventa 700.00
  E     viene creata una transazione DEPOSIT con stato COMPLETED
  E     sourceBalanceAfter e destBalanceAfter sono coerenti

Scenario: Deposito su conto non attivo
  Dato   che il conto ha stato FROZEN
  Quando provo a depositare
  Allora ricevo errore 400 "Account is not active"
```

---

### US-4.2 Prelievo da conto proprio
**Come** cliente, **voglio** prelevare contanti da un mio conto, **così che** disponga di liquidità.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 3 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Prelievo valido entro i limiti
  Dato   che il conto ha saldo 1000.00 e limite ATM 300.00
  Quando invio POST /api/v1/customer/transactions/withdraw con importo 100.00
  Allora il saldo diminuisce di 100.00
  E     viene creata una transazione WITHDRAWAL con stato COMPLETED

Scenario: Prelievo inferiore al minimo
  Quando invio un prelievo di 5.00 (minimo 10.00)
  Allora ricevo errore 400 "Minimum withdrawal amount is €10.00"

Scenario: Prelievo superiore al saldo
  Dato   che il conto ha saldo 50.00
  Quando provo a prelevare 100.00
  Allora ricevo errore 400 "Insufficient funds"

Scenario: Prelievo superiore al limite ATM
  Dato   che il limite ATM è 300.00
  Quando provo a prelevare 400.00
  Allora ricevo errore 400 "Withdrawal amount exceeds ATM limit of €300.00"
```

---

### US-4.3 Bonifico a un altro conto
**Come** cliente, **voglio** trasferire fondi a un altro conto, **così che** possa pagare o inviare denaro.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 5 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Bonifico valido
  Dato   che il conto sorgente ha saldo 1500.00 e limite singolo 10000.00
  Quando invio POST /api/v1/customer/transactions/transfer con importo 200.00
  Allora il saldo sorgente diminuisce di 200.00
  E     il saldo destinazione aumenta di 200.00
  E     viene creata una transazione TRANSFER con stato COMPLETED

Scenario: Bonifico con destinazione inesistente
  Quando indirizzo un bonifico a un conto non esistente
  Allora ricevo errore 404 "Account not found"

Scenario: Bonifico a se stessi
  Quando il conto sorgente e destinazione coincidono
  Allora ricevo errore 400 "Source and destination accounts must be different"

Scenario: Bonifico inferiore al minimo
  Quando invio un bonifico di 0.50 (minimo 1.00)
  Allora ricevo errore 400 "Minimum transfer amount is €1.00"

Scenario: Bonifico superiore al limite giornaliero
  Dato   che il limite giornaliero è 15000.00 e oggi ho già trasferito 14500.00
  Quando provo a trasferire 1000.00
  Allora ricevo errore 400 "Daily transfer limit exceeded"

Scenario: Bonifico superiore al limite mensile
  Dato   che il limite mensile è 50000.00 e questo mese ho già trasferito 49000.00
  Quando provo a trasferire 2000.00
  Allora ricevo errore 400 "Monthly transfer limit exceeded"
```

---

### US-4.4 Bonifico tramite beneficiario salvato
**Come** cliente, **voglio** inviare un bonifico selezionando un beneficiario dalla mia lista, **così che** non debba ricordare l'IBAN ogni volta.

| | |
|---|---|
| **Priorità** | P2 — Could Have |
| **Punti** | 2 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Bonifico con beneficiario selezionato
  Dato   che ho salvato il beneficiario "Luca" con IBAN IT...
  Quando seleziono "Luca" e invio il bonifico
  Allora l'IBAN destinazione viene risolto automaticamente
  E     il bonifico viene eseguito correttamente

Scenario: Beneficiario con conto non attivo
  Dato   che il conto del beneficiario è CLOSED
  Quando provo a inviare un bonifico a quel beneficiario
  Allora ricevo errore 400 "Destination account is not active"
```

---

### US-4.5 Consultazione storico transazioni
**Come** cliente, **voglio** consultare lo storico delle transazioni filtrando per intervallo di date, **così che** possa tracciare i miei movimenti.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 3 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Storico con risultati
  Dato   che esistono transazioni nel periodo richiesto
  Quando invio GET /api/v1/customer/transactions/all?start=2026-01-01&end=2026-07-14
  Allora ricevo una pagina di transazioni
  E     ogni transazione contiene typeName, statusName, importo, data

Scenario: Storico vuoto
  Quando filtro per un periodo senza transazioni
  Allora ricevo una lista vuota

Scenario: Intervallo superiore a 365 giorni
  Quando filtro per un intervallo > 365 giorni
  Allora ricevo errore 400 "Date range cannot exceed 365 days"

Scenario: Transazioni recenti (ultime 10)
  Quando invio GET /api/v1/customer/transactions/recent/IT...
  Allora ricevo al massimo 10 transazioni ordinate per data decrescente
```

---

## FASE 5 — Beneficiari

### US-5.1 Gestione della lista beneficiari
**Come** cliente, **voglio** salvare, visualizzare e eliminare beneficiari, **così che** i miei contatti frequenti siano sempre disponibili per i bonifici.

| | |
|---|---|
| **Priorità** | P2 — Could Have |
| **Punti** | 3 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Aggiunta beneficiario
  Dato   che il conto di destinazione esiste ed è ACTIVE
  Quando salvo un beneficiario con nickname e IBAN
  Allora il beneficiario viene aggiunto alla mia lista
  E     non posso aggiungere lo stesso IBAN due volte

Scenario: Tentativo di aggiungere se stessi come beneficiario
  Quando provo a salvare un beneficiario con il mio stesso IBAN
  Allora ricevo errore 400 "Cannot add your own account as beneficiary"

Scenario: Eliminazione beneficiario
  Quando elimino un beneficiario dalla lista
  Allora viene rimosso e non è più disponibile per i bonifici
```

---

## FASE 6 — Limiti di Conto

### US-6.1 Configurazione iniziale dei limiti (first-login)
**Come** cliente al primo accesso, **voglio** configurare i limiti del mio conto, **così che** le operazioni siano regolamentate secondo le mie esigenze.

| | |
|---|---|
| **Priorità** | P0 — Must Have |
| **Punti** | 5 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Primo accesso — configurazione obbligatoria
  Dato   che l'utente ha limitsSetupComplete = false
  Quando effettuo il login per la prima volta
  Allora vengo reindirizzato alla pagina di configurazione limiti
  E     posso modificare tutti i tipi di limite senza restrizioni

Scenario: Configurazione completata
  Dato   che ho impostato tutti i limiti desiderati
  Quando confermo la configurazione
  Allora limitsSetupComplete diventa true
  E     posso accedere alla dashboard

Scenario: Valori fuori range
  Dato   che il range consentito per ATM_WITHDRAWAL è 10-300
  Quando imposto un valore fuori range
  Allora ricevo errore 400 con range min e max
```

---

### US-6.2 Modifica limiti come cliente (dopo configurazione iniziale)
**Come** cliente, **voglio** modificare i limiti del mio conto secondo le policy imposte, **così che** possa adattarli alle mie necessità.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 3 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Modifica limite con policy USER_FULL
  Dato   che ATM_WITHDRAWAL ha policy USER_FULL
  Quando aumento o diminuisco il limite
  Allora la modifica viene applicata

Scenario: Tentativo di modifica con policy BANK_ONLY
  Dato   che INSTANT_TRANSFER_SINGLE ha policy BANK_ONLY
  Quando provo a modificare il limite come cliente
  Allora ricevo errore 403 "This limit can only be modified by bank staff"

Scenario: Tentativo di aumento con policy USER_LOWER_ONLY
  Dato   che SINGLE_TRANSFER ha policy USER_LOWER_ONLY e valore attuale 5000
  Quando provo ad aumentare a 8000
  Allora ricevo errore 403 "You can only decrease this limit"
```

---

### US-6.3 Gestione limiti come dipendente
**Come** dipendente, **voglio** impostare e modificare i limiti di qualsiasi conto, **così che** la banca possa gestire le policy di rischio.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 3 |
| **Ruolo** | Employee |

**Criteri di accettazione:**

```
Scenario: Impostazione limite da parte del dipendente
  Quando invio PUT /api/v1/employee/accounts/IT.../limits/ATM_WITHDRAWAL con maxAmount 250.00
  Allora il limite viene aggiornato senza restrizioni di policy

Scenario: Valore minimo non rispettato
  Quando imposto un valore inferiore al minimo consentito per il tipo
  Allora ricevo errore 400 con il range consentito
```

---

## FASE 7 — Carte

### US-7.1 Visualizzazione delle proprie carte
**Come** cliente, **voglio** vedere le mie carte (tipo, numero mascherato, scadenza, stato), **così che** sappia quali carte ho attive.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 2 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Elenco carte dell'utente
  Quando invio GET /api/v1/customer/cards
  Allora ricevo la lista delle mie carte
  E     il numero carta è mascherato (solo ultime 4 cifre visibili)

Scenario: Dettaglio carta con dati sensibili
  Quando invio GET /api/v1/customer/cards/{id}/sensitive
  Allora ricevo numero carta completo e CVV
  E     solo se la carta appartiene al mio account
```

---

### US-7.2 Blocco carta
**Come** cliente, **voglio** bloccare una carta in caso di smarrimento, **così che** non possa essere utilizzata impropriamente.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 2 |
| **Ruolo** | Employee (su richiesta) |

**Criteri di accettazione:**

```
Scenario: Blocco carta
  Quando invio PATCH /api/cards/{id}/status?status=BLOCKED
  Allora lo stato della carta diventa BLOCKED
  E     il blocco è permanente e irreversibile

Scenario: Tentativo di sblocco carta bloccata
  Quando provo a cambiare lo stato di una carta BLOCKED
  Allora ricevo errore 400 "Card is permanently blocked"
```

---

## FASE 8 — Ciclo di Chiusura

### US-8.1 Richiesta di chiusura conto
**Come** cliente, **voglio** richiedere la chiusura di un conto, **così che** il rapporto con la banca per quel conto possa terminare.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 3 |
| **Ruolo** | Customer |

**Criteri di accettazione:**

```
Scenario: Richiesta chiusura con saldo zero
  Dato   che il conto ha saldo 0.00 e stato ACTIVE
  Quando invio POST /api/v1/customer/accounts/closure-request
  Allora lo stato del conto diventa FROZEN (in attesa di convalida)

Scenario: Richiesta chiusura con saldo positivo
  Dato   che il conto ha saldo 150.00
  Quando provo a richiedere la chiusura
  Allora ricevo errore 400 "Account balance must be zero to request closure"

Scenario: Richiesta chiusura su conto già FROZEN
  Quando provo a richiedere la chiusura su un conto FROZEN
  Allora ricevo errore 400 "Account is already pending closure"
```

---

### US-8.2 Convalida della chiusura da parte del dipendente
**Come** dipendente, **voglio** convalidare o rifiutare la chiusura di un conto, **così che** la banca registri ufficialmente la cessazione del rapporto.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 2 |
| **Ruolo** | Employee |

**Criteri di accettazione:**

```
Scenario: Convalida chiusura
  Dato   che il conto ha stato FROZEN e saldo 0.00
  Quando invio PUT /api/v1/employee/accounts/IT.../closure/validate
  Allora lo stato del conto diventa CLOSED
  E     il conto non potrà più essere utilizzato per transazioni

Scenario: Rifiuto chiusura
  Quando invio PUT /api/v1/employee/accounts/IT.../closure/reject
  Allora lo stato del conto torna ACTIVE

Scenario: Transazione su conto CLOSED
  Quando provo a eseguire una transazione su un conto CLOSED
  Allora ricevo errore 400 "Account is not active"
```

---

## FASE 9 — Back-Office Employee

### US-9.1 Consultazione conti e utenti
**Come** dipendente, **voglio** visualizzare l'elenco dei conti e degli utenti con filtri, **così che** possa monitorare l'attività della banca.

| | |
|---|---|
| **Priorità** | P1 — Should Have |
| **Punti** | 2 |
| **Ruolo** | Employee |

**Criteri di accettazione:**

```
Scenario: Elenco conti con filtro stato
  Quando invio GET /api/v1/employee/accounts?status=ACTIVE
  Allora ricevo solo i conti con lo stato specificato

Scenario: Elenco clienti
  Quando invio GET /api/v1/employee/users/customers
  Allora ricevo la lista dei clienti ordinata per nome

Scenario: Dettaglio conto come dipendente
  Quando invio GET /api/v1/employee/accounts/IT...
  Allora ricevo i dettagli completi del conto inclusi limits e cards
```

---

## Requisiti Non Funzionali

### NFR-1 Sicurezza
- Password memorizzate come hash SHA-256, mai in chiaro nel database
- JWT emesso da Keycloak con scadenza configurabile
- CORS limitato a origini note (localhost:8081, localhost:3000)
- Nessuna eccezione espone stacktrace al frontend

### NFR-2 Integrità dei Dati
- Transazioni atomiche su operazioni multiple (registrazione, apertura conto, bonifico)
- Optimistic locking sugli aggiornamenti di saldo (campo `version`)
- Validazione dei limiti sia lato client che lato server

### NFR-3 Performance
- Storico transazioni con paginazione (max 20 per pagina)
- Ricerca transazioni con range date max 365 giorni
- Ultimi 10 transazioni per il dashboard

### NFR-4 Manutenibilità
- Organizzazione del codice per dominio (account, transaction, beneficiary, card, user)
- DTO separati per request e response
- Eccezioni centralizzate via GlobalExceptionHandler
