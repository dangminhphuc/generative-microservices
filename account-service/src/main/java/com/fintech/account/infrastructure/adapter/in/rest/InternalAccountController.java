package com.fintech.account.infrastructure.adapter.in.rest;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.port.in.GetAccountByNumberUseCase;
import com.fintech.common.dto.AccountInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/accounts")
public class InternalAccountController {

    private final GetAccountByNumberUseCase getAccountByNumberUseCase;

    public InternalAccountController(GetAccountByNumberUseCase getAccountByNumberUseCase) {
        this.getAccountByNumberUseCase = getAccountByNumberUseCase;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountInfoResponse> getAccountByNumber(@PathVariable String accountNumber) {
        AccountInfoResponse response = getAccountByNumberUseCase.execute(AccountNumber.of(accountNumber));
        return ResponseEntity.ok(response);
    }
}
