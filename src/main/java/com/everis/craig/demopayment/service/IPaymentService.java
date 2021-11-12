package com.everis.craig.demopayment.service;

import com.everis.craig.demopayment.document.Payment;
import reactor.core.publisher.Mono;

public interface IPaymentService extends IBaseService<Payment, String> {
    Mono<Payment> findByObtaining_Iban(String iban);

    Mono<Void> deleteByObtaining_Iban(String iban);

    Mono<Payment> findByObtaining_Detail_AccountNumber(String accountNumber);

    Mono<Payment> findByObtaining_CardNumber(String creditCard);
}
