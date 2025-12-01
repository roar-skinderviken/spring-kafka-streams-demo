package no.roar.kafka.streams.config

import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.FraudMetrics
import no.roar.kafka.streams.model.Transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JacksonJsonSerde
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class FraudDetectionTopologyConfig(
    @param:Value($$"${transaction-topic.name}") private val transactionTopic: String,
    @param:Value($$"${fraud-alert-topic.name}") private val fraudAlertTopic: String
) {
    @Bean
    fun defaultTopology(builder: StreamsBuilder): KStream<String, Transaction> {
        val stream: KStream<String, Transaction> = builder.stream(
            transactionTopic
            //Consumed.with(Serdes.String(), JsonSerde(Transaction::class.java)) only required when KafkaStreamsConfiguration is missing
        )

        stream
            .groupBy(
                { _, transaction -> transaction.accountId },
                Grouped.with(Serdes.String(), JacksonJsonSerde(Transaction::class.java))
            )
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .aggregate(
                { FraudMetrics() },
                { _, transaction, metrics ->
                    FraudMetrics(
                        metrics.transactionCount + 1,
                        metrics.totalAmount + transaction.amount
                    )
                },
                Materialized.with(Serdes.String(), JacksonJsonSerde(FraudMetrics::class.java))
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()
            .filter { _, metrics -> metrics.shouldAlert }
            .map { windowedKey, metrics ->
                KeyValue.pair(
                    windowedKey.key(),
                    FraudAlert(
                        accountId = windowedKey.key(),
                        totalAmount = metrics.totalAmount
                    )
                )
            }.to(
                fraudAlertTopic,
                Produced.with(Serdes.String(), JacksonJsonSerde(FraudAlert::class.java))
            )

        return stream
    }
}