package com.everis.craig.demopayment.document;

import com.everis.craig.demopayment.document.Detail;
import lombok.Data;

@Data
public class Transaction {
    private String transactionType;
    private Double transactionAmount;
    private Detail detail;
    private Double commission;
    private String description="";
}
