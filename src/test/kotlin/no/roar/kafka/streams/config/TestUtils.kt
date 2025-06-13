package no.roar.kafka.streams.config

import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import org.apache.kafka.streams.TestInputTopic
import org.springframework.kafka.core.KafkaTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit

object TestUtils {

    // last offset is for releasing time window in tests
    val timeOffsetsForTwoHours = listOf(-3700L, -3699L, -3698L, -30L, -29L, -28L, 10L)

    private const val ACCOUNT_ID_IN_TEST = "~accountId1~"
    private const val TRANSACTION_AMOUNT_IN_TEST = 3020

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.HOURS)

    private val expectedFraudAlert = FraudAlert(ACCOUNT_ID_IN_TEST, TRANSACTION_AMOUNT_IN_TEST * 3)
    val expectedFraudAlertList = listOf(expectedFraudAlert, expectedFraudAlert)

    private fun sendTestTransactions(
        timeOffsets: List<Long>,
        sendTransactionFunc: (Transaction) -> Unit
    ) = timeOffsets
        .map { timeOffset ->
            Transaction(
                ACCOUNT_ID_IN_TEST,
                TRANSACTION_AMOUNT_IN_TEST,
                now.plusSeconds(timeOffset).toEpochMilli()
            )
        }
        .forEach(sendTransactionFunc)

    fun sendTestTransactionsUsingTestInputTopic(
        inputTopic: TestInputTopic<String, Transaction>,
        timeOffsets: List<Long>
    ) = sendTestTransactions(timeOffsets) { transaction ->
        inputTopic.pipeInput(
            transaction.accountId,
            transaction,
            transaction.timestamp
        )
    }

    fun sendTestTransactionsUsingKafkaTemplate(
        kafkaTemplate: KafkaTemplate<String, Transaction>,
        timeOffsets: List<Long>
    ) = sendTestTransactions(timeOffsets) { transaction ->
        kafkaTemplate.send(
            "transaction",
            0,
            transaction.timestamp,
            transaction.accountId,
            transaction
        )
    }
}