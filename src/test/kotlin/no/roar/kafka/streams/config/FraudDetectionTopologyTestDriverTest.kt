package no.roar.kafka.streams.config

import no.roar.kafka.streams.config.TestUtils.expectedFraudAlertList
import no.roar.kafka.streams.config.TestUtils.sendTestTransactions
import no.roar.kafka.streams.config.TestUtils.timeOffsetsForTwoHours
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.waitAtMost
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@SpringBootTest
@ActiveProfiles("test", "topology-test")
class FraudDetectionTopologyTestDriverTest {

    @Value("\${transaction-topic.name}")
    lateinit var inputTopicName: String

    @Value("\${fraud-alert-topic.name}")
    lateinit var outputTopicName: String

    @Autowired
    lateinit var streamsBuilder: StreamsBuilderFactoryBean

    lateinit var testDriver: TopologyTestDriver
    lateinit var inputTopic: TestInputTopic<String, Transaction>
    lateinit var outputTopic: TestOutputTopic<String, FraudAlert>

    @BeforeEach
    fun setup() {
        this.testDriver = TopologyTestDriver(
            streamsBuilder.topology,
            streamsBuilder.streamsConfiguration
        )

        this.inputTopic = testDriver.createInputTopic(
            inputTopicName,
            Serdes.String().serializer(),
            JsonSerde(Transaction::class.java).serializer()
        )

        this.outputTopic = testDriver.createOutputTopic(
            outputTopicName,
            Serdes.String().deserializer(),
            JsonSerde(FraudAlert::class.java).deserializer()
        )
    }

    @AfterEach
    fun tearDown() {
        testDriver.close()
    }

    @Test
    fun `given two transactions this hour and one transaction earlier same day expect no fraud alert to be published`() {
        sendTestTransactionsUsingTestInputTopic(listOf(-3698L, -30L, -29L, 10L))

        waitAtMost(Duration.ofSeconds(10))
            .pollDelay(Duration.ofSeconds(5))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted { assertTrue(outputTopic.isEmpty) }

        assertTrue(outputTopic.isEmpty)
    }

    @Test
    fun `given two groups of transactions withing same hours expect 2 fraud alerts to be published`() {
        sendTestTransactionsUsingTestInputTopic(timeOffsetsForTwoHours)

        waitAtMost(Duration.ofSeconds(10))
            .until { !outputTopic.isEmpty }

        assertThat(outputTopic.readValuesToList()).isEqualTo(expectedFraudAlertList)
    }

    private fun sendTestTransactionsUsingTestInputTopic(timeOffsets: List<Long>) {
        sendTestTransactions(timeOffsets) { transaction ->
            inputTopic.pipeInput(
                transaction.accountId,
                transaction,
                transaction.timestamp
            )
        }
    }
}