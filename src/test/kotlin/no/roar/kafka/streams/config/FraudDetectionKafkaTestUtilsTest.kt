package no.roar.kafka.streams.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.roar.kafka.streams.config.TestUtils.expectedFraudAlertList
import no.roar.kafka.streams.config.TestUtils.sendTestTransactionsUsingKafkaTemplate
import no.roar.kafka.streams.config.TestUtils.timeOffsetsForTwoHours
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

@SpringBootTest
@EmbeddedKafka(topics = ["transaction", "fraud-alert"])
class FraudDetectionKafkaTestUtilsTest(
    embeddedKafka: EmbeddedKafkaBroker,
    transactionKafkaTemplate: KafkaTemplate<String, Transaction>
) : BehaviorSpec({

    // TODO: Fix me
    xGiven("a configured Kafka fraud alert consumer") {
        val fraudAlertConsumer = createFraudAlertConsumer(embeddedKafka)

        When("transactions exceeding the threshold are sent to Kafka") {
            sendTestTransactionsUsingKafkaTemplate(transactionKafkaTemplate, timeOffsetsForTwoHours)

            Then("the fraud alert consumer receives the expected fraud alerts") {
                val fraudAlertRecords = KafkaTestUtils.getRecords(fraudAlertConsumer)

                fraudAlertRecords.shouldNotBeEmpty()

                val receivedFraudAlerts = fraudAlertRecords.map { it.value() }
                receivedFraudAlerts shouldBe expectedFraudAlertList
            }
        }
    }
}) {
    companion object {
        private fun createFraudAlertConsumer(embeddedKafka: EmbeddedKafkaBroker): Consumer<String, FraudAlert> =
            DefaultKafkaConsumerFactory<String, FraudAlert>(
                KafkaTestUtils.consumerProps(
                    embeddedKafka,
                    "test-group",
                    true,
                ).apply {
                    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonJsonDeserializer::class.java
                    this[JacksonJsonDeserializer.VALUE_DEFAULT_TYPE] = FraudAlert::class.java.name
                }
            ).createConsumer().apply { subscribe(listOf("fraud-alert")) }
    }
}