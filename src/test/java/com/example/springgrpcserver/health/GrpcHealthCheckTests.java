package com.example.springgrpcserver.health;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for gRPC Health Check functionality.
 * <p>
 * These tests verify the standard gRPC health check protocol (grpc.health.v1.Health)
 * following the pattern from Spring gRPC samples:
 * https://github.com/spring-projects/spring-grpc/blob/main/samples/grpc-server/src/test/java/org/springframework/grpc/sample/GrpcServerHealthIntegrationTests.java
 *
 * <h2>What are gRPC Health Checks?</h2>
 * <p>
 * gRPC Health Checks provide a standardized way for clients and load balancers
 * to verify that a gRPC service is operational. The protocol is defined in
 * {@code grpc.health.v1} and supports:
 * <ul>
 *   <li><b>Overall health</b> - Check if the server is running</li>
 *   <li><b>Service-specific health</b> - Check individual service availability</li>
 *   <li><b>Health watching</b> - Stream health status changes (advanced)</li>
 * </ul>
 *
 * <h2>Health Check RPC Methods</h2>
 * <ul>
 *   <li>{@code Check} - Immediate health status query (used in these tests)</li>
 *   <li>{@code Watch} - Streaming health updates (for advanced monitoring)</li>
 * </ul>
 *
 * <h2>Serving Status</h2>
 * <ul>
 *   <li>{@code SERVING} - Service is healthy and accepting requests</li>
 *   <li>{@code NOT_SERVING} - Service is unhealthy or shutting down</li>
 *   <li>{@code SERVICE_UNKNOWN} - Service not registered (returns NOT_FOUND error)</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:healthtestdb",
        "spring.grpc.server.health.enabled=true"
})
@Import(HealthGrpcTestConfiguration.class)
class GrpcHealthCheckTests {

    /**
     * Health check client stub configured by HealthGrpcTestConfiguration.
     * Connects to the embedded gRPC server started by @SpringBootTest.
     */
    @Autowired
    private HealthGrpc.HealthBlockingStub healthStub;

    /**
     * Test overall server health check.
     *
     * <p>When checking health with an empty service name, the server returns
     * the overall health status. This is useful for load balancer health checks.
     *
     * <h3>Example with grpcurl:</h3>
     * <pre>
     * grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check
     * </pre>
     */
    @Test
    void testOverallHealthCheck() {
        // Request overall server health (empty service name)
        HealthCheckRequest request = HealthCheckRequest.newBuilder()
                .build();

        HealthCheckResponse response = healthStub.check(request);

        // Verify server is serving
        assertThat(response.getStatus())
                .as("Overall server health should be SERVING")
                .isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
    }

    /**
     * Test health check for a specific service.
     *
     * <p>Note: Spring gRPC health service by default only provides overall server
     * health. Per-service health checks require additional configuration.
     * This test verifies that querying a specific service that hasn't been
     * registered with the health service returns NOT_FOUND.
     *
     * <h3>Example with grpcurl:</h3>
     * <pre>
     * grpcurl -plaintext -d '{"service": "todo.TodoService"}' \
     *   localhost:9090 grpc.health.v1.Health/Check
     * </pre>
     */
    @Test
    void testServiceSpecificHealthCheck() {
        // Request health for TodoService (not registered with health service)
        HealthCheckRequest request = HealthCheckRequest.newBuilder()
                .setService("todo.TodoService")
                .build();

        // Expect NOT_FOUND since per-service health isn't configured
        assertThatThrownBy(() -> healthStub.check(request))
                .isInstanceOf(io.grpc.StatusRuntimeException.class)
                .satisfies(e -> {
                    io.grpc.StatusRuntimeException sre = (io.grpc.StatusRuntimeException) e;
                    assertThat(sre.getStatus().getCode())
                            .as("Service-specific health check should return NOT_FOUND when not configured")
                            .isEqualTo(io.grpc.Status.Code.NOT_FOUND);
                });
    }

    /**
     * Test health check for non-existent service.
     *
     * <p>When querying a service that doesn't exist, the health check service
     * should return a NOT_FOUND error (not to be confused with NOT_SERVING status).
     *
     * <h3>Status vs Error:</h3>
     * <ul>
     *   <li><b>NOT_SERVING status</b> - Service exists but is unhealthy</li>
     *   <li><b>NOT_FOUND error</b> - Service doesn't exist at all</li>
     * </ul>
     */
    @Test
    void testNonExistentServiceHealthCheck() {
        // Request health for a service that doesn't exist
        HealthCheckRequest request = HealthCheckRequest.newBuilder()
                .setService("nonexistent.Service")
                .build();

        // Expect NOT_FOUND status exception
        assertThatThrownBy(() -> healthStub.check(request))
                .isInstanceOf(io.grpc.StatusRuntimeException.class)
                .satisfies(e -> {
                    io.grpc.StatusRuntimeException sre = (io.grpc.StatusRuntimeException) e;
                    assertThat(sre.getStatus().getCode())
                            .as("Non-existent service should return NOT_FOUND")
                            .isEqualTo(io.grpc.Status.Code.NOT_FOUND);
                });
    }

    /**
     * Test that health checks respond quickly.
     *
     * <p>Health checks must be fast (< 100ms) to avoid timing out load balancer
     * health checks, which typically have short timeouts.
     *
     * <h3>Why Speed Matters:</h3>
     * <ul>
     *   <li>Load balancers typically timeout health checks after 1-5 seconds</li>
     *   <li>Slow health checks can cause false negatives</li>
     *   <li>Multiple failed checks trigger service removal from load balancer</li>
     * </ul>
     */
    @Test
    void testHealthCheckPerformance() {
        HealthCheckRequest request = HealthCheckRequest.newBuilder().build();

        long startTime = System.currentTimeMillis();
        HealthCheckResponse response = healthStub.check(request);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(response.getStatus()).isNotNull();
        assertThat(duration)
                .as("Health check should respond in less than 100ms")
                .isLessThan(100L);
    }

    /**
     * Test multiple concurrent health checks.
     *
     * <p>Health check service should handle concurrent requests efficiently
     * without degradation, as multiple load balancers or monitoring systems
     * may check health simultaneously.
     *
     * <h3>Real-world Scenario:</h3>
     * <p>In production, you might have:
     * <ul>
     *   <li>Multiple load balancer instances</li>
     *   <li>Monitoring systems (Prometheus, Datadog, etc.)</li>
     *   <li>Kubernetes liveness/readiness probes</li>
     * </ul>
     * All checking health concurrently every few seconds.
     */
    @Test
    void testConcurrentHealthChecks() throws InterruptedException {
        int concurrentRequests = 10;
        Thread[] threads = new Thread[concurrentRequests];

        for (int i = 0; i < concurrentRequests; i++) {
            threads[i] = new Thread(() -> {
                HealthCheckRequest request = HealthCheckRequest.newBuilder().build();
                HealthCheckResponse response = healthStub.check(request);
                assertThat(response.getStatus())
                        .isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout per thread
            assertThat(thread.isAlive())
                    .as("Health check thread should complete within timeout")
                    .isFalse();
        }
    }

}

/**
 * Test configuration for gRPC Health Check client.
 */
@Configuration
class HealthGrpcTestConfiguration {

    @Bean
    public HealthGrpc.HealthBlockingStub healthStub(
            @org.springframework.beans.factory.annotation.Value("${spring.grpc.server.port:9090}") int grpcPort) {

        io.grpc.ManagedChannel channel = io.grpc.ManagedChannelBuilder
                .forAddress("localhost", grpcPort)
                .usePlaintext()
                .build();

        return HealthGrpc.newBlockingStub(channel);
    }
}
