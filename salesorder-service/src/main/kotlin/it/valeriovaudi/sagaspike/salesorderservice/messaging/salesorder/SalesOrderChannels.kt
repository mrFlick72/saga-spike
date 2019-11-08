package it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder

import org.springframework.cloud.stream.annotation.Input
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.dsl.MessageChannels
import org.springframework.messaging.SubscribableChannel


interface SalesOrderMessageChannel {

    @Input
    fun createSalesOrderRequestChannel(): SubscribableChannel

}

@Configuration
class SalesOrderMessageChannelConfig {

    @Bean
    fun newSalesOrderRequestChannel() = MessageChannels.flux()

    @Bean
    fun newSalesOrderResponseChannel() = MessageChannels.direct()

    @Bean
    fun salesOrderCompleteChannel() = MessageChannels.flux()

    @Bean
    fun responseChannelAdapter() = MessageChannels.flux()

    @Bean
    fun createSalesOrderResponseChannel() = MessageChannels.flux()

    @Bean
    fun processSalesOrderGoods() = MessageChannels.flux();

    @Bean
    fun rollbackSalesOrderGoods() = MessageChannels.flux();

}