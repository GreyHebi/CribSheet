package ru.hebi.cribSheet.grpc

import io.grpc.Status
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.hebi.cribSheet.grpc.dto.Request
import ru.hebi.cribSheet.grpc.dto.Response
import ru.hebi.cribSheet.grpc.dto.response
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("(Integration) API клиента")
class ClientControllerIntegrationTest : IntegrationTest() {

    @Autowired lateinit var webMvc: MockMvc

    @DisplayName("Синхронная работа клиент-сервер")
    @Test
    fun call() {
        val param = "test-log"
        webMvc.perform(
            get("/client/call?txt=$param")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().json("[\"Hello\", \"$param\", \"!\"]"))
    }

    @DisplayName("Неверный логин пользователя")
    @Test
    fun `call - wrong login`() {
        val param = "123Error"
        webMvc.perform(
            get("/client/call?txt=$param")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Логин не соответствует шаблону: $REGEX"))
    }


    @DisplayName("Потоковая передача со стороны сервера")
    @Test
    fun getStream() {
        val param = "test-log"
        webMvc.perform(
            get("/client/getStream?txt=$param")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().json("[\"Hello\", \"$param\", \"!\"]"))
    }

    @DisplayName("Неверный логин при получении потока данных")
    @Test
    fun `getStream - wrong login`() {
        val param = "123Error"
        webMvc.perform(
            get("/client/getStream?txt=$param")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Логин не соответствует шаблону: $REGEX"))
    }


    @DisplayName("Потоковая передача со стороны клиента")
    @Test
    fun putStream() {
        webMvc.perform(
            post("/client/putStream")
                .contentType(APPLICATION_JSON)
                .content("[\"abcd\", \"efgh\", \"ijkl\"]")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().json("[\"Hello\", \"abcd\", \"efgh\", \"ijkl\", \"!\"]"))
    }

    @DisplayName("Неверный логин при потоковой передаче данных")
    @ParameterizedTest
    @MethodSource("wrongLogins")
    fun `putStream - wrong login`(params: List<String>) {
        webMvc.perform(
            post("/client/putStream")
                .contentType(APPLICATION_JSON)
                .content(params.joinToString("\",\"", "[\"", "\"]"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Логин не соответствует шаблону: $REGEX"))
    }



    @DisplayName("Потоковый обмен данными")
    @Test
    fun streamCall() {
        webMvc.perform(
            post("/client/streamCall")
                .contentType(APPLICATION_JSON)
                .content("[\"abcd\", \"efgh\", \"ijkl\"]")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().json("[\"1\",\"abcd\",  \"2\", \"efgh\", \"3\", \"ijkl\"]"))
    }

    @DisplayName("Неверный логин при потоковом обмене данных")
    @ParameterizedTest
    @MethodSource("wrongLogins")
    fun `streamCall - wrong login`(params: List<String>) {
        webMvc.perform(
            post("/client/streamCall")
                .contentType(APPLICATION_JSON)
                .content(params.joinToString("\",\"", "[\"", "\"]"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Логин не соответствует шаблону: $REGEX"))
    }

    fun wrongLogins(): Stream<Arguments> = Stream.of(
        arguments( listOf("sml", "normal1", "normal2", "normal3") ),
        arguments( listOf("normal1", "sml", "normal2", "normal3") ),
    )
}


@GrpcService
class TestService : SendServiceGrpc.SendServiceImplBase() {

    private val log = LoggerFactory.getLogger("SomeTestRemoteService")

    override fun call(request: Request?, responseObserver: StreamObserver<Response>) {
        val error = validateRequest(request)
        if (error !== null) {
            responseObserver.onError(error)
            log.error("При обработке запроса произошла ошибка", error)
            return
        }

        log.info("Идет обработка запроса ${request!!.rqUid}")
        responseObserver.onNext(response {
            this.msgs.addAll(listOf("Hello", request.login, "!"))
        })
        responseObserver.onCompleted()
        log.info("Обработка запроса ${request.rqUid} завершена")
    }

    override fun getStream(request: Request?, responseObserver: StreamObserver<Response>) {
        val error = validateRequest(request)
        if (error !== null) {
            responseObserver.onError(error)
            log.error("При обработке запроса произошла ошибка", error)
            return
        }

        log.info("Идет обработка запроса ${request!!.rqUid}")
        responseObserver.onNext(response {
            this.msgs.addAll(listOf("Hello"))
        })
        responseObserver.onNext(response {
            this.msgs.addAll(listOf(request.login))
        })
        responseObserver.onNext(response {
            this.msgs.addAll(listOf("!"))
        })
        responseObserver.onCompleted()
        log.info("Обработка запроса ${request.rqUid} завершена")
    }

    override fun putStream(responseObserver: StreamObserver<Response>): StreamObserver<Request> {
        log.info("Началась обработка потокового запроса")
        return object : StreamObserver<Request> {
            private val data = mutableListOf<String>()

            override fun onNext(request: Request?) {
                val error = validateRequest(request)
                if (error != null) {
                    responseObserver.onError(error)
                    log.warn("Пришли некорректные данные: $request")
                    return
                }

                log.info("Началась обработка данных ${request!!.rqUid}")
                data.add(request.login)
            }

            override fun onError(t: Throwable) {
                throw t
            }

            override fun onCompleted() {
                responseObserver.onNext(response {
                    data.add(0, "Hello")
                    data.add("!")
                    this.msgs.addAll(data)
                })
                responseObserver.onCompleted()
                log.info("Потоковый запрос успешно обработан. Отправлены $data")
            }
        }
    }

    override fun streamCall(responseObserver: StreamObserver<Response>): StreamObserver<Request> {
        log.info("Начался обмен потоками данных")
        return object : StreamObserver<Request> {
            val counter = AtomicInteger(0)

            override fun onNext(request: Request?) {
                val error = validateRequest(request)
                if (error != null) {
                    responseObserver.onError(error)
                    log.warn("Пришли некорректные данные: $request")
                    return
                }

                log.info("Началась обработка данных ${request!!.rqUid}")
                val i = counter.incrementAndGet()
                responseObserver.onNext(response { this.msgs.addAll(listOf("$i", request.login)) })
            }

            override fun onError(t: Throwable) {
                throw t
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
                log.info("Обмен данными завершен")
            }

        }
    }
}


private fun validateRequest(request: Request?) : Throwable? {
    if (request == null) {
        return Status.DATA_LOSS
            .withDescription("Отсутствует тело запроса")
            .asException()
    }

    if (!pattern.matches(request.login)) {
        return Status.INVALID_ARGUMENT
            .withDescription("Логин не соответствует шаблону: $REGEX")
            .asException()
    }

    return null
}

private const val REGEX = "^[A-Za-z][A-Za-z0-9.-]{3,15}\$"
private val pattern = Regex(REGEX)