package com.javaisland.bank_backend.card.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.AccountStatusRepository;
import com.javaisland.bank_backend.card.dto.CardResponseDto;
import com.javaisland.bank_backend.card.dto.CardSensitiveDto;
import com.javaisland.bank_backend.card.model.Card;
import com.javaisland.bank_backend.card.model.CardStatus;
import com.javaisland.bank_backend.card.model.CardType;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.repository.CardStatusRepository;
import com.javaisland.bank_backend.card.repository.CardTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.security.EncryptionService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardStatusRepository cardStatusRepository;
    @Mock private CardTypeRepository cardTypeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountStatusRepository accountStatusRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private EncryptionService encryptionService;

    @InjectMocks private CardService cardService;

    private Card createCard(Long id, String cardNumber, String statusName, Long accountId) {
        Card card = new Card();
        card.setId(id);
        card.setCardNumber(cardNumber);
        card.setAccountId(accountId);
        CardStatus status = new CardStatus();
        status.setStatusName(statusName);
        status.setId(1);
        card.setStatus(status);
        CardType type = new CardType();
        type.setTypeName("DEBIT");
        card.setCardType(type);
        card.setHolderName("Test User");
        card.setExpirationDate(LocalDate.now().plusYears(5));
        card.setCvv("123");
        return card;
    }

    @Test
    void issueDebitCard_createsCard() {
        when(cardTypeRepository.findByTypeName("DEBIT")).thenReturn(Optional.of(new CardType()));
        CardStatus status = new CardStatus();
        status.setStatusName("INACTIVE");
        when(cardStatusRepository.findByStatusName("INACTIVE")).thenReturn(Optional.of(status));

        Card saved = new Card();
        saved.setId(1L);
        saved.setCardNumber("1234567890123456");
        saved.setAccountId(1L);
        saved.setCardType(new CardType());
        saved.setStatus(status);
        saved.setHolderName("Test User");
        when(cardRepository.save(any())).thenReturn(saved);
        when(cardRepository.existsByCardNumberHash(anyString())).thenReturn(false);
        when(encryptionService.hmacSha256Hex(anyString())).thenReturn("hash");

        Card result = cardService.issueDebitCard(1L, "Test User", "INACTIVE");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cardRepository).save(any());
    }

    @Test
    void getCardDetailForUser_withOwnCard_returnsDto() {
        Long userId = 1L;
        Long cardId = 10L;
        Long accountId = 100L;

        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setId(accountId);
        account.setAccountNumber("IT123");
        account.setUser(user);

        Card card = createCard(cardId, "1234567890123456", "ACTIVE", accountId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(account));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CardResponseDto result = cardService.getCardDetailForUser(userId, cardId);

        assertNotNull(result);
        assertEquals(cardId, result.getId());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void getCardDetailForUser_withOtherCard_throwsException() {
        Long userId = 1L;
        Long cardId = 10L;
        Long otherAccountId = 200L;

        User user = new User();
        user.setId(userId);

        Account userAccount = new Account();
        userAccount.setId(100L);
        userAccount.setUser(user);

        Card card = createCard(cardId, "1234567890123456", "ACTIVE", otherAccountId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(userAccount));

        assertThrows(ApiBankException.class, () -> cardService.getCardDetailForUser(userId, cardId));
    }

    @Test
    void getCardSensitiveForUser_returnsSensitiveData() {
        Long userId = 1L;
        Long cardId = 10L;
        Long accountId = 100L;

        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setId(accountId);
        account.setUser(user);

        Card card = createCard(cardId, "1234567890123456", "ACTIVE", accountId);
        card.setCvv("999");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(account));

        CardSensitiveDto result = cardService.getCardSensitiveForUser(userId, cardId);

        assertEquals("1234567890123456", result.getCardNumber());
        assertEquals("999", result.getCvv());
    }

    @Test
    void blockCardsByAccountId_blocksCards() {
        Long accountId = 100L;
        Card card = createCard(1L, "1234567890123456", "ACTIVE", accountId);

        CardStatus blockedStatus = new CardStatus();
        blockedStatus.setStatusName("BLOCKED");

        when(cardRepository.findByAccountId(accountId)).thenReturn(List.of(card));
        when(cardStatusRepository.findByStatusName("BLOCKED")).thenReturn(Optional.of(blockedStatus));

        cardService.blockCardsByAccountId(accountId);

        assertEquals("BLOCKED", card.getStatus().getStatusName());
        verify(cardRepository).save(card);
    }

    @Test
    void updateCardStatus_blockedCard_throwsException() {
        Long cardId = 1L;
        Card card = createCard(cardId, "1234567890123456", "BLOCKED", 100L);

        CardStatus blockedStatus = new CardStatus();
        blockedStatus.setStatusName("BLOCKED");
        blockedStatus.setId(1);
        card.setStatus(blockedStatus);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardStatusRepository.findByStatusName("BLOCKED")).thenReturn(Optional.of(blockedStatus));

        assertThrows(ApiBankException.class, () -> cardService.updateCardStatus(cardId, "ACTIVE"));
    }

    @Test
    void unblockCard_activatesCard() {
        Long cardId = 1L;
        Card card = createCard(cardId, "1234567890123456", "BLOCKED", 100L);

        Account account = new Account();
        account.setUser(new User());

        CardStatus activeStatus = new CardStatus();
        activeStatus.setStatusName("ACTIVE");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardStatusRepository.findByStatusName("ACTIVE")).thenReturn(Optional.of(activeStatus));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        cardService.unblockCard(cardId);

        assertEquals("ACTIVE", card.getStatus().getStatusName());
        verify(cardRepository).save(card);
    }

    @Test
    void unblockCard_accountBlocked_throwsException() {
        Long cardId = 1L;
        Card card = createCard(cardId, "1234567890123456", "BLOCKED", 100L);

        Account account = new Account();
        account.setStatusId(AccountStatus.FROZEN);
        account.setUser(new User());

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        assertThrows(ApiBankException.class, () -> cardService.unblockCard(cardId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void unblockCard_accountClosed_throwsException() {
        Long cardId = 1L;
        Card card = createCard(cardId, "1234567890123456", "BLOCKED", 100L);

        Account account = new Account();
        account.setStatusId(AccountStatus.CLOSED);
        account.setUser(new User());

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        assertThrows(ApiBankException.class, () -> cardService.unblockCard(cardId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void issueDebitCard_encryptsSensitiveData() {
        when(cardTypeRepository.findByTypeName("DEBIT")).thenReturn(Optional.of(new CardType()));
        CardStatus status = new CardStatus();
        status.setStatusName("INACTIVE");
        when(cardStatusRepository.findByStatusName("INACTIVE")).thenReturn(Optional.of(status));
        when(cardRepository.existsByCardNumberHash(anyString())).thenReturn(false);
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(encryptionService.encrypt(anyString())).thenReturn("cipher");
        when(encryptionService.hmacSha256Hex(anyString())).thenReturn("hash");

        Card result = cardService.issueDebitCard(1L, "Test User", "INACTIVE");

        assertNotNull(result);
        assertNull(result.getCardNumber(), "plaintext card number must not be persisted");
        assertNull(result.getCvv(), "plaintext cvv must not be persisted");
        assertNotNull(result.getCardNumberHash());
        assertNotNull(result.getCardNumberEnc());
        assertNotNull(result.getCvvEnc());
        verify(encryptionService, times(2)).encrypt(anyString());
    }

    @Test
    void getCardSensitiveForUser_withEncryptedFields_returnsDecryptedValues() {
        Long userId = 1L;
        Long cardId = 10L;
        Long accountId = 100L;

        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setId(accountId);
        account.setUser(user);

        Card card = createCard(cardId, null, "ACTIVE", accountId);
        card.setCardNumberEnc("enc-number");
        card.setCvvEnc("enc-cvv");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(account));
        when(encryptionService.decrypt("enc-number")).thenReturn("1234567890123456");
        when(encryptionService.decrypt("enc-cvv")).thenReturn("999");

        CardSensitiveDto result = cardService.getCardSensitiveForUser(userId, cardId);

        assertEquals("1234567890123456", result.getCardNumber());
        assertEquals("999", result.getCvv());
    }

    @Test
    void getCardDetailForUser_withEncryptedFields_returnsMaskedNumber() {
        Long userId = 1L;
        Long cardId = 10L;
        Long accountId = 100L;

        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setId(accountId);
        account.setAccountNumber("IT123");
        account.setUser(user);

        Card card = createCard(cardId, null, "ACTIVE", accountId);
        card.setCardNumberEnc("enc-number");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(account));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(encryptionService.decrypt("enc-number")).thenReturn("1234567890123456");

        CardResponseDto result = cardService.getCardDetailForUser(userId, cardId);

        assertEquals("****3456", result.getMaskedCardNumber());
    }
}
