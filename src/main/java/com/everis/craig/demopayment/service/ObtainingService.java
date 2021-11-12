package com.everis.craig.demopayment.service;

import com.everis.craig.demopayment.document.Obtaining;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static com.everis.craig.demopayment.util.LogTraceResponse.logTraceResponse;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class ObtainingService {
    private final WebClient.Builder webClientBuilder;

    public ObtainingService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    private final Logger logger = LoggerFactory.getLogger(ObtainingService.class);

    public Mono<Obtaining> findByDetailAccountNumber(String accountNumber) {
        return webClientBuilder
                .baseUrl("")
                .build()
                .get()
                .uri("", Collections.singletonMap("", accountNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE OBTAINING FIND FAILED"));
                })
                .bodyToMono(Obtaining.class);
    }

    public Mono<Obtaining> findByIban(String iban) {
        return webClientBuilder
                .baseUrl("")
                .build()
                .get()
                .uri("", Collections.singletonMap("", iban))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE OBTAINING FIND FAILED"));
                })
                .bodyToMono(Obtaining.class);
    }

    public Mono<Obtaining> updateObtaining(Obtaining obtaining) {
        logger.info("OBTAINING_WEBCLIENT_UPDATE {}", obtaining);
        return webClientBuilder
                .baseUrl("")
                .build()
                .post()
                .uri("")
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(obtaining), Obtaining.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE OBTAINING UPDATE FAILED"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    return Mono.error(new RuntimeException("THE OBTAINING UPDATE FAILED"));
                })
                .bodyToMono(Obtaining.class);
    }


}
