package com.fintech.account.application.service;

import com.fintech.account.application.dto.CreateBankAccountCommand;
import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.port.in.CreateBankAccountUseCase;
import com.fintech.account.domain.port.in.GetAccountByNumberUseCase;
import com.fintech.account.domain.port.in.GetBalanceUseCase;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.account.domain.service.AccountNumberGenerator;
import com.fintech.account.infrastructure.config.CacheConfig;
import com.fintech.common.domain.Money;
import com.fintech.common.dto.AccountInfoResponse;
import com.fintech.common.dto.BalanceResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cache behavior tests for {@link BankAccountService}.
 *
 * Verifies Requirement 14 (Cache reads and eviction):
 * - @Cacheable: second call with same key hits cache, repository NOT called again
 * - @CacheEvict: after create, cache is evicted so next read goes to repository
 *
 * Uses @SpringBootTest to load the full Spring proxy so @Cacheable/@CacheEvict
 * annotations are honoured. Infrastructure beans (DB, Kafka, Eureka) are disabled
 * via TestPropertySource and @MockitoBean.
 *
 * NOTE: BankAccountService implements multiple interfaces, so Spring creates a
 * JDK dynamic proxy. We inject via the use case interfaces, not the concrete class.
 */
@SpringBootTest(
        classes = {
                BankAccountServiceCacheTest.TestConfig.class,
                BankAccountService.class,
                CacheConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
        // Disable Eureka auto-registration
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        // Disable datasource / JPA / Kafka auto-configuration
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        // Provide required property values so @Value injections don't fail
        "jwt.secret=test-jwt-secret-key-for-cache-tests-only-256-bits-long",
        "internal.api.secret=test-internal-secret"
})
@DisplayName("BankAccountService — Cache Behavior Tests")
class BankAccountServiceCacheTest {

    // -----------------------------------------------------------------------
    // Minimal Spring context: only CacheConfig + BankAccountService + mocks
    // -----------------------------------------------------------------------

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {
        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    // Infrastructure mocks — injected into BankAccountService via @MockitoBean
    @MockitoBean
    BankAccountRepository bankAccountRepository;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    EventPublisher eventPublisher;

    @MockitoBean
    AccountNumberGenerator accountNumberGenerator;

    // Inject via interfaces — BankAccountService implements multiple interfaces,
    // so Spring creates a JDK proxy; we must inject by interface type.
    @Autowired
    GetBalanceUseCase getBalanceUseCase;

    @Autowired
    CreateBankAccountUseCase createBankAccountUseCase;

    @Autowired
    GetAccountByNumberUseCase getAccountByNumberUseCase;

    @Autowired
    CacheManager cacheManager;

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String ACCOUNT_NUMBER_STR = "1234567890";
    private static final AccountNumber ACCOUNT_NUMBER = AccountNumber.of(ACCOUNT_NUMBER_STR);

    private BankAccount sampleAccount;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test to ensure isolation
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });

        // Build a sample BankAccount (balance 100,000 VND)
        sampleAccount = BankAccount.create(ACCOUNT_NUMBER, USER_ID);
        sampleAccount.credit(Money.of(100_000L), "setup");

        // Build a sample User
        sampleUser = User.create(
                Email.of("test@example.com"),
                "hashed-password",
                "Test User",
                PhoneNumber.of("0901234567")
        );
    }

    // -----------------------------------------------------------------------
    // Task 4.6.3 — getBalance: second call hits cache
    // -----------------------------------------------------------------------

    /**
     * Verifies Requirement 14 AC-2:
     * WHEN execute(userId) is called twice with the same userId,
     * THEN findByUserId() is called only once — second call is served from cache.
     */
    @Test
    @DisplayName("getBalance: second call with same userId should hit cache (findByUserId called only once)")
    void getBalance_secondCall_shouldHitCache() {
        // Arrange
        when(bankAccountRepository.findByUserId(USER_ID))
                .thenReturn(List.of(sampleAccount));

        // Act — call twice with the same userId
        BalanceResponse first = getBalanceUseCase.execute(USER_ID);
        BalanceResponse second = getBalanceUseCase.execute(USER_ID);

        // Assert — repository called only once; second result came from cache
        verify(bankAccountRepository, times(1)).findByUserId(USER_ID);
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.accounts()).hasSize(1);
        assertThat(second.accounts()).hasSize(1);
    }

    // -----------------------------------------------------------------------
    // Task 4.6.4 — getBalance: after createAccount, cache is evicted
    // -----------------------------------------------------------------------

    /**
     * Verifies Requirement 14 AC (CacheEvict on create):
     * WHEN execute(CreateBankAccountCommand) is called after a cached getBalance,
     * THEN the cache entry for that userId is evicted,
     * AND the next getBalance call goes to the repository again (cache miss).
     */
    @Test
    @DisplayName("getBalance: after createAccount, cache is evicted and repository is called again")
    void getBalance_afterCreateAccount_shouldEvictCache() {
        // Arrange
        when(bankAccountRepository.findByUserId(USER_ID))
                .thenReturn(List.of(sampleAccount));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(sampleUser));
        when(bankAccountRepository.countByUserId(USER_ID))
                .thenReturn(0);
        when(accountNumberGenerator.generate())
                .thenReturn(AccountNumber.of("9876543210"));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenReturn(sampleAccount);

        // Step 1: populate cache with first getBalance call
        getBalanceUseCase.execute(USER_ID);
        verify(bankAccountRepository, times(1)).findByUserId(USER_ID);

        // Step 2: create a new account — this should evict the cache
        createBankAccountUseCase.execute(new CreateBankAccountCommand(USER_ID));

        // Step 3: call getBalance again — cache was evicted, so repository must be called again
        getBalanceUseCase.execute(USER_ID);

        // Assert — findByUserId called twice total (before and after eviction)
        verify(bankAccountRepository, times(2)).findByUserId(USER_ID);
    }

    // -----------------------------------------------------------------------
    // Task 4.6.5 — getByAccountNumber: second call hits cache
    // -----------------------------------------------------------------------

    /**
     * Verifies Requirement 14 AC-4:
     * WHEN execute(AccountNumber) is called twice with the same account number,
     * THEN findByAccountNumber() is called only once — second call is served from cache.
     */
    @Test
    @DisplayName("getByAccountNumber: second call with same accountNumber should hit cache")
    void getByAccountNumber_secondCall_shouldHitCache() {
        // Arrange
        when(bankAccountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(sampleAccount));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(sampleUser));

        // Act — call twice with the same account number
        AccountInfoResponse first = getAccountByNumberUseCase.execute(ACCOUNT_NUMBER);
        AccountInfoResponse second = getAccountByNumberUseCase.execute(ACCOUNT_NUMBER);

        // Assert — repository called only once; second result came from cache
        verify(bankAccountRepository, times(1)).findByAccountNumber(ACCOUNT_NUMBER);
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.accountNumber()).isEqualTo(ACCOUNT_NUMBER_STR);
        assertThat(second.accountNumber()).isEqualTo(ACCOUNT_NUMBER_STR);
    }
}
