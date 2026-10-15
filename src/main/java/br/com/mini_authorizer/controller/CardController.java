package br.com.mini_authorizer.controller;

import br.com.mini_authorizer.controller.dto.CardResponseDTO;
import br.com.mini_authorizer.controller.dto.CreateCardRequestDTO;
import br.com.mini_authorizer.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/cartoes")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponseDTO create(@Valid @RequestBody CreateCardRequestDTO request) {
        cardService.create(request.cardNumber(), request.password());
        return new CardResponseDTO(request.cardNumber(), request.password());
    }

    @GetMapping("/{card_number}")
    public BigDecimal getBalance(@PathVariable("card_number") String cardNumber) {
        return cardService.getBalance(cardNumber);
    }
}
