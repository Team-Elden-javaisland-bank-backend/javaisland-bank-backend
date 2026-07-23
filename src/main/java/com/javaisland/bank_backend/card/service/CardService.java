package com.javaisland.bank_backend.card.service;

import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.card.dto.CardResponseDto;
import com.javaisland.bank_backend.card.dto.CardSensitiveDto;
import com.javaisland.bank_backend.card.model.Card;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.repository.CardStatusRepository;
import com.javaisland.bank_backend.card.repository.CardTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CardStatusRepository cardStatusRepository;
    private final CardTypeRepository cardTypeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Random random = new Random();

    public CardService(CardRepository cardRepository,
                       CardStatusRepository cardStatusRepository,
                       CardTypeRepository cardTypeRepository,
                       AccountRepository accountRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.cardRepository = cardRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.cardTypeRepository = cardTypeRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Card issueDebitCard(Long accountId, String holderName) {
        return issueDebitCard(accountId, holderName, "INACTIVE");
    }

    public Card issueDebitCard(Long accountId, String holderName, String statusName) {
        var debitType = cardTypeRepository.findByTypeName("DEBIT")
                .orElseThrow(() -> new ApiBankException("Tipo carta DEBIT non configurato."));
        var status = cardStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new ApiBankException("Stato carta " + statusName + " non configurato."));

        Card card = new Card();
        card.setAccountId(accountId);
        card.setHolderName(holderName);
        card.setCardType(debitType);
        card.setStatus(status);
        card.setExpirationDate(LocalDate.now().plusYears(5));
        card.setCvv(String.format("%03d", random.nextInt(1000)));
        card.setCardNumber(generateUniqueCardNumber());

        return cardRepository.save(card);
    }

    @Transactional
    public void activateCardsByAccountId(Long accountId) {
        var activeStatus = cardStatusRepository.findByStatusName("ACTIVE")
                .orElseThrow(() -> new ApiBankException("Stato carta ACTIVE non configurato."));
        cardRepository.findByAccountId(accountId).forEach(card -> {
            if (!card.getStatus().getStatusName().equals("BLOCKED")) {
                card.setStatus(activeStatus);
                cardRepository.save(card);
            }
        });
    }

    @Transactional
    public void deleteCardsByAccountId(Long accountId) {
        cardRepository.findByAccountId(accountId).forEach(cardRepository::delete);
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
        Card saved = cardRepository.save(card);
        if ("BLOCKED".equals(newStatusName)) {
            accountRepository.findById(card.getAccountId()).ifPresent(account ->
                notificationService.send(account.getUser().getId(), "CARD", "La carta terminata in " + card.getCardNumber().substring(12) + " è stata bloccata.", "NOTIF_CARD_BLOCKED", "[\"" + card.getCardNumber().substring(12) + "\"]")
            );
        }
        return saved;
    }

    @Transactional
    public Card unblockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata con questo ID."));

        var activeStatus = cardStatusRepository.findByStatusName("ACTIVE")
                .orElseThrow(() -> new ApiBankException("Stato carta ACTIVE non configurato."));

        card.setStatus(activeStatus);
        Card saved = cardRepository.save(card);
        accountRepository.findById(card.getAccountId()).ifPresent(account ->
            notificationService.send(account.getUser().getId(), "CARD", "La carta " + card.getCardNumber().substring(12) + " è stata sbloccata.", "NOTIF_CARD_UNBLOCKED", "[\"" + card.getCardNumber().substring(12) + "\"]")
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardsByUserId(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
        var accounts = accountRepository.findByUserId(user.getId());
        if (accounts.isEmpty()) return List.of();
        var accountIds = accounts.stream().map(a -> a.getId()).toList();
        return cardRepository.findByAccountIdIn(accountIds).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponseDto getCardDetailForUser(Long userId, Long cardId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata.", "CARD_NOT_FOUND"));
        var accounts = accountRepository.findByUserId(user.getId());
        var ownsAccount = accounts.stream().anyMatch(a -> a.getId().equals(card.getAccountId()));
        if (!ownsAccount) {
            throw new ApiBankException("Carta non appartiene all'utente.", "FORBIDDEN");
        }
        return toDto(card);
    }

    @Transactional(readOnly = true)
    public CardSensitiveDto getCardSensitiveForUser(Long userId, Long cardId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata.", "CARD_NOT_FOUND"));
        var accounts = accountRepository.findByUserId(user.getId());
        var ownsAccount = accounts.stream().anyMatch(a -> a.getId().equals(card.getAccountId()));
        if (!ownsAccount) {
            throw new ApiBankException("Carta non appartiene all'utente.", "FORBIDDEN");
        }
        return CardSensitiveDto.builder()
                .cardNumber(card.getCardNumber())
                .cvv(card.getCvv())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CardResponseDto> getAllCards() {
        return cardRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardsByAccountId(Long accountId) {
        return cardRepository.findByAccountId(accountId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponseDto getCardDetail(Long cardId) {
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata.", "CARD_NOT_FOUND"));
        return toDto(card);
    }

    @Transactional(readOnly = true)
    public CardSensitiveDto getCardSensitiveByCardId(Long cardId) {
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("Carta non trovata.", "CARD_NOT_FOUND"));
        return CardSensitiveDto.builder()
                .cardNumber(card.getCardNumber())
                .cvv(card.getCvv())
                .build();
    }

    private CardResponseDto toDto(Card card) {
        String acctNum = accountRepository.findById(card.getAccountId())
                .map(a -> a.getAccountNumber())
                .orElse(null);
        return CardResponseDto.builder()
                .id(card.getId())
                .maskedCardNumber("****" + card.getCardNumber().substring(12))
                .holderName(card.getHolderName())
                .expirationDate(card.getExpirationDate())
                .cardType(card.getCardType().getTypeName())
                .status(card.getStatus().getStatusName())
                .accountId(card.getAccountId())
                .accountNumber(acctNum)
                .build();
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