package ru.hebi.cribSheet.mail.service

import java.nio.file.Path

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
    fun send(message : Message): Boolean
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
    val attachment: List<Path> = listOf()
)