package com.javaisland.bank_backend.user;

import com.javaisland.bank_backend.account.Account;
import com.javaisland.bank_backend.account.AccountRepository;
import com.javaisland.bank_backend.account.AccountService;
import com.javaisland.bank_backend.exception.ApiBankException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getPendingRegistrations() {
        return userRepository.findByRoleTypeIdAndStatusId(RoleType.CUSTOMER, UserStatus.PENDING).stream()
                .map(user -> {
                    List<Account> accounts = accountRepository.findByUserId(user.getId());
                    String pendingAccountNumber = accounts.isEmpty() ? null : accounts.get(0).getAccountNumber();
                    return PendingRegistrationDto.builder()
                            .userId(user.getId())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .birthDate(user.getBirthDate())
                            .email(user.getEmail())
                            .pendingAccountNumber(pendingAccountNumber)
                            .registeredAt(user.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void validateRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));

        if (user.getStatusId() != UserStatus.PENDING) {
            throw new ApiBankException("User " + userId + " has no pending registration.", "INVALID_USER_STATE");
        }

        accountService.activateInitialAccountForUser(userId);

        user.setStatusId(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Registration validated by employee for user id={}", userId);
    }

    @Transactional(readOnly = true)
    public List<CustomerListItemDto> getAllCustomersSortedByName() {
        List<User> customers = userRepository.findByRoleTypeId(RoleType.CUSTOMER);

        return customers.stream()
                .sorted(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getLastName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> CustomerListItemDto.builder()
                        .userId(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .statusId(u.getStatusId())
                        .build())
                .toList();
    }
}
