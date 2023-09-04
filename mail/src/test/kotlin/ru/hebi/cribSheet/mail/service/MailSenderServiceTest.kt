package ru.hebi.cribSheet.mail.service

import com.icegreen.greenmail.spring.GreenMailBean
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import ru.hebi.cribSheet.mail.IntegrationTest
import ru.hebi.cribSheet.mail.UnitTest
import java.util.*

private const val AUTHOR = "ivan@localhost"
private const val RECIPIENT = "alex@localhost"
private const val SUBJECT = "Некая тема"
private const val TEXT = "Какой-то длинный текст письма"
private val ATTACHMENT = ByteAttachment("filename.txt", "Текст во вложении".toByteArray())
private const val FILENAME = "file.txt"

@DisplayName("(Unit) Сервис по отправке сообщений по электронной почте")
class MailSenderServiceUnitTest : UnitTest() {

    @Mock
    lateinit var sender: JavaMailSender

    @InjectMocks
    lateinit var target: MailSenderService

    @BeforeEach fun beforeEach() {
        Mockito.`when`(sender.createMimeMessage())
            .thenReturn(MimeMessage(null as Session?))
    }

    @DisplayName("Ошибка при отправке сообщение - ошибка аутентификации")
    @Test
    fun unauthorized() {
        Mockito.`when`(sender.send(any(MimeMessage::class.java)))
            .thenThrow(MailAuthenticationException("Не знаю такого"))

        val actual = target.send(Message(
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
        ))

        assertFalse(actual)
    }

    @DisplayName("Ошибка при отправке сообщение - неизвестная ошибка при отправке")
    @Test
    fun `some exception on send`() {
        Mockito.`when`(sender.send(any(MimeMessage::class.java)))
            .thenThrow(MailSendException("Тайм-аут?"))

        val actual = target.send(Message(
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
        ))

        assertFalse(actual)
    }

    @DisplayName("Ошибка при отправке сообщение - ошибка при маппинге")
    @Test
    fun `mapping exception`() {
        val actual = target.send(Message(
            recipient = "(крывой получатель",
            subject = SUBJECT,
            text = TEXT,
        ))

        assertFalse(actual)
    }

    @DisplayName("Ошибка при отправке сообщение - неизвестная ошибка")
    @Test
    fun `another exception`() {
        Mockito.`when`(sender.createMimeMessage())
            .thenReturn(null as MimeMessage?)

        val actualException = assertThrows<Exception> { target.send(Message(
                recipient = RECIPIENT,
                subject = SUBJECT,
                text = TEXT,
            ))
        }
        print("Поймано исключение: $actualException")
    }

    @DisplayName("Отправка простого письма")
    @Test
    fun `send simple message`() {
        target.send(Message(
                recipient = RECIPIENT,
                subject = SUBJECT,
                text = TEXT,
        ))

        verify(sender).send(any(MimeMessage::class.java))
    }

    @DisplayName("Отправка сообщения с байтовым вложением")
    @Test
    fun `send with byte attachment`() {
        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
        )
        message.addAttachment(ATTACHMENT.filename, ATTACHMENT.data)
        target.send(message)

        verify(sender).send(any(MimeMessage::class.java))
    }

    @DisplayName("Отправка сообщения с вложенным файлом")
    @Test
    fun `send with file attachment`() {
        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
        )
        message.addAttachment(path(FILENAME))
        target.send(message)

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
                text = TEXT,
        ))

        assertTrue(actual)
        greenMailBean.receivedMessages.forEach {
            assertAll("mail",
                { assertEquals(SUBJECT, it.subject) },
                { assertEquals(RECIPIENT, it.allRecipients.first().toString()) },
                { assertEqualsContent(TEXT, it) },
            )
        }
    }

    @DisplayName("Отправка письма с байтовым вложением")
    @Test
    fun `send with byte attachment`() {
        val attachment = Byte(ATTACHMENT.filename, ATTACHMENT.data)
        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
            attachment = mutableListOf(attachment),
        )

        val actual = target.send(message)

        assertTrue(actual)
        val actualMail = greenMailBean.receivedMessages.last()
        assertAll("mail",
            { assertEquals(AUTHOR, actualMail.from.first().toString()) },
            { assertEquals(RECIPIENT, actualMail.allRecipients.first().toString()) },
            { assertEquals(SUBJECT, actualMail.subject) },
            { assertEqualsContent(TEXT, actualMail) },
            { assertEqualsAttachment(attachment, actualMail) }
        )
    }

    @DisplayName("Отправка письма с вложенным файлом")
    @Test
    fun `send with file attachment`() {
        val attachment = File(path(FILENAME))
        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
            attachment = mutableListOf(attachment),
        )

        val actual = target.send(message)

        assertTrue(actual)
        val actualMail = greenMailBean.receivedMessages.last()
        assertAll("mail",
            { assertEquals(AUTHOR, actualMail.from.first().toString()) },
            { assertEquals(RECIPIENT, actualMail.allRecipients.first().toString()) },
            { assertEquals(SUBJECT, actualMail.subject) },
            { assertEqualsContent(TEXT, actualMail) },
            { assertEqualsAttachment(attachment, actualMail) }
        )
    }

}


/** Внутренний тестовый класс для байтовых вложений */
private data class ByteAttachment(
    val filename: String,
    val data: ByteArray,
)

private fun assertEqualsContent(expected: String, actual: MimeMessage) {
    /* Т.к. сообщение передается в Base64 и бывает MultiPart (когда с вложениями) проще удостовериться,
       что ожидаемый текст присутствует(!) в теле письма */
    val expectedInBase64 = Base64.getEncoder().encodeToString(expected.toByteArray())
    val message = actual.contentInString()
    assertTrue(message.contains(expectedInBase64)) {
        "В сообщении '$message' отсутствует ожидаемый текст сообщения '$expected' (в Base64: '$expectedInBase64')"
    }
}

private fun assertEqualsAttachment(expected: Attachment, actual: MimeMessage) {
    /* Аналогично с методом выше: проще удостовериться, что вложенный файл присутствует в теле письма,
       чем его выковыривать */
    val expectedBytes = expected.source().inputStream.readAllBytes()
    val expectedInBase64 = Base64.getEncoder().encodeToString(expectedBytes)
    val message = actual.contentInString()

    assertAll("attachment",
        {
            assertTrue(message.contains("Content-Disposition: attachment; filename=${expected.filename()}")) {
                "В сообщении '$message' отсутствует наименование вложенного файла '${expected.filename()}'"
            }
        },
        {
            assertTrue(message.contains(expectedInBase64)) {
                "В сообщении '$message' отсутствует ожидаемый текст вложенного файла '$expected' (в Base64: '$expectedInBase64')"
            }
        },
    )
}

private fun MimeMessage.contentInString() = String(rawInputStream.readAllBytes())