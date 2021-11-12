package com.everis.craig.demopayment.service;

import com.everis.craig.demopayment.document.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.everis.craig.demopayment.util.LogTraceResponse.logTraceResponse;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class TransactionService {
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public TransactionService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public Mono<Transaction> createTransaction(Transaction transaction) {
        return webClientBuilder
                .baseUrl("")
                .build()
                .post()
                .uri("")
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(transaction), Transaction.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE TRANSACTION CREATED FAILED"));
                })
                .bodyToMono(Transaction.class);
    }

}
