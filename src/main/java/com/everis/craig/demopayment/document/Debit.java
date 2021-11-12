package com.everis.craig.demopayment.document;

import lombok.Data;

import java.util.List;

@Data
public class Debit {
    private List<Obtaining> obtainings;
    private Obtaining principal;
    private String cardNumber;
}
