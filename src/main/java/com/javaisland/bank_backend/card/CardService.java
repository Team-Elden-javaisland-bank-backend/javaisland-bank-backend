package com.javaisland.bank_backend.card;

import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Random;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final Random random = new Random();

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    // 🧠 LOGICA PER GENERARE UNA NUOVA CARTA (Usa CardType Enum)
    public Card issueNewCard(Long accountId, String holderName, CardType cardType) {
        Card card = new Card();
        card.setAccountId(accountId);
        card.setHolderName(holderName);
        card.setCardType(cardType);

        // Impostiamo lo stato iniziale usando l'Enum
        card.setStatus(CardStatus.INACTIVE);

        card.setExpirationDate(LocalDate.now().plusYears(5));
        card.setCvv(String.format("%03d", random.nextInt(1000)));
        card.setCardNumber(generateUniqueCardNumber());

        return cardRepository.save(card);
    }

    // 🧠 ATTIVAZIONE O BLOCCO DELLA CARTA (Usa CardStatus Enum)
    public Card updateCardStatus(Long cardId, CardStatus newStatus) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata con questo ID."));

        // Se la carta è bloccata in modo definitivo, blocca la modifica
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ApiBankException("Impossibile modificare lo stato: la carta è BLOCCATA definitivamente.");
        }

        card.setStatus(newStatus);
        return cardRepository.save(card);
    }

    private String generateUniqueCardNumber() {
        String generatedNumber;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));
            }
            generatedNumber = sb.toString();
        } while (cardRepository.findByCardNumber(generatedNumber).isPresent());

        return generatedNumber;
    }
}