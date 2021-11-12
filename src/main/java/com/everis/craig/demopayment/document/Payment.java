package com.everis.craig.demopayment.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "payment")
public class Payment {
    @Id
    private String id;
    private Double amount;
    private Obtaining obtaining;
    private String description;
    private Double creditLine;
    private String status;
    private LocalDateTime dateTime;
    private LocalDateTime paymentDateTime=LocalDateTime.now();
}
