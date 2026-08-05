package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.AccountHolderDto;
import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.DashboardSummaryDto;
import com.javaisland.bank_backend.account.dto.EmployeeUserDetailDto;
import com.javaisland.bank_backend.account.dto.MonthlySummaryDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountQueryService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final CardService cardService;

    @Transactional(readOnly = true)
    public boolean isLastActiveAccount(Long userId) {
        long activeCount = accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() == AccountStatus.ACTIVE)
                .count();
        return activeCount <= 1;
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByUserId(Long userId) {
        User user = getUserOrThrow(userId);
        return accountRepository.findByUserId(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonthlySummaryDto getMonthlySummary(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        OffsetDateTime start = startOfMonth.atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        OffsetDateTime end = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();

        Long accountId = account.getId();
        BigDecimal income = transactionRepository.sumInflowByAccountBetween(accountId, TransactionStatus.COMPLETED, start, end);
        Long movementCount = transactionRepository.countByAccountBetween(accountId, TransactionStatus.COMPLETED, start, end);

        return MonthlySummaryDto.builder()
                .monthlyIncome(income)
                .movementCount(movementCount)
                .balanceChangePercentage(BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(Long userId) {
        User user = getUserOrThrow(userId);
        List<Account> accounts = accountRepository.findByUserId(user.getId());

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Rome"));
        OffsetDateTime monthStart = LocalDate.now().withDayOfMonth(1)
                .atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime();

        BigDecimal totalCurrentBalance = BigDecimal.ZERO;
        BigDecimal totalPreviousMonthBalance = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (account.getStatusId() != AccountStatus.ACTIVE) continue;
            Long accountId = account.getId();
            BigDecimal inflow = transactionRepository
                    .sumInflowByAccountBetween(accountId, TransactionStatus.COMPLETED, monthStart, now);
            BigDecimal outflow = transactionRepository
                    .sumOutflowByAccountBetween(accountId, TransactionStatus.COMPLETED, monthStart, now);
            BigDecimal current = account.getBalance();
            BigDecimal previousMonth = current.subtract(inflow).add(outflow);
            totalCurrentBalance = totalCurrentBalance.add(current);
            totalPreviousMonthBalance = totalPreviousMonthBalance.add(previousMonth);
        }

        BigDecimal balanceChangeAbsolute = totalCurrentBalance.subtract(totalPreviousMonthBalance);
        BigDecimal balanceChangePercentage = totalPreviousMonthBalance.signum() == 0
                ? (totalCurrentBalance.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100))
                : balanceChangeAbsolute
                        .divide(totalPreviousMonthBalance.abs(), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return DashboardSummaryDto.builder()
                .totalCurrentBalance(totalCurrentBalance)
                .totalPreviousMonthBalance(totalPreviousMonthBalance)
                .balanceChangeAbsolute(balanceChangeAbsolute)
                .balanceChangePercentage(balanceChangePercentage)
                .build();
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetail(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);
        return toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetailAsEmployee(String accountNumber) {
        return toDto(getAccountOrThrow(accountNumber));
    }

    @Transactional(readOnly = true)
    public AccountHolderDto getAccountHolderInfo(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        User user = account.getUser();
        return AccountHolderDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .accountNumber(account.getAccountNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeUserDetailDto getEmployeeUserDetail(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        return EmployeeUserDetailDto.builder()
                .userId(account.getUser().getId())
                .username(account.getUser().getUsername())
                .firstName(account.getUser().getFirstName())
                .lastName(account.getUser().getLastName())
                .email(account.getUser().getEmail())
                .birthDate(account.getUser().getBirthDate())
                .profession(account.getUser().getProfession())
                .gender(account.getUser().getGender())
                .fiscalCode(account.getUser().getFiscalCode())
                .phone(account.getUser().getPhone())
                .residence(account.getUser().getResidence())
                .birthPlace(account.getUser().getBirthPlace())
                .birthProvince(account.getUser().getBirthProvince())
                .profilePictureUrl(account.getUser().getProfilePictureUrl())
                .userStatus(account.getUser().getStatus().getUserStatus())
                .userCreatedAt(account.getUser().getCreatedAt())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .accountStatus(getStatusName(account.getStatusId()))
                .accountCreatedAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .cards(toCardSummaryList(account.getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeUserDetailDto getEmployeeUserDetailByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        Optional<Account> accountOpt = accountRepository.findByUserId(userId).stream().findFirst();

        EmployeeUserDetailDto.EmployeeUserDetailDtoBuilder builder = EmployeeUserDetailDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .profession(user.getProfession())
                .gender(user.getGender())
                .fiscalCode(user.getFiscalCode())
                .phone(user.getPhone())
                .residence(user.getResidence())
                .birthPlace(user.getBirthPlace())
                .birthProvince(user.getBirthProvince())
                .profilePictureUrl(user.getProfilePictureUrl())
                .userStatus(user.getStatus().getUserStatus())
                .userCreatedAt(user.getCreatedAt());

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            builder.accountNumber(account.getAccountNumber())
                    .balance(account.getBalance())
                    .accountStatus(getStatusName(account.getStatusId()))
                    .accountCreatedAt(account.getCreatedAt())
                    .closedAt(account.getClosedAt())
                    .cards(toCardSummaryList(account.getId()));
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAllAccountsByStatus(Integer statusId) {
        List<Account> accounts = statusId == null
                ? accountRepository.findAll()
                : accountRepository.findByStatusId(statusId);
        return accounts.stream().map(this::toDto).toList();
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
    }

    private void assertOwnership(Account account, User user) {
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ApiBankException("FORBIDDEN", "FORBIDDEN");
        }
    }

    private AccountResponseDto toDto(Account account) {
        return AccountResponseDto.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .statusId(account.getStatusId())
                .profileId(account.getUser().getId())
                .profileFirstName(account.getUser().getFirstName())
                .profileLastName(account.getUser().getLastName())
                .profilePictureUrl(account.getUser().getProfilePictureUrl())
                .userStatusId(account.getUser().getStatus().getId())
                .initialAmount(account.getInitialAmount())
                .isLimitsConfigured(account.getIsLimitsConfigured())
                .createdAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .closureRequestedAt(account.getClosureRequestedAt())
                .build();
    }

    private List<EmployeeUserDetailDto.CardSummaryDto> toCardSummaryList(Long accountId) {
        return cardRepository.findByAccountId(accountId).stream()
                .map(card -> EmployeeUserDetailDto.CardSummaryDto.builder()
                        .id(card.getId())
                        .maskedCardNumber(cardService.maskCardNumber(card))
                        .fullCardNumber(null)
                        .cvv(null)
                        .holderName(card.getHolderName())
                        .expirationDate(card.getExpirationDate())
                        .cardType(card.getCardType().getTypeName())
                        .cardStatus(card.getStatus().getStatusName())
                        .build())
                .toList();
    }

    private String getStatusName(Integer statusId) {
        if (statusId == null) return "UNKNOWN";
        return switch (statusId) {
            case AccountStatus.INACTIVE -> "INACTIVE";
            case AccountStatus.ACTIVE -> "ACTIVE";
            case AccountStatus.FROZEN -> "FROZEN";
            case AccountStatus.CLOSED -> "CLOSED";
            default -> "UNKNOWN";
        };
    }
}
