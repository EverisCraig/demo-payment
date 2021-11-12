package com.everis.craig.demopayment.service;

import com.everis.craig.demopayment.document.Detail;
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
public class DetailService {
    private final WebClient.Builder webClientBuilder;

    public DetailService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    private final Logger logger= LoggerFactory.getLogger(DetailService.class);

    public Mono<Detail> findByAccountNumber(String accountNumber){
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
                            String.format("THE ACCOUNT NUMBER DONT EXIST IN MICRO SERVICE DETAIL-> %s", accountNumber)
                    ));
                })
                .bodyToMono(Detail.class);
    }

    public Mono<Detail> findByIban(String iban){
        return webClientBuilder
                .baseUrl("")
                .build()
                .get()
                .uri("",Collections.singletonMap("", iban))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException(
                            String.format("THE IBAN DONT EXIST IN MICRO SERVICE DETAIL-> %s",iban)
                    ));
                })
                .bodyToMono(Detail.class);
    }

    public Mono<Detail> updateDetail(Detail detail){
        logger.info("BILL_WEBCLIENT_UPDATE {}",detail);
        return webClientBuilder
                .baseUrl("")
                .build()
                .post()
                .uri("")
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(detail), Detail.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE DETAIL UPDATE FAILED"));
                })
                .bodyToMono(Detail.class);
    }



}
