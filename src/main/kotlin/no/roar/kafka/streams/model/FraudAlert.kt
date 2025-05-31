package no.roar.kafka.streams.model

data class FraudAlert(
    val accountId: String,
    val totalAmount: Int
)