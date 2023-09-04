package ru.hebi.cribSheet.mail

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import ru.hebi.cribSheet.mail.service.GreenMailConfiguration

abstract class TestSupport

@Tag("integration")
@ActiveProfiles("test")
@Import(GreenMailConfiguration::class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT
)
abstract class IntegrationTest : TestSupport()

@Tag("unit-test")
@ExtendWith(MockitoExtension::class)
abstract class UnitTest : TestSupport()
