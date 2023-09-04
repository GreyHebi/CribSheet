package ru.hebi.cribSheet.mail

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.StringUtils
import ru.hebi.cribSheet.mail.config.GreenMailConfiguration
import java.nio.file.Path

abstract class TestSupport {

    fun path(path: String) : Path {
        val a = StringUtils.cleanPath(path)
        val b = this.javaClass.getResource(a) ?: error("$path не найден")
        return Path.of(b.toURI())
    }

}

@Tag("integration")
@ActiveProfiles("test")
@Import(GreenMailConfiguration::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTest : TestSupport()

@Tag("unit-test")
@ExtendWith(MockitoExtension::class)
abstract class UnitTest : TestSupport()
