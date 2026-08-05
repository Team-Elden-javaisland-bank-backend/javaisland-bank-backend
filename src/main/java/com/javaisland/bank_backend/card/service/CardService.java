package com.javaisland.bank_backend.card.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.AccountStatusRepository;
import com.javaisland.bank_backend.card.dto.CardResponseDto;
import com.javaisland.bank_backend.card.dto.CardSensitiveDto;
import com.javaisland.bank_backend.card.model.Card;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.repository.CardStatusRepository;
import com.javaisland.bank_backend.card.repository.CardTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.security.EncryptionService;
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
    private final AccountStatusRepository accountStatusRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EncryptionService encryptionService;
    private final Random random = new Random();

    public CardService(CardRepository cardRepository,
                       CardStatusRepository cardStatusRepository,
                       CardTypeRepository cardTypeRepository,
                       AccountRepository accountRepository,
                       AccountStatusRepository accountStatusRepository,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       EncryptionService encryptionService) {
        this.cardRepository = cardRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.cardTypeRepository = cardTypeRepository;
        this.accountRepository = accountRepository;
        this.accountStatusRepository = accountStatusRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public Card issueDebitCard(Long accountId, String holderName) {
        return issueDebitCard(accountId, holderName, "INACTIVE");
    }

    @Transactional
    public Card issueDebitCard(Long accountId, String holderName, String statusName) {
        var debitType = cardTypeRepository.findByTypeName("DEBIT")
                .orElseThrow(() -> new ApiBankException("CARD_TYPE_NOT_FOUND", "CARD_TYPE_NOT_FOUND"));
        var status = cardStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));

        Card card = new Card();
        card.setAccountId(accountId);
        card.setHolderName(holderName);
        card.setCardType(debitType);
        card.setStatus(status);
        card.setExpirationDate(LocalDate.now().plusYears(5));
        String plainCardNumber = generateUniqueCardNumber();
        String plainCvv = String.format("%03d", random.nextInt(900) + 100);
        card.setCardNumberEnc(encryptionService.encrypt(plainCardNumber));
        card.setCvvEnc(encryptionService.encrypt(plainCvv));
        card.setCardNumberHash(encryptionService.hmacSha256Hex(plainCardNumber));

        return cardRepository.save(card);
    }

    @Transactional
    public void activateCardsByAccountId(Long accountId) {
        var activeStatus = cardStatusRepository.findByStatusName("ACTIVE")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
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

    @Transactional
    public void blockCardsByAccountId(Long accountId) {
        var blockedStatus = cardStatusRepository.findByStatusName("BLOCKED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        cardRepository.findByAccountId(accountId).forEach(card -> {
            if (!card.getStatus().getStatusName().equals("BLOCKED")) {
                card.setStatus(blockedStatus);
                cardRepository.save(card);
            }
        });
    }

    @Transactional
    public void closeCardsByAccountId(Long accountId) {
        var closedStatus = cardStatusRepository.findByStatusName("CLOSED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        cardRepository.findByAccountId(accountId).forEach(card -> {
            if (!card.getStatus().getStatusName().equals("CLOSED")) {
                card.setStatus(closedStatus);
                cardRepository.save(card);
            }
        });
    }

    @Transactional
    public void unblockCardsByAccountId(Long accountId) {
        var activeStatus = cardStatusRepository.findByStatusName("ACTIVE")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        cardRepository.findByAccountId(accountId).forEach(card -> {
            card.setStatus(activeStatus);
            cardRepository.save(card);
        });
    }

    @Transactional
    public Card updateCardStatus(Long cardId, String newStatusName) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("CARD_NOT_FOUND", "CARD_NOT_FOUND"));

        var blockedStatus = cardStatusRepository.findByStatusName("BLOCKED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));

        if (card.getStatus().getId().equals(blockedStatus.getId())) {
            throw new ApiBankException("CARD_BLOCKED_PERMANENT", "CARD_BLOCKED_PERMANENT");
        }

        var newStatus = cardStatusRepository.findByStatusName(newStatusName)
                .orElseThrow(() -> new ApiBankException("CARD_INVALID_STATUS", "CARD_INVALID_STATUS"));

        card.setStatus(newStatus);
        Card saved = cardRepository.save(card);
        if ("BLOCKED".equals(newStatusName)) {
            accountRepository.findById(card.getAccountId()).ifPresent(account -> {
                if (account.getUser() != null) {
                    notificationService.send(account.getUser().getId(), "CARD", "Card ending in " + last4Of(card) + " has been blocked.", "NOTIF_CARD_BLOCKED", "[\"" + last4Of(card) + "\"]");
                }
            });
        }
        return saved;
    }

    @Transactional
    public Card unblockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("CARD_NOT_FOUND", "CARD_NOT_FOUND"));

        Account account = accountRepository.findById(card.getAccountId())
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));

        if (account.getStatusId() != null
                && (account.getStatusId() == AccountStatus.FROZEN || account.getStatusId() == AccountStatus.CLOSED)) {
            throw new ApiBankException("CARD_UNBLOCK_ACCOUNT_BLOCKED", "CARD_UNBLOCK_ACCOUNT_BLOCKED");
        }

        var activeStatus = cardStatusRepository.findByStatusName("ACTIVE")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));

        card.setStatus(activeStatus);
        Card saved = cardRepository.save(card);
        accountRepository.findById(card.getAccountId()).ifPresent(acc -> {
            if (acc.getUser() != null) {
                notificationService.send(acc.getUser().getId(), "CARD", "Card ending in " + last4Of(card) + " has been unblocked.", "NOTIF_CARD_UNBLOCKED", "[\"" + last4Of(card) + "\"]");
            }
        });
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardsByUserId(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
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
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("CARD_NOT_FOUND", "CARD_NOT_FOUND"));
        var accounts = accountRepository.findByUserId(user.getId());
        var ownsAccount = accounts.stream().anyMatch(a -> a.getId().equals(card.getAccountId()));
        if (!ownsAccount) {
            throw new ApiBankException("FORBIDDEN", "FORBIDDEN");
        }
        return toDto(card);
    }

    @Transactional(readOnly = true)
    public CardSensitiveDto getCardSensitiveForUser(Long userId, Long cardId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiBankException("CARD_NOT_FOUND", "CARD_NOT_FOUND"));
        var accounts = accountRepository.findByUserId(user.getId());
        var ownsAccount = accounts.stream().anyMatch(a -> a.getId().equals(card.getAccountId()));
        if (!ownsAccount) {
            throw new ApiBankException("FORBIDDEN", "FORBIDDEN");
        }
        return CardSensitiveDto.builder()
                .cardNumber(decryptCardNumber(card))
                .cvv(decryptCvv(card))
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
                .orElseThrow(() -> new ApiBankException("CARD_NOT_FOUND", "CARD_NOT_FOUND"));
        return toDto(card);
    }

    private CardResponseDto toDto(Card card) {
        String acctNum = accountRepository.findById(card.getAccountId())
                .map(a -> a.getAccountNumber())
                .orElse(null);
        String acctStatus = accountRepository.findById(card.getAccountId())
                .map(a -> a.getStatusId() != null
                        ? accountStatusRepository.findById(a.getStatusId()).map(AccountStatus::getStatusName).orElse(null)
                        : null)
                .orElse(null);
        return CardResponseDto.builder()
                .id(card.getId())
                .maskedCardNumber(maskCardNumber(card))
                .holderName(card.getHolderName())
                .expirationDate(card.getExpirationDate())
                .cardType(card.getCardType().getTypeName())
                .status(card.getStatus().getStatusName())
                .accountId(card.getAccountId())
                .accountNumber(acctNum)
                .accountStatus(acctStatus)
                .build();
    }

    public String maskCardNumber(Card card) {
        String full = decryptCardNumber(card);
        if (full == null || full.length() < 4) {
            return "****";
        }
        return "****" + full.substring(full.length() - 4);
    }

    public String last4Of(Card card) {
        String full = decryptCardNumber(card);
        if (full == null || full.length() < 4) {
            return "****";
        }
        return full.substring(full.length() - 4);
    }

    private String decryptCardNumber(Card card) {
        if (card.getCardNumberEnc() != null && !card.getCardNumberEnc().isBlank()) {
            return encryptionService.decrypt(card.getCardNumberEnc());
        }
        return card.getCardNumber();
    }

    private String decryptCvv(Card card) {
        if (card.getCvvEnc() != null && !card.getCvvEnc().isBlank()) {
            return encryptionService.decrypt(card.getCvvEnc());
        }
        return card.getCvv();
    }

    private String generateUniqueCardNumber() {
        String generatedNumber;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 15; i++) {
                sb.append(random.nextInt(10));
            }
            generatedNumber = sb.toString() + luhnCheckDigit(sb.toString());
        } while (cardRepository.existsByCardNumberHash(encryptionService.hmacSha256Hex(generatedNumber)));

        return generatedNumber;
    }

    private String luhnCheckDigit(String digits) {
        int sum = 0;
        boolean alternate = true;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return String.valueOf((10 - (sum % 10)) % 10);
    }
}