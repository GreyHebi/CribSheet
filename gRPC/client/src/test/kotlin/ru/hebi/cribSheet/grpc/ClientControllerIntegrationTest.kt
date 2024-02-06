package ru.hebi.cribSheet.grpc

import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.hebi.cribSheet.grpc.dto.Request
import ru.hebi.cribSheet.grpc.dto.Response
import ru.hebi.cribSheet.grpc.dto.response


@DisplayName("(Integration) API клиента")
class ClientControllerIntegrationTest : IntegrationTest() {

    @Autowired lateinit var webMvc: MockMvc

    @DisplayName("Синхронная работа клиент-сервер")
    @Test
    fun call() {
        val param = "test log"
        webMvc.perform(
            get("/client?txt=$param")
        )
            .andDo(MockMvcResultHandlers.print()) //MockMvcResultHandlers, а не стандартный котлиновский, для печати в лог
            .andExpect(status().isOk)
            .andExpect(content().json("[\"Hello\", \"$param\", \"!\"]"))
    }
}


@GrpcService
class TestService : SendServiceGrpc.SendServiceImplBase() {

    override fun call(request: Request?, responseObserver: StreamObserver<Response>) {
        responseObserver.onNext(response {
            this.msgs.addAll(listOf("Hello", request?.login ?: "<empty>", "!"))
        })
        responseObserver.onCompleted()
    }
}
