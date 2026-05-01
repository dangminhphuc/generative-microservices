package com.fintech.transfer.infrastructure.adapter.in.rest;

import com.fintech.transfer.application.dto.*;
import com.fintech.transfer.domain.port.in.InitiateTransferUseCase;
import com.fintech.transfer.domain.port.in.PreviewTransferUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final PreviewTransferUseCase previewTransferUseCase;

    public TransferController(InitiateTransferUseCase initiateTransferUseCase,
                               PreviewTransferUseCase previewTransferUseCase) {
        this.initiateTransferUseCase = initiateTransferUseCase;
        this.previewTransferUseCase = previewTransferUseCase;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody TransferRequest request) {
        var command = new InitiateTransferCommand(
                UUID.fromString(userId),
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.description());
        TransferResponse response = initiateTransferUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/preview")
    public ResponseEntity<TransferPreviewResponse> previewTransfer(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody TransferRequest request) {
        var command = new PreviewTransferCommand(
                UUID.fromString(userId),
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.description());
        TransferPreviewResponse response = previewTransferUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    public record TransferRequest(
            String sourceAccountNumber,
            String destinationAccountNumber,
            java.math.BigDecimal amount,
            String description
    ) {}
}
