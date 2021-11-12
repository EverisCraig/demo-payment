package com.everis.craig.demopayment.service.dto;

import lombok.Data;

@Data
public class PaymentCreateDto {
    private Double amount;
    private String description;
    private String cardNumber;
    private String iban;
}
