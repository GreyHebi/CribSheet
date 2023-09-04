package ru.hebi.cribSheet.mail.service

import com.icegreen.greenmail.spring.GreenMailBean
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import ru.hebi.cribSheet.mail.IntegrationTest
import ru.hebi.cribSheet.mail.UnitTest
import java.util.*

const val RECIPIENT = "alex@localhost"
const val SUBJECT = "Некая тема"
const val TEXT = """
Какой-то длинный текст письма
"""

@DisplayName("(Unit) Сервис по отправке сообщений по электронной почте")
class MailSenderServiceUnitTest : UnitTest() {

    @Mock
    lateinit var sender: JavaMailSender

    @InjectMocks
    lateinit var target: MailSenderService


    @DisplayName("Отправка простого письма")
    @Test
    fun `send simple message`() {
        Mockito.`when`(sender.createMimeMessage())
            .thenReturn(MimeMessage(null as Session?))

        target.send(Message(
                recipient = RECIPIENT,
                subject = SUBJECT,
                text = TEXT
        ))

        verify(sender).send(any(MimeMessage::class.java))
    }

}

@DisplayName("(Integration) Сервис по отправке сообщений по электронной почте")
class MailSenderServiceIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var greenMailBean: GreenMailBean

    @Autowired
    lateinit var target: MailSenderService

    @BeforeEach fun before() {
        assertTrue(greenMailBean.isStarted)
    }

    @DisplayName("Отправка простого письма")
    @Test
    fun `send simple message`() {
        val actual = target.send(Message(
                recipient = RECIPIENT,
                subject = SUBJECT,
                text = TEXT
        ))

        assertTrue(actual)
        greenMailBean.receivedMessages.forEach {
            assertAll(
                "mail",
                { assertEquals(SUBJECT, it.subject) },
                { assertEquals(RECIPIENT, it.allRecipients.first().toString()) },
                {
                    //почему-то сообщение передаётся в BASE64
                    val rawMessage = it.rawInputStream.readAllBytes()
                    val encodeMessage = Base64.getDecoder().decode(rawMessage)
                    assertEquals(TEXT, String(encodeMessage))
                },
            )
        }
    }

}

@TestConfiguration
class GreenMailConfiguration {
    @Bean
    fun greenMailBean() = GreenMailBean().apply {
        users = listOf(
            "ivan:12345@localhost",
            "alex:09876@localhost"
        )
        hostname = "127.0.0.1"
        portOffset = 3000
        isAutostart = true
        isSmtpProtocol = true
        isPop3Protocol = true
    }
}
