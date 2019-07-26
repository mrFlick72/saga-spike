package it.valeriovaudi.sagaspike.webapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.router
import java.time.Duration


@SpringBootApplication
class WebappApplication {

    @Bean
    fun routes() =
            router {
                GET("/hello/{name}")
                {
                    ok().body(fromObject("hello ${it.pathVariable("name")}"))
                            .delayElement(Duration.ofSeconds(60))
                }
            }
}

fun main(args: Array<String>) {
    runApplication<WebappApplication>(*args)
}