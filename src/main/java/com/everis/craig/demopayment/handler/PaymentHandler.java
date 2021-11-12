package com.everis.craig.demopayment.handler;

import com.everis.craig.demopayment.document.*;
import com.everis.craig.demopayment.service.*;
import com.everis.craig.demopayment.service.dto.CreatePaymentWithAccountDto;

import com.everis.craig.demopayment.service.dto.PaymentCreateDto;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class PaymentHandler {
    private final IPaymentService iPaymentService;
    private final ObtainingService obtainingService;
    private final DebitService debitService;
    private final TransactionService transactionService;
    private final DetailService detailService;

    @Autowired
    public PaymentHandler(
            IPaymentService iPaymentService,
            ObtainingService obtainingService,
            DebitService debitService,
            TransactionService transactionService,
            DetailService detailService) {
        this.iPaymentService = iPaymentService;
        this.obtainingService = obtainingService;
        this.debitService = debitService;
        this.transactionService = transactionService;
        this.detailService = detailService;
    }

    public Mono<ServerResponse> findAll() {
        return ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .body(iPaymentService.findAll(), Payment.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        return iPaymentService.findById(id).flatMap(payment -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(payment))
                .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")));
    }

    public Mono<ServerResponse> findByObtainingIban(ServerRequest request) {
        String iban = request.pathVariable("iban");
        return obtainingService.findByIban(iban).flatMap(obtaining -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(obtaining))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findDetailAccountNumber(ServerRequest request) {
        String accountNumber = request.pathVariable("accountNumber");
        return detailService.findByAccountNumber(accountNumber)
                .flatMap(detail -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(detail))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findPaymentByIban(ServerRequest request) {
        String iban = request.pathVariable("iban");
        return iPaymentService.findByObtaining_Iban(iban).flatMap(payment -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(payment))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findCreditCard(ServerRequest request) {
        String creditCard = request.pathVariable("creditCard");
        return iPaymentService.findByObtaining_CardNumber(creditCard)
                .flatMap(payment -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(payment))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> updateObtaining(ServerRequest request) {
        Mono<Obtaining> obtaining = request.bodyToMono(Obtaining.class);
        return obtaining
                .flatMap(obtainingService::updateObtaining)
                .flatMap(obtaining1 -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(obtaining1))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        return payment.flatMap(iPaymentService::create)
                .flatMap(payment1 -> ServerResponse.created(URI.create("".concat(payment1.getId())))
                        .contentType(APPLICATION_JSON)
                        .bodyValue(payment1))
                .onErrorResume(error -> Mono.error(new RuntimeException(error.getMessage())));
    }

    public Mono<Payment> createPaymentCardLess(Mono<CreatePaymentWithAccountDto> tuple) {
        return tuple
                .zipWhen(createPaymentWithAccountDto -> {
                    log.info("ACCOUNT, {}", createPaymentWithAccountDto.getAccountNumber());
                    return detailService.findByAccountNumber(createPaymentWithAccountDto.getAccountNumber());
                })
                .zipWhen(objects -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("DEPOSIT");
                    transaction.setTransactionAmount(objects.getT1().getAmount());
                    transaction.setDescription(objects.getT1().getDescription());
                    Detail detail = objects.getT2();
                    detail.setBalance(detail.getBalance() - objects.getT1().getAmount());
                    transaction.setDetail(detail);
                    return transactionService.createTransaction(transaction);
                })
                .checkpoint("after debit ")
                .flatMap(objects -> {
                    String creditCard = objects.getT1().getT1().getCreditCard();
                    if (Objects.equals(creditCard, "")) {
                        return Mono.error(() -> new RuntimeException("the credit card is invalid"));
                    }
                    return iPaymentService.findByObtaining_CardNumber(creditCard);
                })
                .zipWhen(payment -> detailService.findByAccountNumber(payment.getObtaining().getDetail().getAccountNumber()))
                .zipWhen(objects -> {
                    Detail detailUpdate = objects.getT2();
                    detailUpdate.setBalance(objects.getT1().getCreditLine());
                    return detailService.updateDetail(detailUpdate);
                })
                .zipWhen(objects -> {
                    Obtaining obtaining = objects.getT1().getT1().getObtaining();
                    obtaining.setDetail(objects.getT2());
                    return obtainingService.updateObtaining(obtaining);
                })
                .flatMap(this::paymentUpdate);
    }

    private Mono<Payment> createPaymentWithCardNotPrincipal(Mono<Tuple2<CreatePaymentWithAccountDto, Debit>> tuple) {
        return tuple
                .zipWhen(objects -> debitService.findByCardNumber(objects.getT2().getCardNumber()))
                .zipWhen(objects -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("PAY");
                    transaction.setTransactionAmount(objects.getT1().getT1().getAmount());
                    transaction.setDescription(objects.getT1().getT1().getDescription());
                    if (objects.getT1().getT1().getAmount() > objects.getT2().getPrincipal().getDetail().getBalance()) {
                        return Mono.error(() -> new RuntimeException("The retire amount exceeds the available balance in your account"));
                    } else {
                        List<Obtaining> acquires = objects.getT2().getObtainings();
                        Obtaining acquire = acquires.stream()
                                .filter(
                                        obtaining -> Objects.equals(
                                                obtaining.getDetail().getAccountNumber(), objects.getT1().getT1().getAccountNumber()))
                                .findFirst().orElse(null);
                        if (acquire == null) {
                            return Mono.error(() -> new RuntimeException("The account number does not exist"));
                        }
                        Detail detail = acquire.getDetail();
                        detail.setBalance(detail.getBalance() - objects.getT1().getT1().getAmount());
                        transaction.setDetail(detail);
                    }
                    return transactionService.createTransaction(transaction);
                })
                .zipWhen(updateDetail -> {
                    List<Obtaining> obtainings = updateDetail.getT1()
                            .getT2()
                            .getObtainings()
                            .stream()
                            .peek(obtaining -> {
                                if (Objects.equals(obtaining.getDetail().getAccountNumber(), updateDetail.getT2().getDetail().getAccountNumber())) {
                                    obtaining.setDetail(updateDetail.getT2().getDetail());
                                }
                            })
                            .collect(Collectors.toList());
                    Debit debit = new Debit();
                    debit.setObtainings(obtainings);
                    debit.setPrincipal(updateDetail.getT1().getT2().getPrincipal());
                    debit.setCardNumber(updateDetail.getT1().getT2().getCardNumber());
                    return debitService.updateDebit(debit);
                })
                .checkpoint("after debit update")
                .flatMap(objects -> {
                    String creditCard = objects.getT1().getT1().getT1().getT1().getCreditCard();
                    if (Objects.equals(creditCard, "")) {
                        return Mono.error(() -> new RuntimeException("the credit card is invalid"));
                    }
                    return iPaymentService.findByObtaining_CardNumber(creditCard);
                })
                .zipWhen(payment -> detailService.findByAccountNumber(payment.getObtaining().getDetail().getAccountNumber()))
                .zipWhen(objects -> {
                    Detail detail = objects.getT2();
                    detail.setBalance(objects.getT1().getCreditLine());
                    return detailService.updateDetail(detail);
                })
                .zipWhen(objects -> {
                    Obtaining obtaining = objects.getT1().getT1().getObtaining();
                    obtaining.setDetail(objects.getT2());
                    return obtainingService.updateObtaining(obtaining);
                })
                .flatMap(this::paymentUpdate);
    }

    private Mono<Payment> createPaymentWithCard(Mono<Tuple2<CreatePaymentWithAccountDto, Debit>> tuple) {
        return tuple
                .zipWhen(objects -> debitService.findByCardNumber(objects.getT2().getCardNumber()))
                .zipWhen(objects -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("PAY");
                    transaction.setTransactionAmount(objects.getT1().getT1().getAmount());
                    transaction.setDescription(objects.getT1().getT1().getDescription());

                    if (objects.getT1().getT1().getAmount() > objects.getT2().getPrincipal().getDetail().getBalance()) {
                        List<Obtaining> acquires = objects.getT2().getObtainings();
                        Obtaining obtaining = acquires.stream()
                                .filter(
                                        obtaining1 -> obtaining1.getDetail().getBalance() > objects.getT1().getT1().getAmount())
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("The retire amount exceeds the available balance in yours accounts"));
                        Detail detail = obtaining.getDetail();
                        detail.setBalance(detail.getBalance() - objects.getT1().getT1().getAmount());
                        transaction.setDetail(detail);
                    } else {
                        objects.getT2()
                                .getPrincipal()
                                .getDetail()
                                .setBalance(objects.getT2().getPrincipal().getDetail().getBalance() - objects.getT1().getT1().getAmount());
                        transaction.setDetail(objects.getT2().getPrincipal().getDetail());
                    }
                    return transactionService.createTransaction(transaction);
                })
                .zipWhen(updateDebit -> {
                    List<Obtaining> acquires = updateDebit.getT1()
                            .getT2()
                            .getObtainings()
                            .stream()
                            .peek(
                                    obtaining -> {
                                        if (Objects.equals(obtaining.getDetail().getAccountNumber(), updateDebit.getT2().getDetail().getAccountNumber())) {
                                            obtaining.setDetail(updateDebit.getT2().getDetail());
                                        }
                                    })
                            .collect(Collectors.toList());
                    Obtaining currentAcq = acquires.stream()
                            .filter(
                                    obtaining -> Objects.equals(obtaining.getDetail().getAccountNumber(), updateDebit.getT2().getDetail().getAccountNumber()))
                            .findFirst().orElse(null);
                    assert currentAcq != null;
                    Boolean isPrincipal = Objects.equals(updateDebit.getT1()
                            .getT2()
                            .getPrincipal()
                            .getIban(), currentAcq.getIban());
                    if (Boolean.TRUE.equals(isPrincipal)) {
                        updateDebit.getT1()
                                .getT2()
                                .getPrincipal()
                                .setDetail(updateDebit.getT2().getDetail());
                    }
                    Debit debit = new Debit();
                    debit.setObtainings(acquires);
                    debit.setPrincipal(updateDebit.getT1()
                            .getT2()
                            .getPrincipal());
                    debit.setCardNumber(updateDebit.getT1()
                            .getT2()
                            .getCardNumber());
                    return debitService.updateDebit(debit);
                })
                .checkpoint("after debit update")
                .flatMap(payment -> {
                    String creditCard = payment.getT1()
                            .getT1()
                            .getT1()
                            .getT1()
                            .getCreditCard();
                    if (Objects.equals(creditCard, "")) {
                        return Mono.error(() -> new RuntimeException("the credit card is invalid"));
                    }
                    return iPaymentService.findByObtaining_CardNumber(creditCard);
                })
                .zipWhen(payment -> detailService.findByAccountNumber(payment.getObtaining().getDetail().getAccountNumber()))
                .zipWhen(objects -> {
                    Detail detailUpdate = objects.getT2();
                    detailUpdate.setBalance(objects.getT1().getCreditLine());
                    return detailService.updateDetail(detailUpdate);
                })
                .zipWhen(updateAcq -> {
                    Obtaining obtaining = updateAcq.getT1().getT1().getObtaining();
                    obtaining.setDetail(updateAcq.getT2());
                    log.info("OBTAINING_DATA, {}", obtaining);
                    return obtainingService.updateObtaining(obtaining);
                })
                .flatMap(this::paymentUpdate);
    }

    public Mono<ServerResponse> makePaymentAccountNumber(ServerRequest request) {
        Mono<CreatePaymentWithAccountDto> paymentCreateDtoMono = request.bodyToMono(CreatePaymentWithAccountDto.class);
        return paymentCreateDtoMono
                .zipWhen(createPaymentWithAccountDto -> debitService.findByAccountNumber(createPaymentWithAccountDto.getAccountNumber())
                        .switchIfEmpty(Mono.defer(() -> Mono.just(new Debit()))))
                .flatMap(objects -> {
                    log.info("DEBIT_DATA {}", objects.getT2());
                    if (objects.getT2().getCardNumber() == null) {
                        return Mono.just(objects.getT1()).as(this::createPaymentCardLess);
                    }
                    return Mono.just(Tuples.of(objects.getT1(), objects.getT2())).as(this::createPaymentWithCard);
                })
                .flatMap(
                        payment -> ServerResponse.created(URI.create("/payment/".concat(payment.getId())))
                                .contentType(APPLICATION_JSON)
                                .bodyValue(payment))
                .log()
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }

    public Mono<ServerResponse> makePaymentWithCardNumber(ServerRequest request) {
        Mono<PaymentCreateDto> paymentCreateDto = request.bodyToMono(PaymentCreateDto.class);
        Mono<Transaction> transactionCreate = Mono.just(new Transaction());
        return Mono.zip(paymentCreateDto, transactionCreate)
                .zipWhen(objects -> debitService.findByCardNumber(objects.getT1().getCardNumber()))
                .zipWhen(objects -> {
                    Transaction transaction = objects.getT1().getT2();
                    transaction.setTransactionType("PAY");
                    transaction.setTransactionAmount(objects.getT1().getT1().getAmount());
                    transaction.setDescription(objects.getT1().getT1().getDescription());
//repeat code
                    if (objects.getT1().getT1().getAmount() > objects.getT2().getPrincipal().getDetail().getBalance()) {
                        List<Obtaining> acquires = objects.getT2().getObtainings();
                        Obtaining obtaining = acquires.stream()
                                .filter(
                                        obtaining1 -> obtaining1.getDetail().getBalance() > objects.getT1().getT1().getAmount())
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("The retire amount exceeds the available balance in yours accounts"));
                        Detail detail = obtaining.getDetail();
                        detail.setBalance(detail.getBalance() - objects.getT1().getT1().getAmount());
                        transaction.setDetail(detail);
                    } else {
                        objects.getT2()
                                .getPrincipal()
                                .getDetail()
                                .setBalance(objects.getT2().getPrincipal().getDetail().getBalance() - objects.getT1().getT1().getAmount());
                        transaction.setDetail(objects.getT2().getPrincipal().getDetail());
                    }
                    return transactionService.createTransaction(transaction);
                    //end code
                })
                .zipWhen(objects -> {
                    List<Obtaining> acquires = objects.getT1()
                            .getT2()
                            .getObtainings()
                            .stream()
                            .peek(
                                    obtaining -> {
                                        if (Objects.equals(obtaining.getDetail().getAccountNumber(), objects.getT2().getDetail().getAccountNumber())) {
                                            obtaining.setDetail(objects.getT2().getDetail());
                                        }
                                    })
                            .collect(Collectors.toList());
                    Obtaining currentAcq = acquires.stream()
                            .filter(
                                    obtaining -> Objects.equals(obtaining.getDetail().getAccountNumber(), objects.getT2().getDetail().getAccountNumber()))
                            .findFirst().orElse(null);
                    assert currentAcq != null;
                    Boolean isPrincipal = objects.getT1().getT2().getPrincipal().getIban().equals(currentAcq.getIban());
                    if (Boolean.TRUE.equals(isPrincipal)) {
                        objects.getT1()
                                .getT2().
                                getPrincipal().
                                setDetail(objects.getT2().getDetail());
                    }
                    Debit debit = new Debit();
                    debit.setObtainings(acquires);
                    debit.setPrincipal(objects.getT1()
                            .getT2()
                            .getPrincipal());
                    debit.setCardNumber(objects.getT1()
                            .getT2()
                            .getCardNumber());
                    return debitService.updateDebit(debit);
                })
                .zipWhen(
                        objects -> iPaymentService.findByObtaining_Iban(objects.getT2().getPrincipal().getIban()))
                .flatMap(
                        objects -> {
                            objects.getT2().setAmount(0.0);
                            objects.getT2().getObtaining().getDetail().setBalance(objects.getT2().getCreditLine());
                            objects.getT2().setDateTime(LocalDateTime.now().plusDays(30));
                            return iPaymentService.update(objects.getT2());
                        })
                .zipWhen(
                        payment -> detailService.findByIban(payment.getObtaining().getIban()))
                .zipWhen(
                        objects -> {
                            objects.getT2().setBalance(objects.getT1().getObtaining().getDetail().getBalance());
                            return detailService.updateDetail(objects.getT2());
                        })
                .flatMap(
                        objects -> ServerResponse.created(URI.create("/payment/".concat(objects.getT1().getT1().getId())))
                                .contentType(APPLICATION_JSON)
                                .bodyValue(objects))
                .log()
                .onErrorResume(
                        throwable -> Mono.error(new RuntimeException(throwable.getMessage())));

    }

    public Mono<ServerResponse> makePayment(ServerRequest request) {
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        Payment paymentDto = new Payment();
        return payment
                .flatMap(
                        payment1 -> {
                            paymentDto.setDescription(payment1.getDescription());
                            paymentDto.setAmount(payment1.getAmount());
                            return obtainingService.findByDetailAccountNumber(payment1.getObtaining().getDetail().getAccountNumber());
                        })
                .checkpoint("after consultation acquisition service web-client by account number")
                .flatMap(
                        obtaining -> {
                            if (Objects.equals(obtaining.getDetail().getBalance(), paymentDto.getCreditLine())) {
                                return ServerResponse.created(URI.create("/payment/".concat(paymentDto.getId())))
                                        .contentType(APPLICATION_JSON)
                                        .bodyValue("you are up to date on your payments");
                            }
                            obtaining.getDetail().setBalance(paymentDto.getAmount() - obtaining.getDetail().getBalance());
                            paymentDto.setObtaining(obtaining);
                            paymentDto.setPaymentDateTime(LocalDateTime.now());
                            return iPaymentService.create(paymentDto);
                        })
                .flatMap(
                        o -> ServerResponse.created(URI.create("/payment/".concat(paymentDto.getId())))
                                .contentType(APPLICATION_JSON)
                                .bodyValue(paymentDto));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        return payment
                .zipWhen(
                        payment1 -> iPaymentService.findByObtaining_Iban(payment1.getObtaining().getIban())
                )
                .flatMap(
                        objects -> {
                            objects.getT2().setAmount(objects.getT1().getAmount());
                            objects.getT2().setObtaining(objects.getT1().getObtaining());
                            objects.getT2().setDateTime(objects.getT2().getDateTime().plusDays(30));
                            return iPaymentService.update(objects.getT2());
                        })
                .checkpoint("after update payment", true)
                .flatMap(
                        payment1 -> ServerResponse.created(URI.create("/payment/".concat(payment1.getId())))
                                .contentType(APPLICATION_JSON)
                                .bodyValue(payment1))
                .onErrorResume(
                        throwable -> Mono.error(new RuntimeException("update payment failed")));
    }

    private Mono<? extends Payment> paymentUpdate(Tuple2<Tuple2<Tuple2<Payment, Detail>, Detail>, Obtaining> objects) {
        Payment payment = objects.getT1().getT1().getT1();
        Obtaining obtaining = objects.getT2();
        payment.setAmount(0.0);
        payment.setObtaining(obtaining);
        payment.setDateTime(LocalDateTime.now().plusDays(30));
        return iPaymentService.update(payment);
    }
}
