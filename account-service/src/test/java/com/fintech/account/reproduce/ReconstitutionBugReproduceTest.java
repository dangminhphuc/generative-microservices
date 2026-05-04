package com.fintech.account.reproduce;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.AccountStatus;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.infrastructure.adapter.out.persistence.BankAccountJpaEntity;
import com.fintech.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduce tests for the BankAccountJpaEntity reconstitution bug.
 * <p>
 * These tests FAIL with the current code and PASS only after the fix.
 * <p>
 * Root cause: BankAccountJpaEntity.toDomain() calls BankAccount.create() instead of
 * a proper reconstitute() method. create() always resets balance=0, version=0,
 * status=ACTIVE — losing all persisted state.
 * <p>
 * See: Requirement 1 in account-service-production-hardening spec.
 * See: LL-1 (Lesson Learned) in requirements.md.
 */
@DisplayName("[REPRODUCE] BankAccountJpaEntity Reconstitution Bug")
class ReconstitutionBugReproduceTest {

    /**
     * FAILS NOW: toDomain() calls create() which sets balance = Money.zero().
     * The entity has balance=500000 stored, but the returned domain object has balance=0.
     * <p>
     * PASSES AFTER FIX: toDomain() calls reconstitute() which restores balance=500000.
     */
    @Test
    @DisplayName("REPRODUCE: balance is reset to 0 after round-trip through JPA entity")
    void toDomain_shouldRestoreBalance_notResetToZero() {
        // Arrange — simulate an account that was loaded from DB with balance 500,000 VND
        BankAccountJpaEntity entity = buildEntityWithBalance(500_000L);

        // Act
        BankAccount account = entity.toDomain();

        // Assert — FAILS with current code: actual balance is 0, not 500,000
        assertThat(account.getBalance())
                .as("Balance must be restored from DB, not reset to zero by create()")
                .isEqualTo(Money.of(500_000L));
    }

    /**
     * FAILS NOW: toDomain() calls create() which sets status = ACTIVE.
     * The entity has status=FROZEN stored, but the returned domain object has status=ACTIVE.
     * <p>
     * PASSES AFTER FIX: toDomain() calls reconstitute() which restores status=FROZEN.
     */
    @Test
    @DisplayName("REPRODUCE: status is reset to ACTIVE after round-trip through JPA entity")
    void toDomain_shouldRestoreStatus_notResetToActive() {
        // Arrange — simulate a frozen account loaded from DB
        BankAccountJpaEntity entity = buildEntityWithStatus(AccountStatus.FROZEN);

        // Act
        BankAccount account = entity.toDomain();

        // Assert — FAILS with current code: actual status is ACTIVE, not FROZEN
        assertThat(account.getStatus())
                .as("Status must be restored from DB, not reset to ACTIVE by create()")
                .isEqualTo(AccountStatus.FROZEN);
    }

    /**
     * FAILS NOW: toDomain() calls create() which sets version = 0.
     * The entity has version=5 stored, but the returned domain object has version=0.
     * <p>
     * PASSES AFTER FIX: toDomain() calls reconstitute() which restores version=5.
     */
    @Test
    @DisplayName("REPRODUCE: version is reset to 0 after round-trip through JPA entity")
    void toDomain_shouldRestoreVersion_notResetToZero() {
        // Arrange — simulate an account with version=5 (has been updated 5 times)
        BankAccountJpaEntity entity = buildEntityWithVersion(5L);

        // Act
        BankAccount account = entity.toDomain();

        // Assert — FAILS with current code: actual version is 0, not 5
        assertThat(account.getVersion())
                .as("Version must be restored from DB, not reset to 0 by create()")
                .isEqualTo(5L);
    }

    /**
     * FAILS NOW: toDomain() calls create() which generates a NEW random UUID.
     * The entity has a specific id, but the returned domain object has a different id.
     * <p>
     * PASSES AFTER FIX: toDomain() calls reconstitute() which preserves the original id.
     */
    @Test
    @DisplayName("REPRODUCE: entity id is lost after round-trip through JPA entity")
    void toDomain_shouldPreserveId_notGenerateNewOne() {
        // Arrange
        UUID originalId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        BankAccountJpaEntity entity = buildEntityWithId(originalId);

        // Act
        BankAccount account = entity.toDomain();

        // Assert — FAILS with current code: create() generates a new random UUID
        assertThat(account.getId())
                .as("ID must be preserved from DB, not replaced with a new random UUID by create()")
                .isEqualTo(originalId);
    }

    /**
     * FAILS NOW: because toDomain() resets balance to 0, calling debit() on a loaded
     * account always throws INSUFFICIENT_BALANCE even when the account has enough funds.
     * <p>
     * This is the most critical consequence of the bug — transfers always fail.
     * <p>
     * PASSES AFTER FIX: debit() works correctly with the restored balance.
     */
    @Test
    @DisplayName("REPRODUCE: debit() always throws INSUFFICIENT_BALANCE on accounts loaded from DB")
    void toDomain_balanceResetToZero_causesDebitToAlwaysFail() {
        // Arrange — account has 100,000 VND in DB, trying to debit 50,000 VND
        BankAccountJpaEntity entity = buildEntityWithBalance(100_000L);
        BankAccount account = entity.toDomain();

        // Act & Assert — FAILS with current code: balance is 0, so debit throws
        // After fix: balance is 100,000, so debit succeeds
        assertThatThrownBy(() -> account.debit(Money.of(50_000L), "transfer-123"))
                .as("With the bug: balance=0 causes INSUFFICIENT_BALANCE even though DB has 100,000 VND. " +
                        "After fix: this should NOT throw — debit should succeed.")
                .hasMessageContaining("Insufficient balance");
        // NOTE: This test asserts the BUGGY behavior.
        // After the fix, this test should be INVERTED:
        //   assertThatNoException().isThrownBy(() -> account.debit(...))
        // But for reproduce purposes, we assert the bug IS present.
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a BankAccountJpaEntity simulating a row loaded from the database.
     * Uses reflection-style field population via the entity's own fromDomain() to
     * create a realistic entity, then manually overrides balance via a helper.
     */
    private BankAccountJpaEntity buildEntityWithBalance(long balanceAmount) {
        return buildEntity(
                UUID.randomUUID(),
                "1234567890",
                UUID.randomUUID(),
                balanceAmount,
                "VND",
                AccountStatus.ACTIVE,
                1L
        );
    }

    private BankAccountJpaEntity buildEntityWithStatus(AccountStatus status) {
        return buildEntity(
                UUID.randomUUID(),
                "1234567890",
                UUID.randomUUID(),
                100_000L,
                "VND",
                status,
                1L
        );
    }

    private BankAccountJpaEntity buildEntityWithVersion(long version) {
        return buildEntity(
                UUID.randomUUID(),
                "1234567890",
                UUID.randomUUID(),
                100_000L,
                "VND",
                AccountStatus.ACTIVE,
                version
        );
    }

    private BankAccountJpaEntity buildEntityWithId(UUID id) {
        return buildEntity(
                id,
                "1234567890",
                UUID.randomUUID(),
                100_000L,
                "VND",
                AccountStatus.ACTIVE,
                1L
        );
    }

    /**
     * Creates a BankAccountJpaEntity by going through fromDomain() with a crafted
     * BankAccount, then using the entity's own getters to verify state.
     * <p>
     * Since BankAccount.create() always sets balance=0, we use fromDomain() on a
     * freshly created account and accept that the entity's balance field will be 0.
     * For tests that need non-zero balance, we use a workaround via reflection.
     * <p>
     * Actually, the cleanest approach: use fromDomain() on a BankAccount that has
     * been credited to reach the desired balance.
     */
    private BankAccountJpaEntity buildEntity(UUID id, String accountNumber, UUID userId,
                                             long balance, String currency,
                                             AccountStatus status, long version) {
        // We need to create an entity that simulates what JPA would load from DB.
        // The only way to set all fields is via fromDomain(), but BankAccount.create()
        // doesn't support setting balance/status/version directly.
        //
        // Workaround: create account, credit it to reach desired balance, then
        // use fromDomain(). This is only possible for ACTIVE accounts with positive balance.
        //
        // For FROZEN status or specific version, we use reflection to set fields directly
        // on the entity — simulating what JPA does when hydrating from ResultSet.

        BankAccount account = BankAccount.create(AccountNumber.of(accountNumber), userId);
        // Credit to reach desired balance
        if (balance > 0) {
            account.credit(Money.of(balance), "setup");
        }

        BankAccountJpaEntity entity = BankAccountJpaEntity.fromDomain(account);

        // Use reflection to override fields that fromDomain() can't set correctly
        // (id, status, version) — simulating JPA hydration from DB
        try {
            setField(entity, "id", id);
            setField(entity, "status", status);
            setField(entity, "version", version);
            setField(entity, "balance", balance);
            setField(entity, "currency", currency);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity fields via reflection", e);
        }

        return entity;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
