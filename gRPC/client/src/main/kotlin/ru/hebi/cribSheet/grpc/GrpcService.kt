package ru.hebi.cribSheet.grpc

import io.grpc.*
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.client.inject.GrpcClient
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.hebi.cribSheet.grpc.dto.Response
import ru.hebi.cribSheet.grpc.dto.request
import ru.hebi.cribSheet.grpc.dto.uUID
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


private val log = LoggerFactory.getLogger(GrpcService::class.java)

@Service
class GrpcService{

    // -- Синхронное взаимодействие

    @GrpcClient(value = "sendService")
    lateinit var sendServiceSync: SendServiceGrpc.SendServiceBlockingStub

    fun call(login : String) : List<String> {
        val rq = request {
            this.rqUid = uUID { value = UUID.randomUUID().toString() }
            this.rqTm  = now()
            this.login = login
        }

        val resp = sendServiceSync.call(rq)
        return resp.msgsList
    }

    fun getStream(login: String): List<String> {
        val rq = request {
            this.rqUid = uUID { value = UUID.randomUUID().toString() }
            this.rqTm = now()
            this.login = login
        }

        val response = sendServiceSync.getStream(rq)

        return response.asSequence()
            .flatMap { it.msgsList }
            .toList()
    }


    // -- Асинхронное взаимодействие

    @GrpcClient(value = "sendService")
    lateinit var sendServiceAsync: SendServiceGrpc.SendServiceStub

    fun putStream(people: List<String>) : List<String> {
        log.info("Отправка потоком запроса $people")
        val response = PutStreamResponse()
        val request = sendServiceAsync.putStream(response)

        for (person in people) {
            if (response.isCancelled) break

            val rq = request {
                this.rqUid = uUID { value = UUID.randomUUID().toString() }
                this.rqTm  = now()
                this.login = person
            }
            request.onNext(rq)
        }
        request.onCompleted()
        return response.get()
    }

    //Логика метода: отправили 1й запрос -> подождали ответ -> обработали ответ -> перешли к следующему запросу
    fun streamCall(people: List<String>) : List<String> {
        val response = StreamCallResponse()
        val request = sendServiceAsync.streamCall(response)

        val result = mutableListOf<String>()
        //необходимо после отправки логина дождаться и обработать ответ и только после этого отправлять следующий логин
        for (person in people) {
            response.resetSignal()
            //отправляем запрос
            val rq = request {
                this.rqUid = uUID { value = UUID.randomUUID().toString() }
                this.rqTm  = now()
                this.login = person
            }
            request.onNext(rq)
            result.addAll(response.next().msgsList)
        }

        request.onCompleted()
        return result.toList()
    }

}


fun timestamp(instant: Instant) = com.google.protobuf.timestamp {
    this.seconds = instant.epochSecond
    this.nanos   = instant.nano
}
fun now() = timestamp(Instant.now())

class PutStreamResponse : StreamObserver<Response>, Future<List<String>> {

    private val signal: CountDownLatch = CountDownLatch(1)
    private val result: MutableList<String> = mutableListOf()
    private var error: Throwable? = null

    override fun onNext(response: Response?) {
        result.addAll(response!!.msgsList)
        log.info("Пришел ответ $response")
    }

    override fun onError(t: Throwable) {
        error = t
        signal.countDown()
        log.warn("Пришла ошибка")
    }

    override fun onCompleted() {
        signal.countDown()
        log.info("Все ответы получены")
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        throw IllegalStateException()
    }

    override fun get(): List<String> {
        log.info("Начали ожидать ответ")
        signal.await()
        if (error != null) {
            throw error as Throwable
        } else {
            return result.toList()
        }
    }

    override fun get(timeout: Long, unit: TimeUnit): List<String> {
        signal.await(timeout, unit) //при таймауте возвращает false, может в этом случае возвращать пустой список? или бросать тайм-аут-эксепшн
        if (error != null) {
            throw error as Throwable
        } else {
            return result.toList()
        }
    }

    override fun isCancelled() = signal.count == 0L && error != null
    override fun isDone() = signal.count == 0L && error == null
}

class StreamCallResponse : StreamObserver<Response> {
    private var signal : CountDownLatch = CountDownLatch(0)
    private var error : Throwable? = null
    private var next : Response? = null

    override fun onNext(response: Response?) {
        signal.countDown()
        next = response
    }

    override fun onError(t: Throwable) {
        signal.countDown()
        error = t
    }

    override fun onCompleted() {
        log.info("Обмен данными завершен")
    }

    fun next() : Response {
        signal.await()
        if (error != null)
            throw error as Throwable
        else
            return next!!
    }

    fun resetSignal() {
        signal = CountDownLatch(1)
    }
}

@GrpcGlobalClientInterceptor
class LogInterceptor : ClientInterceptor {

    private val log = LoggerFactory.getLogger(LogInterceptor::class.java)

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel
    ) = object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

        override fun sendMessage(message: ReqT) {
            log.info("Отправляется сообщение ->\n{}", message)
            super.sendMessage(message)
        }

        override fun start(responseListener: Listener<RespT>?, headers: Metadata?) {
            super.start(object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                override fun onMessage(message: RespT) {
                    log.info("Пришло сообщение <-\n{}", message)
                    super.onMessage(message)
                }
            }, headers)
        }
    }

}
