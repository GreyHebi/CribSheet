package ru.hebi.cribSheet.mail.config

import com.icegreen.greenmail.spring.GreenMailBean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class GreenMailConfiguration {
    @Bean
    fun greenMailBean() = GreenMailBean().apply {
        users = listOf(
            "ivan:12345@localhost",
            "alex:09876@localhost"
        )
        hostname = "127.0.0.1"
        portOffset = 3000
        isAutostart = true
        isSmtpProtocol = true
        isPop3Protocol = true
    }
}