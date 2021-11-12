package com.everis.craig.demopayment.service;
import com.everis.craig.demopayment.document.Client;
import com.everis.craig.demopayment.exception.ArgumentWebClientNotValid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import java.util.Collections;

import static com.everis.craig.demopayment.util.LogTraceResponse.logTraceResponse;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class ClientService {
    private final WebClient.Builder webClientBuilder;

    public ClientService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    private final Logger logger = LoggerFactory.getLogger(ClientService.class);

    private Mono<Client> findByIdentityNumber(String identityNumber) {
        logger.info("identityNumber {}" + identityNumber);
        return webClientBuilder
                .baseUrl("")
                .build()
                .get()
                .uri("", Collections.singletonMap("identityNumber", identityNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new ArgumentWebClientNotValid(
                            String.format("THE PRODUCT NAME DONT EXIST IN MICRO SERVICE PRODUCT-> %s", identityNumber)
                    ));
                })
                .bodyToMono(Client.class);

    }


}
