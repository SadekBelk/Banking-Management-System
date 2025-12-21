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
        return webClient
                .get()
                .uri(baseUrl + "/api/customers/{id}", customerId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.empty()
                )
                .toBodilessEntity()
                .map(response -> true)
                .onErrorReturn(false)
                .block();
    }

}
