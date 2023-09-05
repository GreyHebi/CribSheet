package ru.hebi.cribSheet.mail.service

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.PathResource
import ru.hebi.cribSheet.mail.hash
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Сервис для отправки сообщений
 */
interface SenderService {
    /**
     * Отправка сообщений
     *
     * @param message сообщение
     *
     * @return true - при удачной отправке сообщения
     */
    fun send(message: Message): Boolean
}

/**
 * Сообщение
 *
 * @param from        отправитель
 * @param recipients  получатели
 * @param subject     тема сообщения
 * @param text        текст
 * @param attachments вложения
 *
 */
data class Message (
    val from: String = "",
    val recipients: Set<String>,
    val subject: String,
    val text: String,
    val attachments: Set<Attachment> = setOf(),
) {

    /**
     * @param from        отправитель
     * @param recipient   получатель
     * @param subject     тема сообщения
     * @param text        текст
     * @param attachments вложения
     */
    constructor(
        from: String = "",
        recipient: String,
        subject: String,
        text: String,
        attachments: Collection<Attachment> = setOf()
    ) : this(from, setOf(recipient), subject, text, attachments.toSet())

    override fun toString() = "Message(from='$from', recipients=$recipients, subject='$subject', text='$text', attachments=$attachments)"
}


/**
 * Вложения
 */
interface Attachment {
    /**
     * @return наименование файла
     */
    fun filename(): String

    /**
     * @return данные файла
     */
    fun source(): InputStreamSource
}

/**
 * @param path путь до файла
 */
data class File(private val path: Path) : Attachment {
    override fun filename() = path.name
    override fun source() = PathResource(path)
    override fun toString() = "FileAttachment(filename='${filename()}')"
}
fun attachment(path: Path) = File(path) as Attachment

/**
 * @param filename наименование файла
 * @param data     данные файла
 */
data class Byte(private val filename: String, private val data: ByteArray) : Attachment {
    override fun filename() = filename
    override fun source() = ByteArrayResource(data)
    override fun toString() = "ByteAttachment(filename='$filename')"
    override fun hashCode() = hash(filename, data)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Byte
        return filename == other.filename
                && data contentEquals other.data
    }

}
fun attachment(filename: String, data: ByteArray) = Byte(filename, data) as Attachment
