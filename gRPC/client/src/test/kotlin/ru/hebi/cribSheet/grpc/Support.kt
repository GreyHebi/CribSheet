package ru.hebi.cribSheet.grpc

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.StringUtils
import java.nio.file.Path
import java.util.*

abstract class TestSupport {

    fun path(path: String) : Path {
        val a = StringUtils.cleanPath(path)
        val b = this.javaClass.getResource(a) ?: error("$path не найден")
        return Path.of(b.toURI())
    }

}

@Tag("integration")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "grpc.server.inProcessName=test",                  // Enable inProcess server
        "grpc.server.port=-1",                             // Disable external server
        "grpc.client.sendService.address=in-process:test", // Configure the client to connect to the inProcess server
    ],
)
abstract class IntegrationTest : TestSupport()






@Tag("unit-test")
@ExtendWith(MockKExtension::class)
abstract class UnitTest : TestSupport()
