package no.roar.kafka.streams.config

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import no.roar.kafka.streams.config.TestUtils.expectedFraudAlertList
import no.roar.kafka.streams.config.TestUtils.sendTestTransactionsUsingTestInputTopic
import no.roar.kafka.streams.config.TestUtils.timeOffsetsForTwoHours
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.support.serializer.JacksonJsonSerde
import org.springframework.test.context.ActiveProfiles
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SpringBootTest
@ActiveProfiles("test", "topology-test")
class FraudDetectionTopologyTestDriverTest(
    @Value($$"${transaction-topic.name}") inputTopicName: String,
    @Value($$"${fraud-alert-topic.name}") outputTopicName: String,
    streamsBuilder: StreamsBuilderFactoryBean
) : BehaviorSpec({

    Given("a setup with test input- and output topics") {
        lateinit var testDriver: TopologyTestDriver
        lateinit var inputTopic: TestInputTopic<String, Transaction>
        lateinit var outputTopic: TestOutputTopic<String, FraudAlert>

        beforeContainer {
            testDriver = TopologyTestDriver(
                streamsBuilder.topology,
                streamsBuilder.streamsConfiguration
            )

            inputTopic = testDriver.createInputTopic(
                inputTopicName,
                Serdes.String().serializer(),
                JacksonJsonSerde(Transaction::class.java).serializer()
            )

            outputTopic = testDriver.createOutputTopic(
                outputTopicName,
                Serdes.String().deserializer(),
                JacksonJsonSerde(FraudAlert::class.java).deserializer()
            )
        }

        afterContainer {
            testDriver.close()
        }

        When("sending two transactions this hour and one transaction earlier same day") {
            sendTestTransactionsUsingTestInputTopic(inputTopic, listOf(-3698L, -30L, -29L, 10L))

            Then("expect no fraud alert to be published") {
                val config = eventuallyConfig {
                    duration = 10.seconds
                    initialDelay = 5.seconds
                    interval = 500.milliseconds
                }

                eventually(config) {
                    outputTopic.isEmpty.shouldBeTrue()
                }
            }
        }

        When("sending two groups of transactions withing same hours") {
            sendTestTransactionsUsingTestInputTopic(inputTopic, timeOffsetsForTwoHours)

            Then("expect 2 fraud alerts to be received") {
                eventually(10.seconds) {
                    outputTopic.isEmpty.shouldBeFalse()
                    outputTopic.readValuesToList() shouldBe expectedFraudAlertList
                }
            }
        }
    }
})