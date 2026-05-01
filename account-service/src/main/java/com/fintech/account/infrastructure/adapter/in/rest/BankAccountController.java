package com.fintech.account.infrastructure.adapter.in.rest;

import com.fintech.account.application.dto.CreateBankAccountCommand;
import com.fintech.account.application.dto.CreateBankAccountResponse;
import com.fintech.account.domain.port.in.CreateBankAccountUseCase;
import com.fintech.account.domain.port.in.GetBalanceUseCase;
import com.fintech.common.dto.BalanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class BankAccountController {

    private final CreateBankAccountUseCase createBankAccountUseCase;
    private final GetBalanceUseCase getBalanceUseCase;

    public BankAccountController(CreateBankAccountUseCase createBankAccountUseCase,
                                  GetBalanceUseCase getBalanceUseCase) {
        this.createBankAccountUseCase = createBankAccountUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateBankAccountResponse> createAccount(
            @RequestHeader("X-User-Id") String userId) {
        var command = new CreateBankAccountCommand(UUID.fromString(userId));
        CreateBankAccountResponse response = createBankAccountUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<BalanceResponse> getBalance(@RequestHeader("X-User-Id") String userId) {
        BalanceResponse response = getBalanceUseCase.execute(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }
}
