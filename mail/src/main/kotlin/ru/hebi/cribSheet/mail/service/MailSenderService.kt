package ru.hebi.cribSheet.mail.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import jakarta.mail.MessagingException
import java.lang.Exception


private val logger = LoggerFactory.getLogger("ru.hebi.cribSheet.mail.service.MailSender")

@Component
class MailSenderService(
    private val emailSender: JavaMailSender,
) : SenderService {

    override fun send(message: Message): Boolean {
        try {
            emailSender.send(message)
            logger.info("Сообщение отправлено")
            return true
        } catch (e: Exception) {
            when (e) {
                is MailAuthenticationException -> logger.error("Ошибка при аутентификации", e)
                is MailSendException, is MessagingException -> logger.warn("Произошла ошибка при отправке сообщения", e)
                else -> throw e
            }
        }
        return false
    }

}


/**
 * @throws MailAuthenticationException при провале аутентификации
 * @throws MailSendException           при провале отправки сообщения
 * @throws MessagingException          при ошибке в маппинге
 */
private fun JavaMailSender.send(message: Message) {
    val helper = MimeMessageHelper(createMimeMessage(), false)
    helper.setTo(message.recipient)
    helper.setSubject(message.subject)
    helper.setText(message.text)

    if (message.from.isNotBlank()) {
        helper.setFrom(message.from)
    }

    message.attachment
        .map { FileSystemResource(it) }
        .forEach { helper.addAttachment(it.filename, it) }

    send(helper.mimeMessage)
}