package ru.hebi.cribSheet.grpc

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.hebi.cribSheet.grpc.dto.response

@DisplayName("(Unit) Сервис по работе с удаленным сервисом по gRPC")
class GrpcServiceTest : UnitTest() {

    private val sendServiceSync : SendServiceGrpc.SendServiceBlockingStub = mockk()
    private val sendServiceAsync : SendServiceGrpc.SendServiceStub = mockk()
    private val target = GrpcService()

    init {
        target.sendServiceSync = sendServiceSync
        target.sendServiceAsync = sendServiceAsync
    }

    @DisplayName("Синхронная работа клиент-сервер")
    @Test
    fun call() {
        val param = "log"
        every { sendServiceSync.call(any()) } returns response { this.msgs.addAll(listOf("Hello", param, "!")) }

        val actual = target.call(param)

        val expected = listOf("Hello", param, "!")
        assertEquals(expected, actual)
    }

    @DisplayName("Потоковая передача со стороны сервера")
    @Test
    fun getStream() {
        val param = "log"
        every { sendServiceSync.getStream(any()) } returns listOf(
            response { this.msgs.addAll(listOf("Hello")) },
            response { this.msgs.addAll(listOf(param, "!")) }
        ).iterator()

        val actual = target.getStream(param)

        val expected = listOf("Hello", param, "!")
        assertEquals(expected, actual)
    }

    @Test
    @Disabled("Пока не удалось продумать мокировать взаимодействие с объектом ответа")
    fun putStream() {
        //логика
    }


}