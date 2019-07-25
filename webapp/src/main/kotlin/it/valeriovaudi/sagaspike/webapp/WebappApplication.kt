package it.valeriovaudi.sagaspike.webapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.time.LocalDateTime


@EnableScheduling
@SpringBootApplication
class WebappApplication

fun main(args: Array<String>) {
    runApplication<WebappApplication>(*args)
}

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/notify")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/job").withSockJS()
    }
}

@Component
class Schedule(private val simpMessagingTemplate: SimpMessagingTemplate) {

    @Scheduled(initialDelay = 5, fixedRate = 5000)
    fun message() {
        val now = LocalDateTime.now()
        val message = "This is a special message for you at time: $now"
        println(message)
        simpMessagingTemplate.convertAndSend("/notify/job", message)
    }
}