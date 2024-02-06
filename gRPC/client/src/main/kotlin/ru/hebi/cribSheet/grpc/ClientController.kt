package ru.hebi.cribSheet.grpc

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/client")
class ClientController(
    private val grpcService: GrpcService,
) {

    @GetMapping
    fun call(@RequestParam txt: String) : List<String> {
        return grpcService.call(txt)
    }

}