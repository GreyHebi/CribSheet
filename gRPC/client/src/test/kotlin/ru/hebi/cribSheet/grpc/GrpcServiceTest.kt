package ru.hebi.cribSheet.grpc

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.hebi.cribSheet.grpc.dto.response

@DisplayName("(Unit) Сервис по работе с удаленным сервисом по gRPC")
class GrpcServiceTest : UnitTest() {

    private val sendService : SendServiceGrpc.SendServiceBlockingStub = mockk()
    private val target = GrpcService()

    init {
        target.sendService = sendService
    }

    @DisplayName("Синхронная работа клиент-сервер")
    @Test
    fun call() {
        val param = "log"
        every { sendService.call(any()) } returns response { this.msgs.addAll(listOf("Hello", param, "!")) }

        val actual = target.call(param)

        val expected = listOf("Hello", param, "!")
        Assertions.assertEquals(expected, actual)
    }
}