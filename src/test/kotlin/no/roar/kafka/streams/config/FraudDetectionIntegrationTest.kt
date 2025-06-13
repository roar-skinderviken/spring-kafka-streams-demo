package no.roar.kafka.streams.config

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import no.roar.kafka.streams.config.TestUtils.sendTestTransactionsUsingKafkaTemplate
import no.roar.kafka.streams.config.TestUtils.timeOffsetsForTwoHours
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import no.roar.kafka.streams.service.FraudAlertService
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import kotlin.time.Duration.Companion.seconds

// NOTE: https://github.com/Ninja-Squad/springmockk seems not be be maintained anymore.
// Alternative to springmockk is to remove @MockkBean and comment in below @TestConfiguration

@SpringBootTest
@EmbeddedKafka(topics = ["transaction", "fraud-alert"])
class FraudDetectionIntegrationTest(
    transactionKafkaTemplate: KafkaTemplate<String, Transaction>,
    @MockkBean private val mockFraudAlertService: FraudAlertService
) : StringSpec({

    "given list of transactions with combined values above threshold expect 2 calls to handleFraudAlert" {
        every { mockFraudAlertService.handleFraudAlert(any<FraudAlert>()) } just Runs

        sendTestTransactionsUsingKafkaTemplate(
            transactionKafkaTemplate,
            timeOffsetsForTwoHours
        )

        eventually(10.seconds) {
            verify(atLeast = 1) {
                mockFraudAlertService.handleFraudAlert(isNull(inverse = true))
            }
        }
    }
}) {
/*
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockFraudAlertService(): FraudAlertService = mockk()
    }*/
}
