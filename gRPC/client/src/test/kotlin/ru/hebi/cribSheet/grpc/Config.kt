package ru.hebi.cribSheet.grpc

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ImportAutoConfiguration(
    value = [
        GrpcServerAutoConfiguration::class,         // Create required server beans
        GrpcServerFactoryAutoConfiguration::class,  // Select server implementation
        GrpcClientAutoConfiguration::class,         // Support @GrpcClient annotation
    ]
)
class TestServiceConfig {

    @Bean
    fun grpcService() = GrpcService()

    @Bean
    fun testService() = TestService()

}