package com.example.springgrpcserver.config;

import com.example.springgrpcserver.proto.TodoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * Creates a channel to the local gRPC server on the configured port.
     * For testing, we use plaintext (no TLS) for simplicity.
     *
     * Declared as a bean so Spring shuts it down with the context; otherwise every
     * context restart (see @DirtiesContext in the tests) leaks a channel.
     */
    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel todoServiceChannel(
            @Value("${spring.grpc.server.port:9090}") int grpcPort) {
        return ManagedChannelBuilder
            .forAddress("localhost", grpcPort)
            .usePlaintext() // No TLS for testing
            .build();
    }

    /**
     * Creates a blocking stub for TodoService over the test channel.
     */
    @Bean
    public TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub(
            @Qualifier("todoServiceChannel") ManagedChannel todoServiceChannel) {
        return TodoServiceGrpc.newBlockingStub(todoServiceChannel);
    }
}
