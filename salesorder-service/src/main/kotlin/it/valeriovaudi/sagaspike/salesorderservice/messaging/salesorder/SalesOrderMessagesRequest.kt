package it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder

import it.valeriovaudi.sagaspike.salesorderservice.CustomerRepresentation
import java.util.*

data class NewSalesOrderRequest(var salesOrderId: String? = null, var customer: CustomerRepresentation, var goods: List<GoodsRequest> = emptyList()) {
    constructor() : this(UUID.randomUUID().toString(), CustomerRepresentation("", ""), emptyList())
}

data class GoodsRequest(var barcode: String, var quantity: Int) {
    constructor() : this(barcode = "", quantity = 0)
}