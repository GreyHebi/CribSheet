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
 */
class Message private constructor(
    val from: String = "",
    val subject: String,
    val text: String,
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
        attachments: Collection<Attachment> = listOf()
    ) : this(from, listOf(recipient), subject, text, attachments)

    /**
     * @param from        отправитель
     * @param recipients  получатели
     * @param subject     тема сообщения
     * @param text        текст
     * @param attachments вложения
     */
    constructor(
        from: String = "",
        recipients: Collection<String>,
        subject: String,
        text: String,
        attachments: Collection<Attachment> = listOf()
    ) : this(from, subject, text) {
        _recipients.addAll(recipients)
        _attachments.addAll(attachments)
    }

    private val _recipients: MutableSet<String> = mutableSetOf()
    val recipients: Set<String>
        get() = _recipients.toSet()

    fun addRecipient(recipient: String) {
        _recipients.add(recipient)
    }

    private val _attachments: MutableList<Attachment> = mutableListOf()
    val attachments: Set<Attachment>
        get() = _attachments.toSet()

    fun addAttachment(attachment: Attachment) {
        _attachments.add(attachment)
    }

    override fun toString(): String {
        return "Message(from='$from', recipients=$_recipients, subject='$subject', text='$text', attachments=$_attachments)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        return from == other.from
                && subject == other.subject
                && text == other.text
                && _recipients == other._recipients
                && _attachments == other._attachments
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + subject.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + _recipients.hashCode()
        result = 31 * result + _attachments.hashCode()
        return result
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

/** Прикрепить файл */
fun Message.addAttachment(path: Path) : Message {
    addAttachment(File(path))
    return this
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

/** Прикрепить файл */
fun Message.addAttachment(filename: String, data: ByteArray): Message {
    addAttachment(Byte(filename, data))
    return this
}
