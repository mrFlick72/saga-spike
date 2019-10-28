package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.messaging.MessageHeaders

object MessageUtils {
    fun copyHeaders(headers: MessageHeaders) = headers.toMap()
}