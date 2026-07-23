-- One-time migration: set messageKey + messageParams on existing notifications
-- Uses PostgreSQL regex to extract params from Italian text

-- NOTIF_ACCOUNT_ACTIVATED
UPDATE notifications
SET message_key = 'NOTIF_ACCOUNT_ACTIVATED',
    message_params = CONCAT('["', regexp_replace(message, '^Il tuo conto (.+) è stato attivato\.$', '\1'), '"]')
WHERE message ~ '^Il tuo conto .+ è stato attivato\.$' AND message_key IS NULL;

-- NOTIF_ACCOUNT_REJECTED
UPDATE notifications
SET message_key = 'NOTIF_ACCOUNT_REJECTED',
    message_params = CONCAT('["', regexp_replace(message, '^La richiesta di apertura conto (.+) è stata rifiutata\.$', '\1'), '"]')
WHERE message ~ '^La richiesta di apertura conto .+ è stata rifiutata\.$' AND message_key IS NULL;

-- NOTIF_CLOSURE_REQUESTED
UPDATE notifications
SET message_key = 'NOTIF_CLOSURE_REQUESTED',
    message_params = CONCAT('["', regexp_replace(message, '^Richiesta di chiusura conto (.+) inviata\. In attesa di approvazione\.$', '\1'), '"]')
WHERE message ~ '^Richiesta di chiusura conto .+ inviata\. In attesa di approvazione\.$' AND message_key IS NULL;

-- NOTIF_CLOSURE_REJECTED (short version, no riattivato)
UPDATE notifications
SET message_key = 'NOTIF_CLOSURE_REJECTED',
    message_params = CONCAT('["', regexp_replace(message, '^La richiesta di chiusura del conto (.+) è stata rifiutata\.$', '\1'), '"]')
WHERE message ~ '^La richiesta di chiusura del conto .+ è stata rifiutata\.$' AND message_key IS NULL;

-- NOTIF_CLOSURE_REJECTED_UNFREEZE (long version)
UPDATE notifications
SET message_key = 'NOTIF_CLOSURE_REJECTED_UNFREEZE',
    message_params = CONCAT('["', regexp_replace(message, '^La richiesta di chiusura del conto (.+) è stata rifiutata\. Il conto è stato riattivato\.$', '\1'), '"]')
WHERE message ~ '^La richiesta di chiusura del conto .+ è stata rifiutata\. Il conto è stato riattivato\.$' AND message_key IS NULL;

-- NOTIF_ACCOUNT_FROZEN
UPDATE notifications
SET message_key = 'NOTIF_ACCOUNT_FROZEN',
    message_params = CONCAT('["', regexp_replace(message, '^Il tuo conto (.+) è stato congelato da un impiegato\.$', '\1'), '"]')
WHERE message ~ '^Il tuo conto .+ è stato congelato da un impiegato\.$' AND message_key IS NULL;

-- NOTIF_ACCOUNT_UNFROZEN
UPDATE notifications
SET message_key = 'NOTIF_ACCOUNT_UNFROZEN',
    message_params = CONCAT('["', regexp_replace(message, '^Il tuo conto (.+) è stato sbloccato\.$', '\1'), '"]')
WHERE message ~ '^Il tuo conto .+ è stato sbloccato\.$' AND message_key IS NULL;

-- NOTIF_ACCOUNT_CLOSED
UPDATE notifications
SET message_key = 'NOTIF_ACCOUNT_CLOSED',
    message_params = CONCAT('["', regexp_replace(message, '^Il tuo conto (.+) è stato chiuso\.$', '\1'), '"]')
WHERE message ~ '^Il tuo conto .+ è stato chiuso\.$' AND message_key IS NULL;

-- NOTIF_ACCOUNT_OPEN_REQUESTED
UPDATE notifications
SET message_key = 'NOTIF_ACCOUNT_OPEN_REQUESTED',
    message_params = CONCAT('["', regexp_replace(message, '^Richiesta di apertura conto (.+) inviata\. In attesa di approvazione\.$', '\1'), '"]')
WHERE message ~ '^Richiesta di apertura conto .+ inviata\. In attesa di approvazione\.$' AND message_key IS NULL;

-- NOTIF_REGISTRATION_APPROVED
UPDATE notifications
SET message_key = 'NOTIF_REGISTRATION_APPROVED', message_params = NULL
WHERE message = 'Registrazione approvata! Il tuo conto è attivo.' AND message_key IS NULL;

-- NOTIF_REGISTRATION_REJECTED
UPDATE notifications
SET message_key = 'NOTIF_REGISTRATION_REJECTED', message_params = NULL
WHERE message = 'La tua registrazione è stata rifiutata.' AND message_key IS NULL;

-- NOTIF_PWD_CHANGE_REQUESTED
UPDATE notifications
SET message_key = 'NOTIF_PWD_CHANGE_REQUESTED', message_params = NULL
WHERE message = 'Richiesta di cambio password inviata. In attesa di approvazione.' AND message_key IS NULL;

-- NOTIF_PWD_CHANGE_APPROVED
UPDATE notifications
SET message_key = 'NOTIF_PWD_CHANGE_APPROVED', message_params = NULL
WHERE message = 'La tua richiesta di cambio password è stata approvata.' AND message_key IS NULL;

-- NOTIF_PWD_CHANGE_REJECTED
UPDATE notifications
SET message_key = 'NOTIF_PWD_CHANGE_REJECTED', message_params = NULL
WHERE message = 'La tua richiesta di cambio password è stata rifiutata.' AND message_key IS NULL;

-- NOTIF_CARD_BLOCKED
UPDATE notifications
SET message_key = 'NOTIF_CARD_BLOCKED',
    message_params = CONCAT('["', regexp_replace(message, '^La carta terminata in (.+) è stata bloccata\.$', '\1'), '"]')
WHERE message ~ '^La carta terminata in .+ è stata bloccata\.$' AND message_key IS NULL;

-- NOTIF_CARD_UNBLOCKED
UPDATE notifications
SET message_key = 'NOTIF_CARD_UNBLOCKED',
    message_params = CONCAT('["', regexp_replace(message, '^La carta (.+) è stata sbloccata\.$', '\1'), '"]')
WHERE message ~ '^La carta .+ è stata sbloccata\.$' AND message_key IS NULL;

-- NOTIF_DEPOSIT_COMPLETED: "Deposito di €50 sul conto ITD0E5C3B245144AF completato."
UPDATE notifications
SET message_key = 'NOTIF_DEPOSIT_COMPLETED',
    message_params = CONCAT('["', regexp_replace(message, '^Deposito di €(.+) sul conto (.+) completato\.$', '\1'), '", "', regexp_replace(message, '^Deposito di €(.+) sul conto (.+) completato\.$', '\2'), '"]')
WHERE message ~ '^Deposito di €.+ sul conto .+ completato\.$' AND message_key IS NULL;

-- NOTIF_WITHDRAWAL_COMPLETED
UPDATE notifications
SET message_key = 'NOTIF_WITHDRAWAL_COMPLETED',
    message_params = CONCAT('["', regexp_replace(message, '^Prelievo di €(.+) dal conto (.+) completato\.$', '\1'), '", "', regexp_replace(message, '^Prelievo di €(.+) dal conto (.+) completato\.$', '\2'), '"]')
WHERE message ~ '^Prelievo di €.+ dal conto .+ completato\.$' AND message_key IS NULL;

-- NOTIF_LIMIT_CHANGE_REQUESTED
UPDATE notifications
SET message_key = 'NOTIF_LIMIT_CHANGE_REQUESTED',
    message_params = CONCAT('["', regexp_replace(message, '^Richiesta di modifica limite (.+) inviata\. In attesa di approvazione\.$', '\1'), '"]')
WHERE message ~ '^Richiesta di modifica limite .+ inviata\. In attesa di approvazione\.$' AND message_key IS NULL;

-- NOTIF_LIMIT_CHANGE_APPROVED (note: "e stata" without accent in original)
UPDATE notifications
SET message_key = 'NOTIF_LIMIT_CHANGE_APPROVED',
    message_params = CONCAT('["', regexp_replace(message, '^La tua richiesta di modifica limite (.+) e stata approvata\.$', '\1'), '"]')
WHERE message ~ '^La tua richiesta di modifica limite .+ e stata approvata\.$' AND message_key IS NULL;

-- NOTIF_LIMIT_CHANGE_REJECTED (note: "e stata" without accent in original)
UPDATE notifications
SET message_key = 'NOTIF_LIMIT_CHANGE_REJECTED',
    message_params = CONCAT('["', regexp_replace(message, '^La tua richiesta di modifica limite (.+) e stata rifiutata\.$', '\1'), '"]')
WHERE message ~ '^La tua richiesta di modifica limite .+ e stata rifiutata\.$' AND message_key IS NULL;

-- NOTIF_TRANSFER_COMPLETED: "Bonifico di €XXX a YYY completato."
UPDATE notifications
SET message_key = 'NOTIF_TRANSFER_COMPLETED',
    message_params = CONCAT('["', regexp_replace(message, '^Bonifico di €(.+) a (.+) completato\.$', '\1'), '", "', regexp_replace(message, '^Bonifico di €(.+) a (.+) completato\.$', '\2'), '"]')
WHERE message ~ '^Bonifico di €.+ a .+ completato\.$' AND message_key IS NULL;

-- NOTIF_SCHEDULED_TRANSFER_CREATED: "Bonifico programmato di €XXX a YYY per il ZZZ."
UPDATE notifications
SET message_key = 'NOTIF_SCHEDULED_TRANSFER_CREATED',
    message_params = CONCAT('["', regexp_replace(message, '^Bonifico programmato di €(.+) a (.+) per il (.+)\.$', '\1'), '", "', regexp_replace(message, '^Bonifico programmato di €(.+) a (.+) per il (.+)\.$', '\2'), '", "', regexp_replace(message, '^Bonifico programmato di €(.+) a (.+) per il (.+)\.$', '\3'), '"]')
WHERE message ~ '^Bonifico programmato di €.+ a .+ per il .+\.$' AND message_key IS NULL;

-- NOTIF_SCHEDULED_TRANSFER_EXECUTED: "Bonifico programmato di €XXX eseguito verso YYY."
UPDATE notifications
SET message_key = 'NOTIF_SCHEDULED_TRANSFER_EXECUTED',
    message_params = CONCAT('["', regexp_replace(message, '^Bonifico programmato di €(.+) eseguito verso (.+)\.$', '\1'), '", "', regexp_replace(message, '^Bonifico programmato di €(.+) eseguito verso (.+)\.$', '\2'), '"]')
WHERE message ~ '^Bonifico programmato di €.+ eseguito verso .+\.$' AND message_key IS NULL;

-- NOTIF_TRANSFER_RECEIVED: "Ricevuto bonifico di €XXX da YYY."
UPDATE notifications
SET message_key = 'NOTIF_TRANSFER_RECEIVED',
    message_params = CONCAT('["', regexp_replace(message, '^Ricevuto bonifico di €(.+) da (.+)\.$', '\1'), '", "', regexp_replace(message, '^Ricevuto bonifico di €(.+) da (.+)\.$', '\2'), '"]')
WHERE message ~ '^Ricevuto bonifico di €.+ da .+\.$' AND message_key IS NULL;

-- NOTIF_TRANSACTION_CANCELLED: "Transazione #XXX annullata."
UPDATE notifications
SET message_key = 'NOTIF_TRANSACTION_CANCELLED',
    message_params = CONCAT('["', regexp_replace(message, '^Transazione #(.+) annullata\.$', '\1'), '"]')
WHERE message ~ '^Transazione #.+ annullata\.$' AND message_key IS NULL;
