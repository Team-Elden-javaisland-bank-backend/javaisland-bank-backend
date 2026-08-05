package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountLimitRepository;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.LimitChangeRequestRepository;
import com.javaisland.bank_backend.account.service.AccountManagementService;
import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import com.javaisland.bank_backend.beneficiary.repository.BeneficiaryRepository;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.user.dto.CustomerListItemDto;
import com.javaisland.bank_backend.user.dto.PendingRegistrationDto;
import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserPinRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import com.javaisland.bank_backend.user.model.UserPin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserStatusRepository userStatusRepository;
    @Mock private RoleTypeRepository roleTypeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountManagementService accountManagementService;
    @Mock private CardService cardService;
    @Mock private KeycloakAdminService keycloakAdminService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private CardRepository cardRepository;
    @Mock private AccountLimitRepository accountLimitRepository;
    @Mock private LimitChangeRequestRepository limitChangeRequestRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PasswordChangeRequestRepository passwordChangeRequestRepository;
    @Mock private UserPinRepository userPinRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private UserService userService;

    private UserStatus createStatus(int id, String name) {
        UserStatus status = new UserStatus();
        status.setId(id);
        status.setUserStatus(name);
        return status;
    }

    private User createUser(Long id, String firstName, String lastName, String email, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setUsername(email);
        user.setKeycloakId("kc-" + id);
        user.setStatus(status);
        RoleType role = new RoleType();
        role.setRoleName("C");
        user.setRoleType(role);
        return user;
    }

    @Test
    void getPendingRegistrations_returnsPendingUsers() {
        UserStatus pendingStatus = createStatus(1, "PENDING");
        when(userStatusRepository.findByUserStatus("PENDING")).thenReturn(Optional.of(pendingStatus));

        User pendingUser = createUser(1L, "John", "Doe", "john@test.com", pendingStatus);
        when(userRepository.findByStatus(pendingStatus)).thenReturn(List.of(pendingUser));

        List<PendingRegistrationDto> result = userService.getPendingRegistrations();

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
    }

    @Test
    void validateRegistration_activatesUser() {
        UserStatus pendingStatus = createStatus(1, "PENDING");
        User user = createUser(1L, "Jane", "Doe", "jane@test.com", pendingStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserStatus activeStatus = createStatus(2, "ACTIVE");
        when(userStatusRepository.findByUserStatus("ACTIVE")).thenReturn(Optional.of(activeStatus));

        Account account = new Account();
        account.setId(100L);
        account.setAccountNumber("IT123");
        account.setStatusId(AccountStatus.ACTIVE);
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));
        doNothing().when(accountManagementService).activateInitialAccountForUser(1L);

        userService.validateRegistration(1L);

        assertEquals("ACTIVE", user.getStatus().getUserStatus());
        verify(userRepository).save(user);
    }

    @Test
    void rejectRegistration_rejectsUser() {
        UserStatus pendingStatus = createStatus(1, "PENDING");
        User user = createUser(1L, "Jack", "Smith", "jack@test.com", pendingStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userStatusRepository.findByUserStatus("PENDING")).thenReturn(Optional.of(pendingStatus));

        UserStatus annulledStatus = createStatus(3, "ANNULLED");
        when(userStatusRepository.findByUserStatus("ANNULLED")).thenReturn(Optional.of(annulledStatus));

        userService.rejectRegistration(1L);

        assertEquals("ANNULLED", user.getStatus().getUserStatus());
        verify(keycloakAdminService).setUserEnabled("kc-1", false);
        verify(userRepository).save(user);
    }

    @Test
    void validateRegistration_alreadyActive_throwsException() {
        UserStatus activeStatus = createStatus(2, "ACTIVE");
        User user = createUser(1L, "Active", "User", "active@test.com", activeStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(ApiBankException.class, () -> userService.validateRegistration(1L));
    }

    @Test
    void rejectRegistration_notPending_throwsException() {
        UserStatus activeStatus = createStatus(2, "ACTIVE");
        User user = createUser(1L, "Not", "Pending", "not@test.com", activeStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserStatus pendingStatus = createStatus(1, "PENDING");
        when(userStatusRepository.findByUserStatus("PENDING")).thenReturn(Optional.of(pendingStatus));

        assertThrows(ApiBankException.class, () -> userService.rejectRegistration(1L));
    }

    @Test
    void reopenRegistration_reopensUser() {
        UserStatus annulledStatus = createStatus(3, "ANNULLED");
        User user = createUser(1L, "Reopen", "Me", "reopen@test.com", annulledStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userStatusRepository.findByUserStatus("ANNULLED")).thenReturn(Optional.of(annulledStatus));

        UserStatus pendingStatus = createStatus(1, "PENDING");
        when(userStatusRepository.findByUserStatus("PENDING")).thenReturn(Optional.of(pendingStatus));

        userService.reopenRegistration(1L);

        assertEquals("PENDING", user.getStatus().getUserStatus());
        verify(userRepository).save(user);
    }

    @Test
    void getAnnulledRegistrations_returnsAnnulledUsers() {
        UserStatus annulledStatus = createStatus(3, "ANNULLED");
        when(userStatusRepository.findByUserStatus("ANNULLED")).thenReturn(Optional.of(annulledStatus));

        User annulledUser = createUser(1L, "Annulled", "User", "annulled@test.com", annulledStatus);
        when(userRepository.findByStatus(annulledStatus)).thenReturn(List.of(annulledUser));

        List<PendingRegistrationDto> result = userService.getAnnulledRegistrations();

        assertEquals(1, result.size());
        assertEquals("Annulled", result.get(0).getFirstName());
    }

    @Test
    void getAllCustomersSortedByName_returnsCustomers() {
        RoleType customerRole = new RoleType();
        customerRole.setRoleName("C");
        when(roleTypeRepository.findByRoleName("C")).thenReturn(Optional.of(customerRole));

        UserStatus activeStatus = createStatus(2, "ACTIVE");
        when(userStatusRepository.findByUserStatus("ACTIVE")).thenReturn(Optional.of(activeStatus));
        User customer = createUser(1L, "Alice", "Brown", "alice@test.com", activeStatus);
        when(userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(customerRole))
                .thenReturn(List.of(customer));

        var result = userService.getAllCustomersSortedByName(PageRequest.of(0, 20), null);

        assertEquals(1, result.content().size());
        assertEquals("Alice", result.content().get(0).getFirstName());
    }

    @Test
    void getAllCustomersSortedByName_defaultFiltersOnlyActive() {
        RoleType customerRole = new RoleType();
        customerRole.setRoleName("C");
        when(roleTypeRepository.findByRoleName("C")).thenReturn(Optional.of(customerRole));

        UserStatus activeStatus = createStatus(2, "ACTIVE");
        UserStatus pendingStatus = createStatus(1, "PENDING");
        when(userStatusRepository.findByUserStatus("ACTIVE")).thenReturn(Optional.of(activeStatus));

        User activeCustomer = createUser(1L, "Alice", "Brown", "alice@test.com", activeStatus);
        User pendingCustomer = createUser(2L, "Bob", "Smith", "bob@test.com", pendingStatus);
        when(userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(customerRole))
                .thenReturn(List.of(activeCustomer, pendingCustomer));

        var result = userService.getAllCustomersSortedByName(PageRequest.of(0, 20), null);

        assertEquals(1, result.content().size());
        assertEquals("Alice", result.content().get(0).getFirstName());
    }

    @Test
    void validateRegistration_sendsNotification() {
        UserStatus pendingStatus = createStatus(1, "PENDING");
        User user = createUser(1L, "Notif", "Test", "notif@test.com", pendingStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserStatus activeStatus = createStatus(2, "ACTIVE");
        when(userStatusRepository.findByUserStatus("ACTIVE")).thenReturn(Optional.of(activeStatus));

        Account account = new Account();
        account.setId(100L);
        account.setAccountNumber("IT123");
        account.setStatusId(AccountStatus.ACTIVE);
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));
        doNothing().when(accountManagementService).activateInitialAccountForUser(1L);

        userService.validateRegistration(1L);

        verify(notificationService).send(eq(1L), eq("ACCOUNT"), anyString(), eq("NOTIF_REGISTRATION_APPROVED"), isNull());
        verify(auditLogService).log(eq("REGISTRATION"), eq(1L), eq("VALIDATE"), eq("system"), anyString());
    }

    @Test
    void rejectRegistration_sendsNotification() {
        UserStatus pendingStatus = createStatus(1, "PENDING");
        User user = createUser(1L, "Reject", "Notif", "reject@test.com", pendingStatus);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userStatusRepository.findByUserStatus("PENDING")).thenReturn(Optional.of(pendingStatus));

        UserStatus annulledStatus = createStatus(3, "ANNULLED");
        when(userStatusRepository.findByUserStatus("ANNULLED")).thenReturn(Optional.of(annulledStatus));

        userService.rejectRegistration(1L);

        verify(notificationService).send(eq(1L), eq("ACCOUNT"), anyString(), eq("NOTIF_REGISTRATION_REJECTED"), isNull());
        verify(auditLogService).log(eq("REGISTRATION"), eq(1L), eq("REJECT"), eq("system"), anyString());
    }
}
