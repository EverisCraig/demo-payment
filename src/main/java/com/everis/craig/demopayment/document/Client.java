package com.everis.craig.demopayment.document;

import lombok.Data;

@Data
public class Client {
    private String clientIdentityType;
    private String clientIdentityNumber;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String clientType;
}
