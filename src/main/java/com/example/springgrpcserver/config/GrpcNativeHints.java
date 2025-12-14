package com.example.springgrpcserver.config;

import com.example.springgrpcserver.proto.*;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Runtime hints for GraalVM Native Image compilation.
 *
 * GraalVM Native Image uses ahead-of-time (AOT) compilation, which means:
 * - All code paths must be known at build time
 * - Reflection usage must be explicitly declared
 * - Dynamic class loading must be avoided
 *
 * Spring Boot 4 provides automatic hints for most scenarios, but gRPC
 * protobuf-generated classes sometimes need explicit registration.
 *
 * This configuration ensures protobuf messages and service descriptors
 * are available for reflection at runtime in native images.
 */
@Configuration
@ImportRuntimeHints(GrpcNativeHints.GrpcRuntimeHints.class)
public class GrpcNativeHints {

    /**
     * Registers runtime hints for gRPC and protobuf classes.
     *
     * This is needed because:
     * 1. Protobuf uses reflection for serialization/deserialization
     * 2. gRPC uses reflection for service registration and method dispatch
     * 3. Native image compilation eliminates unused code, which can break
     *    reflection-based frameworks unless we provide hints
     */
    static class GrpcRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register protobuf message classes for reflection
            // These are used during message serialization/deserialization
            registerProtoMessages(hints);

            // Register gRPC service classes for reflection
            // These are used for service registration and method dispatch
            registerGrpcServices(hints);
        }

        /**
         * Registers protobuf message classes.
         *
         * Each message class needs reflection access for:
         * - Field access (reading/writing values)
         * - Method invocation (getters, builders)
         * - Constructor access (creating instances)
         */
        private void registerProtoMessages(RuntimeHints hints) {
            Class<?>[] messageClasses = {
                Todo.class,
                CreateTodoRequest.class,
                GetTodoRequest.class,
                ListTodosRequest.class,
                ListTodosResponse.class,
                UpdateTodoRequest.class,
                DeleteTodoRequest.class,
                DeleteTodoResponse.class,
                TodoResponse.class
            };

            for (Class<?> messageClass : messageClasses) {
                hints.reflection()
                    // Register for full reflection access (fields, methods, constructors)
                    .registerType(messageClass, hint -> hint
                        .withMembers(
                            org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                            org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                        )
                    );
            }
        }

        /**
         * Registers gRPC service classes.
         *
         * Service classes need reflection for:
         * - Method dispatch (routing RPCs to implementation methods)
         * - Service descriptor access (metadata about the service)
         */
        private void registerGrpcServices(RuntimeHints hints) {
            // Register the generated service class
            hints.reflection()
                .registerType(TodoServiceGrpc.class, hint -> hint
                    .withMembers(
                        org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                        org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                    )
                );

            // Register service descriptor for reflection
            // This is needed for server reflection and service discovery
            try {
                hints.resources().registerPattern("todo.proto");
            } catch (Exception e) {
                // Proto file resources are handled by protobuf plugin
            }
        }
    }
}
