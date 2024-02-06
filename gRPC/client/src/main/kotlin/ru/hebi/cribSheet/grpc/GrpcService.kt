package ru.hebi.cribSheet.grpc

import io.grpc.*
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.hebi.cribSheet.grpc.dto.request
import ru.hebi.cribSheet.grpc.dto.uUID
import java.time.Instant
import java.util.*

@Service
class GrpcService{

    @GrpcClient(value = "sendService", interceptors = [LogInterceptor::class])
    lateinit var sendService: SendServiceGrpc.SendServiceBlockingStub

    fun call(login : String) : List<String> {
        val rq = request {
            this.rqUid = uUID { value = UUID.randomUUID().toString() }
            this.rqTm  = now()
            this.login = login
        }

        val resp = sendService.call(rq)
        return resp.msgsList
    }

}


fun timestamp(instant: Instant) = com.google.protobuf.timestamp {
    this.seconds = instant.epochSecond
    this.nanos   = instant.nano
}
fun now() = timestamp(Instant.now())

class LogInterceptor : ClientInterceptor {

    private val log = LoggerFactory.getLogger(LogInterceptor::class.java)

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        log.info("Отправляется запрос $method")
        return next.newCall(method, callOptions)
    }

}