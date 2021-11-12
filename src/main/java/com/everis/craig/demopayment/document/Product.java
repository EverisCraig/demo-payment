package com.everis.craig.demopayment.document;

import com.everis.craig.demopayment.document.Politics;
import lombok.Data;

@Data
public class Product {
    private String productName;
    private String productType;
    private Politics politics;
}
