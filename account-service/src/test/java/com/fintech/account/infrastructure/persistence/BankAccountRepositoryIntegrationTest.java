package com.fintech.account.infrastructure.persistence;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.AccountStatus;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.common.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@Testcontainers
@ActiveProfiles("test")
class BankAccountRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("account_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Mock Kafka infrastructure so the full Spring context can start without a real Kafka broker.
    // KafkaConfig requires KafkaTemplate for DeadLetterPublishingRecoverer and ConsumerFactory
    // for kafkaListenerContainerFactory; EventPublisher is implemented by KafkaEventPublisher
    // which also needs KafkaTemplate.
    @MockBean
    @SuppressWarnings("rawtypes")
    org.springframework.kafka.core.KafkaTemplate kafkaTemplate;

    @MockBean
    @SuppressWarnings("rawtypes")
    org.springframework.kafka.core.ConsumerFactory consumerFactory;

    @MockBean
    EventPublisher eventPublisher;

    @Autowired
    BankAccountRepository bankAccountRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // Counter to generate unique account numbers and user data per test
    private static final AtomicInteger counter = new AtomicInteger(0);

    private UUID userId;

    @BeforeEach
    void setUp() {
        // Create a unique user for each test (FK constraint on bank_accounts.user_id)
        int n = counter.incrementAndGet();
        User user = User.create(
                Email.of("testuser" + n + "@example.com"),
                "hashed-password",
                "Test User " + n,
                PhoneNumber.of("0" + String.format("%09d", n))
        );
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    /**
     * Creates a new BankAccount with 100,000 VND balance.
     * Uses a unique 10-digit account number derived from the counter.
     */
    private BankAccount createAccountWith100k() {
        int n = counter.incrementAndGet();
        // Pad to exactly 10 digits
        String accountNum = String.format("%010d", n);
        BankAccount account = BankAccount.create(AccountNumber.of(accountNum), userId);
        account.credit(Money.of(100_000L), "initial-credit-" + n);
        return account;
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    @Test
    void save_thenFindById_returnsCorrectBalance() {
        // Arrange
        BankAccount account = createAccountWith100k();

        // Act
        BankAccount saved = bankAccountRepository.save(account);
        Optional<BankAccount> reloaded = bankAccountRepository.findById(saved.getId());

        // Assert
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getBalance()).isEqualTo(Money.of(100_000L));
    }

    @Test
    void save_thenFindById_returnsCorrectVersion() {
        // Arrange — fresh account (version = 0 before first save)
        int n = counter.incrementAndGet();
        String accountNum = String.format("%010d", n);
        BankAccount account = BankAccount.create(AccountNumber.of(accountNum), userId);

        // Act — first save
        BankAccount saved = bankAccountRepository.save(account);
        Optional<BankAccount> reloaded = bankAccountRepository.findById(saved.getId());

        // Assert — JPA @Version starts at 0 after first save
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getVersion()).isEqualTo(0L);

        // Act — second save (credit triggers update → version increments to 1)
        BankAccount toUpdate = reloaded.get();
        toUpdate.credit(Money.of(50_000L), "credit-for-version-test");
        bankAccountRepository.save(toUpdate);

        Optional<BankAccount> reloadedAfterUpdate = bankAccountRepository.findById(saved.getId());
        assertThat(reloadedAfterUpdate).isPresent();
        assertThat(reloadedAfterUpdate.get().getVersion()).isEqualTo(1L);
    }

    @Test
    void save_thenFindById_returnsCorrectStatus() {
        // Arrange
        int n = counter.incrementAndGet();
        String accountNum = String.format("%010d", n);
        BankAccount account = BankAccount.create(AccountNumber.of(accountNum), userId);
        // create() sets status = ACTIVE

        // Act
        BankAccount saved = bankAccountRepository.save(account);
        Optional<BankAccount> reloaded = bankAccountRepository.findById(saved.getId());

        // Assert
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void debitAndSave_thenReload_balanceIsUpdated() {
        // Arrange — account with 100k
        BankAccount account = createAccountWith100k();
        BankAccount saved = bankAccountRepository.save(account);

        // Act — reload, debit 50k, save again
        BankAccount loaded = bankAccountRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("Account not found after save"));
        loaded.debit(Money.of(50_000L), "transfer-debit-test");
        bankAccountRepository.save(loaded);

        // Assert — reload again and verify balance = 50k
        Optional<BankAccount> reloaded = bankAccountRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getBalance()).isEqualTo(Money.of(50_000L));
    }

    @Test
    void flyway_migrations_createExpectedTables() {
        // Query information_schema to verify all 3 expected tables exist
        String sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('users', 'bank_accounts', 'processed_transfers')
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        assertThat(count).isEqualTo(3);
    }
}
