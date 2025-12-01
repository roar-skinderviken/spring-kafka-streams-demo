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
import java.time.Duration

@SpringBootTest
@EmbeddedKafka(topics = ["transaction", "fraud-alert"])
class FraudDetectionKafkaTestUtilsTest(
    embeddedKafka: EmbeddedKafkaBroker,
    transactionKafkaTemplate: KafkaTemplate<String, Transaction>
) : BehaviorSpec({

    Given("a configured Kafka fraud alert consumer") {
        val fraudAlertConsumer = createFraudAlertConsumer(embeddedKafka)

        When("transactions exceeding the threshold are sent to Kafka") {
            sendTestTransactionsUsingKafkaTemplate(transactionKafkaTemplate, timeOffsetsForTwoHours)

            Then("the fraud alert consumer receives the expected fraud alerts") {
                val receivedFraudAlerts =
                    waitForFraudAlerts(
                        consumer = fraudAlertConsumer,
                        expectedCount = expectedFraudAlertList.size,
                        timeoutSeconds = 10
                    )

                receivedFraudAlerts.shouldNotBeEmpty()

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

        fun waitForFraudAlerts(
            consumer: Consumer<String, FraudAlert>,
            expectedCount: Int,
            timeoutSeconds: Long = 10
        ): List<FraudAlert> {

            val deadline = System.currentTimeMillis() + timeoutSeconds * 1_000
            val results = mutableListOf<FraudAlert>()

            while (System.currentTimeMillis() < deadline && results.size < expectedCount) {
                val polled = consumer.poll(Duration.ofMillis(500))
                polled.forEach { results.add(it.value()) }
            }

            return results
        }
    }
}