package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.messaging.MessageHeaders

object MessageUtils {
    fun copyHeaders(headers: MessageHeaders) = headers.toMap()
}