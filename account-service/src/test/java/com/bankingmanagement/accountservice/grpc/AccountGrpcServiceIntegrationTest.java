package com.bankingmanagement.accountservice.grpc;

import com.banking.proto.account.*;
import com.banking.proto.common.Money;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AccountGrpcService.
 * 
 * Tests the complete gRPC flow for ReserveBalance operation.
 * Uses a random (ephemeral) port to avoid local port conflicts.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
                "grpc.server.port=0",
                                "grpc.server.in-process-name=test"
    }
)
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountGrpcServiceIntegrationTest {

    private ManagedChannel channel;
    private AccountServiceGrpc.AccountServiceBlockingStub blockingStub;

    @BeforeEach
    void setupChannel() {
        if (channel == null || channel.isShutdown()) {
                        channel = InProcessChannelBuilder
                                        .forName("test")
                                        .directExecutor()
                                        .build();
            
            blockingStub = AccountServiceGrpc.newBlockingStub(channel);
        }
    }

    @AfterEach
    void teardownChannel() throws InterruptedException {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            channel = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("GetBalance should return NOT_FOUND for non-existent account")
    void getBalance_nonExistentAccount_returnsNotFound() {
        // Given
        String fakeAccountId = UUID.randomUUID().toString();
        GetBalanceRequest request = GetBalanceRequest.newBuilder()
                .setAccountId(fakeAccountId)
                .build();

        // When & Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.getBalance(request)
        );

        assertEquals(io.grpc.Status.Code.NOT_FOUND, exception.getStatus().getCode());
        System.out.println("✅ GetBalance correctly returns NOT_FOUND for non-existent account");
    }

    @Test
    @Order(2)
    @DisplayName("ReserveBalance should return NOT_FOUND for non-existent account")
    void reserveBalance_nonExistentAccount_returnsNotFound() {
        // Given
        String fakeAccountId = UUID.randomUUID().toString();
        ReserveBalanceRequest request = ReserveBalanceRequest.newBuilder()
                .setAccountId(fakeAccountId)
                .setAmount(Money.newBuilder()
                        .setAmount(100)
                        .setCurrency("USD")
                        .build())
                .setIdempotencyKey(UUID.randomUUID().toString())
                .build();

        // When & Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.reserveBalance(request)
        );

        assertEquals(io.grpc.Status.Code.NOT_FOUND, exception.getStatus().getCode());
        System.out.println("✅ ReserveBalance correctly returns NOT_FOUND for non-existent account");
    }

    @Test
    @Order(3)
    @DisplayName("ReserveBalance should return INVALID_ARGUMENT for missing idempotency key")
    void reserveBalance_missingIdempotencyKey_returnsInvalidArgument() {
        // Given
        String fakeAccountId = UUID.randomUUID().toString();
        ReserveBalanceRequest request = ReserveBalanceRequest.newBuilder()
                .setAccountId(fakeAccountId)
                .setAmount(Money.newBuilder()
                        .setAmount(100)
                        .setCurrency("USD")
                        .build())
                // Missing idempotency key!
                .build();

        // When & Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.reserveBalance(request)
        );

        assertEquals(io.grpc.Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("idempotency_key"));
        System.out.println("✅ ReserveBalance correctly returns INVALID_ARGUMENT for missing idempotency key");
    }

    @Test
    @Order(4)
    @DisplayName("CommitReservation should return NOT_FOUND for non-existent reservation")
    void commitReservation_nonExistentReservation_returnsNotFound() {
        // Given
        String fakeReservationId = UUID.randomUUID().toString();
        CommitReservationRequest request = CommitReservationRequest.newBuilder()
                .setReservationId(fakeReservationId)
                .setTransactionId("txn-123")
                .build();

        // When & Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.commitReservation(request)
        );

        assertEquals(io.grpc.Status.Code.NOT_FOUND, exception.getStatus().getCode());
        System.out.println("✅ CommitReservation correctly returns NOT_FOUND for non-existent reservation");
    }

    @Test
    @Order(5)
    @DisplayName("ReleaseReservation should return NOT_FOUND for non-existent reservation")
    void releaseReservation_nonExistentReservation_returnsNotFound() {
        // Given
        String fakeReservationId = UUID.randomUUID().toString();
        ReleaseReservationRequest request = ReleaseReservationRequest.newBuilder()
                .setReservationId(fakeReservationId)
                .setReason("Test release")
                .build();

        // When & Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> blockingStub.releaseReservation(request)
        );

        assertEquals(io.grpc.Status.Code.NOT_FOUND, exception.getStatus().getCode());
        System.out.println("✅ ReleaseReservation correctly returns NOT_FOUND for non-existent reservation");
    }
}
