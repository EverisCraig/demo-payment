package com.everis.craig.demopayment.repository;

import com.everis.craig.demopayment.document.Payment;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface IPaymentRepository extends IRepository<Payment, String> {
    Mono<Payment> findByObtaining_Iban(String iban);

    Mono<Void> deleteObtaining_Iban(String iban);

    Mono<Payment> findByObtaining_Detail_AccountNumber(String accountNumber);

    Mono<Payment> findByObtaining_CardNumber(String creditCard);
}
