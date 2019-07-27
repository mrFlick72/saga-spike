package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface CatalogRepository : ReactiveMongoRepository<Catalog, String>
