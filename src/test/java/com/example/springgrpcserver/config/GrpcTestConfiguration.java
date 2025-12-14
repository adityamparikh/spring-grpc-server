package com.example.springgrpcserver.config;

import com.example.springgrpcserver.proto.TodoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for gRPC client setup.
 *
 * This configuration creates a gRPC client channel and stub for testing.
 * The channel connects to the embedded gRPC server started by @SpringBootTest.
 */
@TestConfiguration
public class GrpcTestConfiguration {

    /**
     * Creates a blocking stub for TodoService.
     *
     * The stub connects to the local gRPC server on the configured port.
     * For testing, we use plaintext (no TLS) for simplicity.
     */
    @Bean
    public TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub(
            @Value("${spring.grpc.server.port:9090}") int grpcPort) {

        // Create a channel to localhost on the gRPC server port
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress("localhost", grpcPort)
            .usePlaintext() // No TLS for testing
            .build();

        // Create and return the blocking stub
        return TodoServiceGrpc.newBlockingStub(channel);
    }
}
