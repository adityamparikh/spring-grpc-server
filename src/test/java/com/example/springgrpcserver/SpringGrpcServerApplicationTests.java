package com.example.springgrpcserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.grpc.server.port=0" // Use random port to avoid conflicts
})
class SpringGrpcServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
