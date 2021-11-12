package com.everis.craig.demopayment.service;

import com.everis.craig.demopayment.document.Debit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static com.everis.craig.demopayment.util.LogTraceResponse.logTraceResponse;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class DebitService {
    private final WebClient.Builder webClientBuilder;

    public DebitService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    private final Logger logger = LoggerFactory.getLogger(DebitService.class);

    public Mono<Debit> findByCardNumber(String cardNumber){
        logger.info("card_number {}", cardNumber);
        return webClientBuilder
                .baseUrl("")
                .build()
                .get()
                .uri("", Collections.singletonMap("cardNumber", cardNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError,response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException(
                            String.format("THE CARD NUMBER DONT EXIST IN MICRO SERVICE DEBIT -> %s", cardNumber)
                    ));
                })
                .bodyToMono(Debit.class);
    }

    public Mono<Debit> findByAccountNumber(String accountNumber){
        logger.info("account_number {}", accountNumber);
        return webClientBuilder
                .baseUrl("")
                .build()
                .get()
                .uri("", Collections.singletonMap("accountNumber",accountNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger,response);
                    return Mono.error(new RuntimeException(
                            String.format("THE ACCOUNT NUMBER DONT EXIST IN MICRO SERVICE DEBIT -> %s",accountNumber)
                    ));
                })
                .bodyToMono(Debit.class);
    }

    public Mono<Debit> updateDebit(Debit debit){
        return webClientBuilder
                .baseUrl("")
                .build()
                .post()
                .uri("")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(debit), Debit.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE DEBIT UPDATE FAILED"));
                })
                .bodyToMono(Debit.class);
    }


}
