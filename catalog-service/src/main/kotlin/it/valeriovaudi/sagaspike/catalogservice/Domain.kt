package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.data.annotation.Id
import java.math.BigDecimal

data class Goods(val barcode: String, val name: String)

data class Price(val price: BigDecimal, val currency: String)

data class Catalog(@Id val id: String, val goods: List<GoodsWithPrice>)

data class GoodsWithPrice(val goods: Goods, val price: Price)

