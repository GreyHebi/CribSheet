package ru.hebi.cribSheet.mail.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.hebi.cribSheet.mail.decodeFromBase64
import ru.hebi.cribSheet.mail.map
import ru.hebi.cribSheet.mail.service.Byte
import ru.hebi.cribSheet.mail.service.Message
import ru.hebi.cribSheet.mail.service.SenderService
import java.util.*

@RestController
@RequestMapping("/sender")
class SenderController(
    private val senderService: SenderService,
) {

    /**
     * Отправка сообщения
     *
     * @param request сообщение
     *
     * @return 200 - сообщение удачно отправлено;
     *         400 - произошла ошибка при подготовке сообщения к отправке;
     *               пользователь, под которым запущено приложение не авторизован;
     *               прочие ошибки при отправке
     */
    @PostMapping
    fun sendMessage(@RequestBody @Valid request: SendMessageRq): ResponseEntity<Nothing> {
        val result = senderService.send(request.toMessage())
        return if (result) ResponseEntity.ok().build()
               else ResponseEntity.badRequest().build()
    }

}


// DTO ->
/**
 * Тело запроса на оправку сообщения
 *
 * @param recipients  получатели
 * @param subject     тема сообщения
 * @param text        сообщение
 * @param attachments вложенные файлы
 */
data class SendMessageRq(
    @field:NotEmpty val recipients: Set<@NotBlank String>,
    @field:NotBlank val subject: String,
    @field:NotBlank val text: String,
    val attachments: Set<@Valid AttachmentsDto>? = null,
)
fun SendMessageRq.toMessage() = Message(
    recipients = recipients,
    subject = subject,
    text = text,
    attachments = attachments?.map { it.toAttachment() } ?: setOf(),
)

/**
 * Вложение
 *
 * @param filename наименование файла
 * @param data     данные файла в Base64
 */
data class AttachmentsDto(
    @field:NotBlank val filename: String,
    @field:NotBlank val data: String,
)
fun AttachmentsDto.toAttachment() = Byte(filename, decodeFromBase64(data))
