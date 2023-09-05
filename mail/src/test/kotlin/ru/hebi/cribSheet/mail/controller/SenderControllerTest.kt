package ru.hebi.cribSheet.mail.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.hebi.cribSheet.mail.*
import ru.hebi.cribSheet.mail.encodeToBase64ToString
import ru.hebi.cribSheet.mail.map
import ru.hebi.cribSheet.mail.service.Byte
import ru.hebi.cribSheet.mail.service.Message
import ru.hebi.cribSheet.mail.service.SenderService
import java.util.*
import java.util.stream.Stream

val RESPONSE_OK : ResponseEntity<Nothing> = ResponseEntity.ok().build()
val RESPONSE_BAD_REQUEST : ResponseEntity<Nothing> = ResponseEntity.badRequest().build()

@DisplayName("(Unit) API отправка сообщений")
class SenderControllerUnitTest : UnitTest() {
    private val senderService: SenderService = mockk()
    private val target: SenderController = SenderController(senderService)

    @DisplayName("Отправка сообщений")
    @ParameterizedTest
    @MethodSource("testData")
    fun `successful send`(recipients: Set<String>, subject: String, text: String, attachment: Set<AttachmentForTest>?) {
        val message = Message(
            recipients = recipients,
            subject = subject,
            text = text,
            attachments = attachment?.map{ it.toModel() }?.toSet() ?: setOf(),
        )
        every { senderService.send(ofType(Message::class)) } returns true

        val request = SendMessageRq(
            recipients = recipients,
            subject = subject,
            text = text,
            attachments = attachment?.map{ it.toDto() },
        )
        val resp = target.sendMessage(request)

        assertEquals(RESPONSE_OK, resp)
        verify(exactly = 1) { senderService.send(eq(message)) }
    }

    @DisplayName("Не удачная отправка сообщений")
    @Test
    fun `unsuccessful send`() {
        every { senderService.send(any()) } returns false

        val request = SendMessageRq(
            recipients = setOf("rec"),
            subject = "темка",
            text = "какой-то текст",
        )
        val resp = target.sendMessage(request)

        assertEquals(RESPONSE_BAD_REQUEST, resp)
    }

    companion object {
        @JvmStatic
        private fun testData() = Stream.of(
            arguments(
                setOf("recip@localhost"), "темка", "Сообщение", null
            ),
            arguments(
                setOf("recip@localhost"), "темка", "Сообщение",
                setOf(AttachmentForTest("foo.txt", "АБВ"))
            ),
            arguments(
                setOf("recip1@localhost", "recip2@localhost"), "темка", "Сообщение",
                setOf(AttachmentForTest("foo.txt", "АБВ"), AttachmentForTest("bra.txt", "ГДЕ"))
            ),
        )
    }
}

private const val RECIPIENT = "alex@localhost"
private const val ANOTHER_RECIPIENT = "sergey@localhost"

@DisplayName("(Integration) API отправка сообщений")
class SenderControllerIntegrationTest : IntegrationTest() {

    val objectMapper = ObjectMapper()
    @Autowired lateinit var webMvc: MockMvc

    @BeforeEach fun before() {
        launchGreenMail()
    }

    @AfterEach fun after() {
        stopGreenMail()
    }

    @DisplayName("Отправка сообщения")
    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `successful send`(testName: String, testCase: SendMailCase) {
        val request = testCase.toDto()

        webMvc.perform(
            post("/sender")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
            )
            .andDo(print()) //MockMvcResultHandlers, а не стандартный котлиновский, для печати в лог
            .andExpect(status().isOk)

        val expected = testCase.toModel()
        assertMail(expected, greenMailBean.receivedMessages.last())
    }

    @Test
    fun `validate fail - not recipient`() {
        val request = SendMessageRq(
            recipients = setOf(),
            subject = "Некому послать",
            text = "А нужно?",
        )

        webMvc.perform(
            post("/sender")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andDo(print()) //MockMvcResultHandlers, а не стандартный котлиновский, для печати в лог
            .andExpect(status().isBadRequest)
    }

    companion object {
        @JvmStatic
        fun `successful send`(): Stream<Arguments> = Stream.of(
            arguments(
                "Простое письмо одному получателю",
                SendMailCase(RECIPIENT, "Какая-то темка", "Типа длинный текст")
            ),
            arguments(
                "Простое письмо нескольким получателям",
                SendMailCase(setOf(RECIPIENT, ANOTHER_RECIPIENT), "Еще какая-то темка", "Очень длинный текст, адресованный первому получателю. Второй только контролирует.")
            ),
            arguments(
                "Письмо с небольшим вложением",
                SendMailCase(RECIPIENT, "Письмо с файлом", "Прикладываю файл", setOf(AttachmentForTest("test.txt", "Текст вложенного файла")))
            )
        )
    }

}

class AttachmentForTest(
    val filename: String,
    val data: String,
)
fun AttachmentForTest.toDto() = AttachmentsDto(filename, encodeToBase64ToString(data))
fun AttachmentForTest.toModel() = Byte(filename, data.toByteArray())

class SendMailCase(
    val recipients: Set<String>,
    val subject: String,
    val text: String,
    val attachments: Set<AttachmentForTest> = setOf(),
) {
    constructor(
        recipient: String,
        subject: String,
        text: String,
        attachments: Set<AttachmentForTest> = setOf(),
    ) : this (setOf(recipient), subject, text, attachments)
}
fun SendMailCase.toDto() = SendMessageRq(recipients, subject, text, attachments.map { it.toDto() })
fun SendMailCase.toModel() = Message(
    recipients = recipients,
    subject = subject,
    text = text,
    attachments = attachments.map { it.toModel() }
)