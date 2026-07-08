package com.javaisland.bank_backend.card;

import com.javaisland.bank_backend.card.CardStatusRepository;
import com.javaisland.bank_backend.card.CardTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Random;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CardStatusRepository cardStatusRepository;
    private final CardTypeRepository cardTypeRepository;
    private final Random random = new Random();

    public CardService(CardRepository cardRepository,
                       CardStatusRepository cardStatusRepository,
                       CardTypeRepository cardTypeRepository) {
        this.cardRepository = cardRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.cardTypeRepository = cardTypeRepository;
    }

    public Card issueNewCard(CardIssueDTO dto) {
        if (!dto.getHolderName().matches("^[a-zA-Z\\s]+$")) {
            throw new ApiBankException("Il nome del titolare può contenere solo lettere e spazi.");
        }

        var cardType = cardTypeRepository.findByTypeName(dto.getCardType())
                .orElseThrow(() -> new ApiBankException("Tipo carta '" + dto.getCardType() + "' non valido."));
        var inactiveStatus = cardStatusRepository.findByStatusName("INACTIVE")
                .orElseThrow(() -> new ApiBankException("Stato carta INACTIVE non configurato."));

        Card card = new Card();
        card.setAccountId(dto.getAccountId());
        card.setHolderName(dto.getHolderName());
        card.setCardType(cardType);
        card.setStatus(inactiveStatus);
        card.setExpirationDate(LocalDate.now().plusYears(5));
        card.setCvv(String.format("%03d", random.nextInt(1000)));
        card.setCardNumber(generateUniqueCardNumber());

        return cardRepository.save(card);
    }

    public Card updateCardStatus(Long cardId, String newStatusName) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata con questo ID."));

        var blockedStatus = cardStatusRepository.findByStatusName("BLOCKED")
                .orElseThrow(() -> new ApiBankException("Stato carta BLOCKED non configurato."));

        if (card.getStatus().getId().equals(blockedStatus.getId())) {
            throw new ApiBankException("Impossibile modificare lo stato: la carta è BLOCCATA definitivamente.");
        }

        var newStatus = cardStatusRepository.findByStatusName(newStatusName)
                .orElseThrow(() -> new ApiBankException("Stato carta '" + newStatusName + "' non valido."));

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