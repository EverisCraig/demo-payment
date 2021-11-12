package com.everis.craig.demopayment.document;

import lombok.Data;

import java.util.List;

@Data
public class Obtaining {
    private Product product;
    private List<Client> clientHolder;
    private List<Client> clientAuthorizedSigner;
    private double initial;
    private String iban;
    private String cardNumber;
    private Detail detail;
}
