package com.fintech.account.application.service;

import com.fintech.account.application.dto.CreateBankAccountCommand;
import com.fintech.account.application.dto.CreateBankAccountResponse;
import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.account.domain.service.AccountNumberGenerator;
import com.fintech.common.dto.AccountInfoResponse;
import com.fintech.common.dto.BalanceResponse;
import com.fintech.common.event.account.AccountCreatedEvent;
import com.fintech.common.exception.BusinessRuleException;
import com.fintech.common.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    private BankAccountService bankAccountService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ACCOUNT_NUMBER_VALUE = "1234567890";

    @BeforeEach
    void setUp() {
        bankAccountService = new BankAccountService(
                bankAccountRepository,
                userRepository,
                eventPublisher,
                accountNumberGenerator,
                new SimpleMeterRegistry()
        );
    }

    // ─── createAccount ────────────────────────────────────────────────────────

    @Test
    void createAccount_withValidUser_returnsResponse() {
        // Arrange
        User user = User.create(
                Email.of("user@example.com"),
                "hashed_password",
                "Nguyen Van A",
                PhoneNumber.of("0901234567")
        );
        AccountNumber accountNumber = AccountNumber.of(ACCOUNT_NUMBER_VALUE);
        BankAccount account = BankAccount.create(accountNumber, USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bankAccountRepository.countByUserId(USER_ID)).thenReturn(0);
        when(accountNumberGenerator.generate()).thenReturn(accountNumber);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(account);

        // Act
        CreateBankAccountResponse response = bankAccountService.execute(new CreateBankAccountCommand(USER_ID));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isNotNull();
        // accountId comes from saved.getId() — BankAccount.create() uses super() which generates a UUID
        assertThat(response.accountNumber()).isEqualTo(ACCOUNT_NUMBER_VALUE);
    }

    @Test
    void createAccount_whenMaxAccountsReached_throwsMaxAccountsReached() {
        // Arrange
        User user = User.create(
                Email.of("user@example.com"),
                "hashed_password",
                "Nguyen Van A",
                PhoneNumber.of("0901234567")
        );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bankAccountRepository.countByUserId(USER_ID)).thenReturn(5);

        // Act & Assert
        assertThatThrownBy(() -> bankAccountService.execute(new CreateBankAccountCommand(USER_ID)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", "MAX_ACCOUNTS_REACHED");
    }

    @Test
    void createAccount_publishesAccountCreatedEvent() {
        // Arrange
        User user = User.create(
                Email.of("user@example.com"),
                "hashed_password",
                "Nguyen Van A",
                PhoneNumber.of("0901234567")
        );
        AccountNumber accountNumber = AccountNumber.of(ACCOUNT_NUMBER_VALUE);
        BankAccount account = BankAccount.create(accountNumber, USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bankAccountRepository.countByUserId(USER_ID)).thenReturn(0);
        when(accountNumberGenerator.generate()).thenReturn(accountNumber);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(account);

        // Act
        bankAccountService.execute(new CreateBankAccountCommand(USER_ID));

        // Assert
        ArgumentCaptor<AccountCreatedEvent> captor = ArgumentCaptor.forClass(AccountCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        AccountCreatedEvent event = captor.getValue();
        assertThat(event).isInstanceOf(AccountCreatedEvent.class);
        assertThat(event.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER_VALUE);
        assertThat(event.getUserId()).isEqualTo(USER_ID.toString());
    }

    // ─── getBalance ───────────────────────────────────────────────────────────

    @Test
    void getBalance_returnsAllAccountsForUser() {
        // Arrange
        AccountNumber accountNumber1 = AccountNumber.of("1234567890");
        AccountNumber accountNumber2 = AccountNumber.of("0987654321");
        BankAccount account1 = BankAccount.create(accountNumber1, USER_ID);
        BankAccount account2 = BankAccount.create(accountNumber2, USER_ID);

        when(bankAccountRepository.findByUserId(USER_ID)).thenReturn(List.of(account1, account2));

        // Act
        BalanceResponse response = bankAccountService.execute(USER_ID);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accounts()).hasSize(2);
    }

    // ─── getAccountByNumber ───────────────────────────────────────────────────

    @Test
    void getAccountByNumber_withExistingAccount_returnsInfo() {
        // Arrange
        AccountNumber accountNumber = AccountNumber.of(ACCOUNT_NUMBER_VALUE);
        BankAccount account = BankAccount.create(accountNumber, USER_ID);
        User user = User.create(
                Email.of("user@example.com"),
                "hashed_password",
                "Nguyen Van A",
                PhoneNumber.of("0901234567")
        );

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // Act
        AccountInfoResponse response = bankAccountService.execute(accountNumber);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isEqualTo(ACCOUNT_NUMBER_VALUE);
        assertThat(response.ownerName()).isEqualTo("Nguyen Van A");
    }

    @Test
    void getAccountByNumber_withNonExistingAccount_throwsResourceNotFoundException() {
        // Arrange
        AccountNumber accountNumber = AccountNumber.of(ACCOUNT_NUMBER_VALUE);

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bankAccountService.execute(accountNumber))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
