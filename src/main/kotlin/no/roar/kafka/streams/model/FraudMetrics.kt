package no.roar.kafka.streams.model

data class FraudMetrics(
    val transactionCount: Int = 0,
    val totalAmount: Int = 0
) {
    val shouldAlert: Boolean
        get() = transactionCount > 2 && totalAmount > FRAUD_THRESHOLD

    companion object {
        const val FRAUD_THRESHOLD = 5000
    }
}
