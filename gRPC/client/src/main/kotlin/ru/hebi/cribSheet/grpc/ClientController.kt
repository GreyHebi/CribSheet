package ru.hebi.cribSheet.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestController
@RequestMapping("/client")
class ClientController(
    private val grpcService: GrpcService,
) {

    @GetMapping("/call")
    fun call(@RequestParam txt: String) = grpcService.call(txt)

    @GetMapping("/getStream")
    fun getStream(@RequestParam txt: String) = grpcService.getStream(txt)

    @PostMapping("/putStream")
    fun putStream(@RequestBody people: List<String>) = grpcService.putStream(people)

    @PostMapping("/streamCall")
    fun streamCall(@RequestBody people: List<String>) = grpcService.streamCall(people)

}

@RestControllerAdvice
class Advicer {

    val log: Logger = LoggerFactory.getLogger(Advicer::class.java)

    @ExceptionHandler(StatusRuntimeException::class)
    fun grpcExceptionHandle(e : StatusRuntimeException): ResponseEntity<String> {
        log.error("Произошла ошибка при обращении во второй сервис", e)
        return when(e.status.code) {
            Status.INVALID_ARGUMENT.code -> ResponseEntity.badRequest().body(e.status.description)
            else -> ResponseEntity.internalServerError().body(e.message)
        }
    }

}