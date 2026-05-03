package com.fintech.account.application.service;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.port.in.DebitCreditUseCase;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferRepository;
import com.fintech.account.infrastructure.config.CacheConfig;
import com.fintech.common.domain.Money;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cache eviction tests for {@link DebitCreditService}.
 *
 * Verifies Requirement 14 (CacheEvict on debit/credit):
 * - After processTransfer, the source account cache entry is evicted
 * - After processTransfer, the destination account cache entry is evicted
 *
 * Uses @SpringBootTest to load the Spring proxy so @Caching/@CacheEvict
 * annotations on DebitCreditService.execute() are honoured.
 *
 * NOTE: DebitCreditService implements DebitCreditUseCase, so Spring creates a
 * JDK dynamic proxy. We inject via the DebitCreditUseCase interface.
 */
@SpringBootTest(
        classes = {
                DebitCreditServiceCacheTest.TestConfig.class,
                DebitCreditService.class,
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
        // Provide required property values
        "jwt.secret=test-jwt-secret-key-for-cache-tests-only-256-bits-long",
        "internal.api.secret=test-internal-secret"
})
@DisplayName("DebitCreditService — Cache Eviction Tests")
class DebitCreditServiceCacheTest {

    // -----------------------------------------------------------------------
    // Minimal Spring context: only CacheConfig + DebitCreditService + mocks
    // -----------------------------------------------------------------------

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {
        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @MockitoBean
    BankAccountRepository bankAccountRepository;

    @MockitoBean
    EventPublisher eventPublisher;

    @MockitoBean
    ProcessedTransferRepository processedTransferRepository;

    // Inject via interface — DebitCreditService implements DebitCreditUseCase,
    // so Spring creates a JDK proxy; we must inject by interface type.
    @Autowired
    DebitCreditUseCase debitCreditUseCase;

    @Autowired
    CacheManager cacheManager;

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private static final String SOURCE_ACCOUNT_NUMBER = "1111111111";
    private static final String DEST_ACCOUNT_NUMBER   = "2222222222";
    private static final AccountNumber SOURCE_AN = AccountNumber.of(SOURCE_ACCOUNT_NUMBER);
    private static final AccountNumber DEST_AN   = AccountNumber.of(DEST_ACCOUNT_NUMBER);

    private BankAccount sourceAccount;
    private BankAccount destAccount;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });

        // Source account: 200,000 VND
        sourceAccount = BankAccount.create(SOURCE_AN, UUID.randomUUID());
        sourceAccount.credit(Money.of(200_000L), "setup");

        // Destination account: 0 VND
        destAccount = BankAccount.create(DEST_AN, UUID.randomUUID());

        // Default mock: transfer not yet processed (idempotency check passes)
        when(processedTransferRepository.existsById(any())).thenReturn(false);
        when(processedTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.findByAccountNumber(SOURCE_AN)).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumber(DEST_AN)).thenReturn(Optional.of(destAccount));
    }

    // -----------------------------------------------------------------------
    // Task 4.6.7 — processTransfer evicts source account cache
    // -----------------------------------------------------------------------

    /**
     * Verifies Requirement 14 (CacheEvict — source account):
     * GIVEN the source account is cached in "accounts-by-number",
     * WHEN execute(TransferInitiatedEvent) is called,
     * THEN the cache entry for the source account number is evicted.
     */
    @Test
    @DisplayName("processTransfer: source account cache entry is evicted after transfer")
    void processTransfer_shouldEvictSourceAccountCache() {
        // Arrange — populate the "accounts-by-number" cache for the source account
        Cache accountsByNumber = cacheManager.getCache("accounts-by-number");
        assertThat(accountsByNumber).isNotNull();
        accountsByNumber.put(SOURCE_ACCOUNT_NUMBER, sourceAccount);

        // Verify cache is populated
        assertThat(accountsByNumber.get(SOURCE_ACCOUNT_NUMBER)).isNotNull();

        // Act — process a transfer (this should evict the source account from cache)
        debitCreditUseCase.execute(buildTransferEvent(
                UUID.randomUUID().toString(),
                SOURCE_ACCOUNT_NUMBER,
                DEST_ACCOUNT_NUMBER,
                BigDecimal.valueOf(50_000)
        ));

        // Assert — source account cache entry is evicted
        assertThat(accountsByNumber.get(SOURCE_ACCOUNT_NUMBER))
                .as("Source account cache entry must be evicted after processTransfer")
                .isNull();
    }

    // -----------------------------------------------------------------------
    // Task 4.6.8 — processTransfer evicts destination account cache
    // -----------------------------------------------------------------------

    /**
     * Verifies Requirement 14 (CacheEvict — destination account):
     * GIVEN the destination account is cached in "accounts-by-number",
     * WHEN execute(TransferInitiatedEvent) is called,
     * THEN the cache entry for the destination account number is evicted.
     */
    @Test
    @DisplayName("processTransfer: destination account cache entry is evicted after transfer")
    void processTransfer_shouldEvictDestinationAccountCache() {
        // Arrange — populate the "accounts-by-number" cache for the destination account
        Cache accountsByNumber = cacheManager.getCache("accounts-by-number");
        assertThat(accountsByNumber).isNotNull();
        accountsByNumber.put(DEST_ACCOUNT_NUMBER, destAccount);

        // Verify cache is populated
        assertThat(accountsByNumber.get(DEST_ACCOUNT_NUMBER)).isNotNull();

        // Act — process a transfer (this should evict the destination account from cache)
        debitCreditUseCase.execute(buildTransferEvent(
                UUID.randomUUID().toString(),
                SOURCE_ACCOUNT_NUMBER,
                DEST_ACCOUNT_NUMBER,
                BigDecimal.valueOf(50_000)
        ));

        // Assert — destination account cache entry is evicted
        assertThat(accountsByNumber.get(DEST_ACCOUNT_NUMBER))
                .as("Destination account cache entry must be evicted after processTransfer")
                .isNull();
    }

    /**
     * Additional: both source and destination are evicted in a single transfer.
     * Also verifies that "accounts-by-userId" allEntries eviction does not throw.
     */
    @Test
    @DisplayName("processTransfer: both source and destination cache entries are evicted")
    void processTransfer_shouldEvictBothSourceAndDestinationCache() {
        // Arrange — populate cache for both accounts
        Cache accountsByNumber = cacheManager.getCache("accounts-by-number");
        assertThat(accountsByNumber).isNotNull();
        accountsByNumber.put(SOURCE_ACCOUNT_NUMBER, sourceAccount);
        accountsByNumber.put(DEST_ACCOUNT_NUMBER, destAccount);

        // Populate accounts-by-userId cache with a dummy entry
        Cache accountsByUserId = cacheManager.getCache("accounts-by-userId");
        assertThat(accountsByUserId).isNotNull();
        accountsByUserId.put(sourceAccount.getUserId(), "cached-balance");

        // Act
        debitCreditUseCase.execute(buildTransferEvent(
                UUID.randomUUID().toString(),
                SOURCE_ACCOUNT_NUMBER,
                DEST_ACCOUNT_NUMBER,
                BigDecimal.valueOf(30_000)
        ));

        // Assert — both account number cache entries are evicted
        assertThat(accountsByNumber.get(SOURCE_ACCOUNT_NUMBER))
                .as("Source account cache must be evicted")
                .isNull();
        assertThat(accountsByNumber.get(DEST_ACCOUNT_NUMBER))
                .as("Destination account cache must be evicted")
                .isNull();

        // accounts-by-userId is evicted with allEntries=true
        assertThat(accountsByUserId.get(sourceAccount.getUserId()))
                .as("accounts-by-userId cache must be fully evicted (allEntries=true)")
                .isNull();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private TransferInitiatedEvent buildTransferEvent(String transferId,
                                                       String sourceAccountNumber,
                                                       String destAccountNumber,
                                                       BigDecimal amount) {
        return new TransferInitiatedEvent(
                transferId,
                sourceAccountNumber,
                destAccountNumber,
                amount,
                "VND",
                "Test transfer"
        );
    }
}
