package com.everis.craig.demopayment.service.dto;

import lombok.Data;

@Data
public class CreatePaymentWithAccountDto {
    private Double amount;
    private String description;
    private String accountNumber;
    private String creditCard;
}
