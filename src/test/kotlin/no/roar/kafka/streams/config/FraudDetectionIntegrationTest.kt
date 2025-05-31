package no.roar.kafka.streams.config

import no.roar.kafka.streams.config.TestUtils.expectedFraudAlert
import no.roar.kafka.streams.config.TestUtils.sendTestTransactionsUsingKafkaTemplate
import no.roar.kafka.streams.config.TestUtils.timeOffsetsForTwoHours
import no.roar.kafka.streams.model.Transaction
import no.roar.kafka.streams.service.FraudAlertService
import org.awaitility.Awaitility.waitAtMost
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration

@SpringBootTest
@EmbeddedKafka(topics = ["transaction", "fraud-alert"])
class FraudDetectionIntegrationTest {

    @Autowired
    lateinit var transactionKafkaTemplate: KafkaTemplate<String, Transaction>

    @MockitoBean
    lateinit var fraudAlertService: FraudAlertService

    @Test
    fun `given list of transactions with combined values above threshold expect 2 calls to handleFraudAlert`() {
        sendTestTransactionsUsingKafkaTemplate(transactionKafkaTemplate, timeOffsetsForTwoHours)

        waitAtMost(Duration.ofSeconds(10))
            .untilAsserted {
                verify(fraudAlertService, times(2)).handleFraudAlert(expectedFraudAlert)
            }
    }
}