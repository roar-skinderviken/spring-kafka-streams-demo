package no.roar.kafka.streams.config

import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.model.Transaction
import org.springframework.kafka.core.KafkaTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit

object TestUtils {

    // last offset is for releasing time window in tests
    val timeOffsetsForTwoHours = listOf(-3700L, -3699L, -3698L, -30L, -29L, -28L, 10L)

    private const val ACCOUNT_ID_IN_TEST = "~accountId1~"
    private const val TRANSACTION_AMOUNT_IN_TEST = 3020

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.HOURS)
    private fun transactionInTest(timestamp: Long) =
        Transaction(ACCOUNT_ID_IN_TEST, TRANSACTION_AMOUNT_IN_TEST, timestamp)

    val expectedFraudAlert = FraudAlert(ACCOUNT_ID_IN_TEST, TRANSACTION_AMOUNT_IN_TEST * 3)
    val expectedFraudAlertList = listOf(expectedFraudAlert, expectedFraudAlert)

    fun sendTestTransactions(
        timeOffsets: List<Long>,
        sendTransactionFunc: (Transaction) -> Unit
    ) = timeOffsets
        .map { timeOffset -> transactionInTest(now.plusSeconds(timeOffset).toEpochMilli()) }
        .forEach(sendTransactionFunc)

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