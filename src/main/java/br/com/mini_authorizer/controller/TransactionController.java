package br.com.mini_authorizer.controller;

import br.com.mini_authorizer.controller.dto.AuthorizeTransactionRequestDTO;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransactionController {

    static final String AUTHORIZED = "OK";

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String authorize(@Valid @RequestBody AuthorizeTransactionRequestDTO request) {
        transactionService.authorize(new Transaction(request.cardNumber(), request.password(), request.amount()));
        return AUTHORIZED;
    }
}
