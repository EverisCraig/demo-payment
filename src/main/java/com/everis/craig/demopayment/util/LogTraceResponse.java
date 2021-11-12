package com.everis.craig.demopayment.util;

import org.slf4j.Logger;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.scheduler.Schedulers;

public class LogTraceResponse {
    public static void logTraceResponse(Logger logger, ClientResponse response) {
        if (logger.isTraceEnabled()){
            logger.trace("Response status: {}", response.statusCode());
            logger.trace("Response headers: {}", response.headers().asHttpHeaders());
            response.bodyToMono(String.class)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(s -> logger.trace("Response body: {}", s));
        }
    }
}
