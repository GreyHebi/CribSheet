package ru.hebi.cribSheet.mail

import com.icegreen.greenmail.spring.GreenMailBean
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.StringUtils
import ru.hebi.cribSheet.mail.config.GreenMailConfiguration
import ru.hebi.cribSheet.mail.service.Attachment
import ru.hebi.cribSheet.mail.service.Message
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
@Import(GreenMailConfiguration::class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTest : TestSupport() {
    @Autowired
    lateinit var greenMailBean: GreenMailBean

    protected fun launchGreenMail() {
        if (!greenMailBean.isStarted) greenMailBean.afterPropertiesSet()
        assertTrue(greenMailBean.isStarted)
    }
    protected fun stopGreenMail() {
        //таким нехитрым образом ребутаем smtp-сервер
        greenMailBean.stop()
    }
}

@Tag("unit-test")
@ExtendWith(SpringExtension::class)
abstract class UnitTest : TestSupport()


fun assertMail(expected: Message, actual: MimeMessage) = assertAll("mail",
    {
        if (expected.from.isBlank()) assertNull(actual.from)
        else assertEquals(expected.from, actual.from.first().toString())
    },
    { assertEquals(expected.recipients, actual.allRecipients.map { it.toString() }.toSet()) },
    { assertEquals(expected.subject, actual.subject) },
    {
        if (expected.attachments.isEmpty()) assertEqualsSimpleContent(expected.text, actual)
        else assertEqualsContent(expected.text, actual)
    },
    {
        if (expected.attachments.isNotEmpty()) expected.attachments.forEach { assertEqualsAttachment(it, actual) }
    }
)

/** Внутренний тестовый класс для байтовых вложений */
data class ByteAttachment(
    val filename: String,
    val data: ByteArray,
)

fun assertEqualsContent(expected: String, actual: MimeMessage) {
    /* Т.к. сообщение передается в Base64 и бывает MultiPart (когда с вложениями) проще удостовериться,
       что ожидаемый текст присутствует(!) в теле письма */
    val expectedInBase64 = encodeToBase64ToString(expected.toByteArray())
    val message = actual.contentInString()

    /* есть переносы строк в теле сообщения, из-за чего падали тесты при длинных отправляемых сообщениях
       добавил сюда, чтоб не портить логирование ошибки */
    assertTrue(message.replace("\\s+".toRegex(), "").contains(expectedInBase64)) {
        "В сообщении '$message' отсутствует ожидаемый текст сообщения '$expected' (в Base64: '$expectedInBase64')"
    }
}

fun assertEqualsAttachment(expected: Attachment, actual: MimeMessage) {
    /* Аналогично с методом выше: проще удостовериться, что вложенный файл присутствует в теле письма,
       чем его выковыривать */
    val expectedBytes = expected.source().inputStream.readAllBytes()
    val expectedInBase64 = encodeToBase64ToString(expectedBytes)
    val message = actual.contentInString()

    assertAll(
        "attachment",
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

//Проверка простых писем, без вложений
fun assertEqualsSimpleContent(expected: String, actual: MimeMessage) {
    val rm = actual.contentInString().replace("\\s+".toRegex(), "")
    val message = decodeFromBase64ToString(rm)
    assertEquals(expected, message)
}

fun MimeMessage.contentInString() = String(rawInputStream.readAllBytes())