package com.fintech.transfer.application.service;

import com.fintech.common.domain.Money;
import com.fintech.common.dto.AccountInfoResponse;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import com.fintech.common.exception.AccessDeniedException;
import com.fintech.transfer.application.dto.*;
import com.fintech.transfer.domain.model.Transfer;
import com.fintech.transfer.domain.port.in.InitiateTransferUseCase;
import com.fintech.transfer.domain.port.in.PreviewTransferUseCase;
import com.fintech.transfer.domain.port.out.AccountServicePort;
import com.fintech.transfer.domain.port.out.EventPublisher;
import com.fintech.transfer.domain.port.out.TransferRepository;
import com.fintech.transfer.domain.service.TransferValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TransferService implements InitiateTransferUseCase, PreviewTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;
    private final AccountServicePort accountServicePort;
    private final TransferValidationService validationService;

    public TransferService(TransferRepository transferRepository,
                           EventPublisher eventPublisher,
                           AccountServicePort accountServicePort,
                           TransferValidationService validationService) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
        this.accountServicePort = accountServicePort;
        this.validationService = validationService;
    }

    @Override
    @Transactional(readOnly = true)
    public TransferPreviewResponse execute(PreviewTransferCommand command) {
        Money amount = Money.of(command.amount());

        validationService.validateMinimumAmount(amount);
        validationService.validateNotSameAccount(command.sourceAccountNumber(), command.destinationAccountNumber());

        AccountInfoResponse source = accountServicePort.getAccountByNumber(command.sourceAccountNumber());
        validationService.validateAccountActive(source.status());

        AccountInfoResponse dest = accountServicePort.getAccountByNumber(command.destinationAccountNumber());

        Money todayTotal = transferRepository.sumAmountBySourceAccountAndDate(
                command.sourceAccountNumber(), LocalDate.now());
        validationService.validateDailyLimit(todayTotal, amount);

        String maskedDestName = maskName(dest.ownerName());

        return new TransferPreviewResponse(source.ownerName(), maskedDestName,
                command.amount(), command.description());
    }

    @Override
    @Transactional
    public TransferResponse execute(InitiateTransferCommand command) {
        Money amount = Money.of(command.amount());

        validationService.validateMinimumAmount(amount);
        validationService.validateNotSameAccount(command.sourceAccountNumber(), command.destinationAccountNumber());

        AccountInfoResponse source = accountServicePort.getAccountByNumber(command.sourceAccountNumber());
        validationService.validateAccountActive(source.status());

        // Verify ownership
        if (!command.userId().toString().equals(resolveUserId(source))) {
            throw new AccessDeniedException("Không có quyền truy cập tài khoản này");
        }

        accountServicePort.getAccountByNumber(command.destinationAccountNumber());

        Money todayTotal = transferRepository.sumAmountBySourceAccountAndDate(
                command.sourceAccountNumber(), LocalDate.now());
        validationService.validateDailyLimit(todayTotal, amount);

        Transfer transfer = Transfer.create(
                command.sourceAccountNumber(),
                command.destinationAccountNumber(),
                amount, command.description(), command.userId());

        Transfer saved = transferRepository.save(transfer);

        eventPublisher.publish(new TransferInitiatedEvent(
                saved.getId().toString(),
                saved.getSourceAccountNumber(),
                saved.getDestinationAccountNumber(),
                saved.getAmount().getAmount(),
                saved.getAmount().getCurrency(),
                saved.getDescription()
        ));

        log.info("Transfer {} initiated: {} -> {} amount {}",
                saved.getId(), command.sourceAccountNumber(),
                command.destinationAccountNumber(), amount);

        return new TransferResponse(
                saved.getId().toString(),
                saved.getStatus().name(),
                saved.getAmount().getAmount(),
                saved.getCreatedAt()
        );
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 2) return "***";
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

    private String resolveUserId(AccountInfoResponse account) {
        // In a real implementation, AccountInfoResponse would include userId
        // For now, ownership is verified at the Account Service level
        return account.accountId();
    }
}
