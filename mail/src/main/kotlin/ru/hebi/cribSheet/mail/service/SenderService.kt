package ru.hebi.cribSheet.mail.service

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.PathResource
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
 * @param attachment вложения
 * @param from       отправитель
 * @param recipient  получатель
 * @param subject    тема
 * @param text       сообщение
 */
data class Message(
    val subject: String,
    val recipient: String,
    val text: String,
    val from: String = "",
    val attachment: MutableList<Attachment> = mutableListOf(),
) {

    /** Прикрепить файл */
    fun addAttachment(path: Path): Message {
        attachment.add(File(path))
        return this
    }

    /** Прикрепить файл */
    fun addAttachment(filename: String, data: ByteArray): Message {
        attachment.add(Byte(filename, data))
        return this
    }
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
class File(private val path: Path) : Attachment {
    override fun filename() = path.name

    override fun source() = PathResource(path)

    override fun toString(): String {
        return "FileAttachment(filename='${filename()}')"
    }

}

/**
 * @param filename наименование файла
 * @param data     данные файла
 */
class Byte(private val filename: String, private val data: ByteArray) : Attachment {
    override fun filename() = filename

    override fun source() = ByteArrayResource(data)

    override fun toString(): String {
        return "ByteAttachment(filename='$filename')"
    }

}
