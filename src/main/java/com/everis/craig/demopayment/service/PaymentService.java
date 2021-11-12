package com.everis.craig.demopayment.service;

import com.everis.craig.demopayment.document.Payment;
import com.everis.craig.demopayment.repository.IPaymentRepository;
import com.everis.craig.demopayment.repository.IRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentService extends BaseService<Payment, String> implements IPaymentService {

    private final IPaymentRepository paymentRepository;

    @Autowired
    public PaymentService(IPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    protected IRepository<Payment, String> getRepository() {
        return paymentRepository;
    }

    @Override
    public Mono<Payment> findByObtaining_Iban(String iban) {
        return paymentRepository.findByObtaining_Iban(iban);
    }

    @Override
    public Mono<Void> deleteByObtaining_Iban(String iban) {
        return paymentRepository.deleteObtaining_Iban(iban);
    }

    @Override
    public Mono<Payment> findByObtaining_Detail_AccountNumber(String accountNumber) {
        return paymentRepository.findByObtaining_Detail_AccountNumber(accountNumber);
    }

    @Override
    public Mono<Payment> findByObtaining_CardNumber(String creditCard) {
        return paymentRepository.findByObtaining_CardNumber(creditCard);
    }
}
