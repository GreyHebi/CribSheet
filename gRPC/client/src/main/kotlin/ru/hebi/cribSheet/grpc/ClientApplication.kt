package ru.hebi.cribSheet.grpc

import io.grpc.*
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.hebi.cribSheet.grpc.dto.*
import java.time.Instant

@SpringBootApplication
class ClientApplication

fun main(args: Array<String>) {
    runApplication<ClientApplication>(*args)
}




@RestController
@RequestMapping("/api")
class Controller(
    private val grpcService: GrpcService,
) {

    @GetMapping("/sync")
    fun sync(@RequestParam login: String) {
        grpcService.sendSync(login)
    }
}




@Service
class GrpcService {

    @GrpcClient(value = "test-service", interceptors = [LogClientInterceptor::class])
    lateinit var testService: TestServiceGrpc.TestServiceBlockingStub

    fun sendSync(login: String) {
        val rq = request {
            this.rqUid = uUID { value = java.util.UUID.randomUUID().toString() }
            this.rqTm  = now()
            this.login = login
        }

        val resp: Response
        try {
            resp = testService.call(rq)
            log.info("$resp")
        } catch (e : Exception) {
            val status = Status.fromThrowable(e)
            log.error("${status.code}: ${status.description}")
        }
    }
}

fun timestamp(instant: Instant) = com.google.protobuf.timestamp {
    this.seconds = instant.epochSecond
    this.nanos   = instant.nano
}
fun now() = timestamp(Instant.now())

private val log = LoggerFactory.getLogger("client-grpc")

class LogClientInterceptor  : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        log.info("Отправляется запрос $method")
        return next.newCall(method, callOptions)
    }
}
