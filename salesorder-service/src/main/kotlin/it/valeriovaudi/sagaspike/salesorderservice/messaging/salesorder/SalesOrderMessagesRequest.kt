package it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder

import it.valeriovaudi.sagaspike.salesorderservice.CustomerRepresentation
import java.util.*

data class NewSalesOrderRequest(var salesOrderId: String? = null, var catalogId: String, var customer: CustomerRepresentation, var goods: List<GoodsRequest> = emptyList()) {
    constructor() : this(UUID.randomUUID().toString(), "", CustomerRepresentation("", ""), emptyList())
}

data class GoodsRequest(var catalogId: String, var barcode: String, var quantity: Int) {
    constructor() : this(catalogId = "", barcode = "", quantity = 0)
}