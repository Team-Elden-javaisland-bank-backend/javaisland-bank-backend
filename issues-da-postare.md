# Issues da postare

---

## Issue 34

**Titolo:** Richieste chiusura conto nella pagina richieste dipendente

**Body:**
Aggiunto campo `closureRequestedAt` all'entita Account per distinguere le richieste di chiusura dai conti congelati manualmente dall'impiegato. L'endpoint `all-requests` ora include gli account con status FROZEN e `closureRequestedAt` non nullo come `ACCOUNT_CLOSURE` con stato PENDING, separati dagli account gia chiusi (stato 4) che rimangono come APPROVED. Sul frontend, il componente `employee-requests` gestisce ora approve (`validateClosure`) e reject (`rejectClosure`) per il tipo `ACCOUNT_CLOSURE`, con bottoni azione mostrati per tutti i tipi di richiesta.

---

## Issue 35

**Titolo:** Rinominata pagina richieste dipendente da "Cambio Password" a "Gestione Richieste"

**Body:**
Aggiornate traduzioni IT e EN per riflettere che la pagina gestisce tutti i tipi di richiesta (cambio password, apertura e chiusura conti). Conferme di approva/rifiuta generalizzate per non riferirsi piu solo al cambio password.

---

## Issue 36

**Titolo:** Fix dropdown notifiche tagliato dal topbar

**Body:**
Dropdown delle notifiche posizionato con `position: fixed` e `z-index: 9999` per evitare che venga tagliato dal topbar fisso. Posizionamento dinamico del dropdown sotto il topbar tramite calcolo `top` nel componente. Aggiunto link "Vai alle notifiche" nell'header del dropdown per navigare alla pagina dedicata.

---

## Issue 37

**Titolo:** Pagina dedicata notifiche per customer

**Body:**
Creata componente `CustomerNotificationsComponent` con lista notifiche complete ordinate per data, mark-as-read al click e mark-all-read. Aggiunta route `/customer/notifications`, link nella sidebar e nella bottom nav mobile con icona campanella. Traduzioni IT/EN per la pagina.
