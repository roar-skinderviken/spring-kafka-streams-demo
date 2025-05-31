package no.roar.kafka.streams.model

data class Transaction(
    val accountId: String,
    val amount: Int,
    val timestamp: Long = 0
)