package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel

interface CatalogMessageChannel {

    @Input
    fun goodsPricingRequestChannel(): SubscribableChannel

    @Output
    fun goodsPricingResponseChannel(): MessageChannel
}
