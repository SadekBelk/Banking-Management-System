package com.bankingmanagement.paymentservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class AccountClient {

    private final WebClient webClient;

    public AccountClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public boolean accountExists(UUID accountId, String baseUrl) {
        return Boolean.TRUE.equals(
                webClient
                        .get()
                        .uri(baseUrl + "/api/accounts/{id}", accountId)
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
