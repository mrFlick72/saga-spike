package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.messaging.MessageHeaders

object MessageUtils {
    fun copyHeaders(headers: MessageHeaders) = headers.toMap()
}