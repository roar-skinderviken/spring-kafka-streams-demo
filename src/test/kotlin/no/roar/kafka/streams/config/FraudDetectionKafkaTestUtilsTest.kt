package no.roar.kafka.streams.config

import no.roar.kafka.streams.config.TestUtils.expectedFraudAlertList
import no.roar.kafka.streams.config.TestUtils.sendTestTransactionsUsingKafkaTemplate
import no.roar.kafka.streams.config.TestUtils.timeOffsetsForTwoHours
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

@SpringBootTest
@EmbeddedKafka(topics = ["transaction", "fraud-alert"])
class FraudDetectionKafkaTestUtilsTest {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Autowired
    lateinit var transactionKafkaTemplate: KafkaTemplate<String, Transaction>

    @Test
    fun `given list of transactions with combined values above threshold expect list of fraud alerts to be published`() {
        val fraudAlertConsumer = createFraudAlertConsumer()
        sendTestTransactionsUsingKafkaTemplate(transactionKafkaTemplate, timeOffsetsForTwoHours)

        val fraudAlertConsumerRecords = KafkaTestUtils.getRecords(fraudAlertConsumer)

        assertFalse(fraudAlertConsumerRecords.isEmpty)
        assertEquals(expectedFraudAlertList, fraudAlertConsumerRecords.map { it.value() })
    }

    private fun createFraudAlertConsumer(): Consumer<String, FraudAlert> =
        DefaultKafkaConsumerFactory<String, FraudAlert>(
            KafkaTestUtils.consumerProps(
                "test-group",
                "true",
                embeddedKafka
            ).apply {
                this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
                this[JsonDeserializer.VALUE_DEFAULT_TYPE] = FraudAlert::class.java.name
            }
        ).createConsumer().apply { subscribe(listOf("fraud-alert")) }
}