package ru.hebi.cribSheet.mail.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import ru.hebi.cribSheet.mail.*
import java.util.*

private const val AUTHOR = "ivan@localhost"
private const val RECIPIENT = "alex@localhost"
private const val ANOTHER_RECIPIENT = "sergey@localhost"
private const val SUBJECT = "Некая тема"
private const val TEXT = "Какой-то длинный текст письма"
private val ATTACHMENT = ByteAttachment("filename.txt", "Текст во вложении".toByteArray())
private const val FILENAME = "file.txt"

@DisplayName("(Unit) Сервис по отправке сообщений по электронной почте")
class MailSenderServiceUnitTest : UnitTest() {

    private val sender: JavaMailSender = mockk()
    private val target: MailSenderService = MailSenderService(sender)

    @BeforeEach fun beforeEach() {
        every { sender.createMimeMessage() } returns MimeMessage(null as Session?)
    }

    @DisplayName("Ошибка при отправке сообщение - ошибка аутентификации")
    @Test
    fun unauthorized() {
        every { sender.send(ofType(MimeMessage::class)) } throws MailAuthenticationException("Не знаю такого")

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
        every { sender.send(ofType(MimeMessage::class)) } throws MailSendException("Тайм-аут?")

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
        //приходится на такие изощрения идти, лишь бы вернуть null
        every { sender.createMimeMessage() as MimeMessage? } returns null as MimeMessage?

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
        //таким образом говорим, что нужно вернуть void; иначе падает исключение, типа для мока не заготовлены ответы
        every { sender.send(ofType(MimeMessage::class)) } returns Unit

        target.send(Message(
                recipient = RECIPIENT,
                subject = SUBJECT,
                text = TEXT,
        ))

        verify(exactly = 1) { sender.send(ofType(MimeMessage::class)) }
    }

    @DisplayName("Отправка простого письма нескольким получателям")
    @Test
    fun `send simple message to several recipients`() {
        every { sender.send(ofType(MimeMessage::class)) } returns Unit

        target.send(Message(
            recipients = setOf(RECIPIENT, ANOTHER_RECIPIENT),
            subject = SUBJECT,
            text = TEXT,
        ))

        verify(exactly = 1) { sender.send(ofType(MimeMessage::class)) }
    }

    @DisplayName("Отправка сообщения с байтовым вложением")
    @Test
    fun `send with byte attachment`() {
        every { sender.send(ofType(MimeMessage::class)) } returns Unit

        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
            attachments = setOf(attachment(ATTACHMENT.filename, ATTACHMENT.data))
        )
        target.send(message)

        verify(exactly = 1) { sender.send(ofType(MimeMessage::class)) }
    }

    @DisplayName("Отправка сообщения с вложенным файлом")
    @Test
    fun `send with file attachment`() {
        every { sender.send(ofType(MimeMessage::class)) } returns Unit

        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = SUBJECT,
            text = TEXT,
            attachments = setOf(attachment(path(FILENAME)))
        )
        target.send(message)

        verify(exactly = 1) { sender.send(ofType(MimeMessage::class)) }
    }

}

@DisplayName("(Integration) Сервис по отправке сообщений по электронной почте")
class MailSenderServiceIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var target: MailSenderService

    @BeforeEach fun before() {
        launchGreenMail()
    }

    @AfterEach fun after() {
        stopGreenMail()
    }

    @DisplayName("Отправка простого письма")
    @Test
    fun `send simple message`() {
        val subject = "тема простого письмеца"
        val text = "тело простенького письмеца"
        val message = Message(
            recipient = RECIPIENT,
            subject = subject,
            text = text,
        )
        val actual = target.send(message)

        assertTrue(actual)
        assertMail(message, greenMailBean.receivedMessages.last())
    }

    @DisplayName("Отправка простого письма нескольким получателям")
    @Test
    fun `send simple message to several recipients`() {
        val subject = "тема простого письмеца для нескольких получателей"
        val text = "тело простенького письмеца для нескольких получателей"
        val recipients = setOf(RECIPIENT, ANOTHER_RECIPIENT)
        val message = Message(
            recipients = recipients,
            subject = subject,
            text = text,
        )
        val actual = target.send(message)

        assertTrue(actual)
        assertMail(message, greenMailBean.receivedMessages.last())
    }

    @DisplayName("Отправка письма с байтовым вложением")
    @Test
    fun `send with byte attachment`() {
        val subject = "темка письмеца с байтовым вложением"
        val text = "тельце письмеца"
        val attachment = Byte(ATTACHMENT.filename, ATTACHMENT.data)
        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = subject,
            text = text,
            attachments = setOf(attachment),
        )

        val actual = target.send(message)

        assertTrue(actual)
        assertMail(message, greenMailBean.receivedMessages.last())
    }

    @DisplayName("Отправка письма с вложенным файлом")
    @Test
    fun `send with file attachment`() {
        val subject = "темка письмеца с файлом"
        val text = "тельце письмеца, а дальше файлик"
        val attachment = File(path(FILENAME))
        val message = Message(
            from = AUTHOR,
            recipient = RECIPIENT,
            subject = subject,
            text = text,
            attachments = setOf(attachment)
        )

        val actual = target.send(message)

        assertTrue(actual)
        assertMail(message, greenMailBean.receivedMessages.last())
    }

}
