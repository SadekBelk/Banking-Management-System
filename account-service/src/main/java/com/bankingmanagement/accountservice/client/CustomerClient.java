package com.bankingmanagement.accountservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CustomerClient {

    private final WebClient webClient;

    public CustomerClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public boolean customerExists(UUID customerId, String baseUrl) {
        return Boolean.TRUE.equals(
                webClient
                        .get()
                        .uri(baseUrl + "/api/customers/{id}", customerId)
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return Mono.just(true);
                            } else if (response.statusCode().value() == 404) {
                                return Mono.just(false);
                            } else {
                                // For other error statuses, return false
                                return Mono.just(false);
                            }
                        })
                        .onErrorReturn(false)
                        .block()
        );
    }

}
