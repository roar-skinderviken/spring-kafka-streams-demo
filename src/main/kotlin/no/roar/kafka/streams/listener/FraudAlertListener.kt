package no.roar.kafka.streams.listener

import net.logstash.logback.argument.StructuredArguments.kv
import no.roar.kafka.loggerFor
import no.roar.kafka.logging.withLoggingContext
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.service.FraudAlertService
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
@Profile("!topology-test")
class FraudAlertListener(
    private val fraudAlertService: FraudAlertService
) {
    @KafkaListener(
        id = "fraudAlertListener",
        groupId = "fraud-alert-group",
        topics = ["fraud-alert"],
    )
    fun listenForFraudAlert(
        fraudAlert: FraudAlert,
        @Header(KafkaHeaders.RECEIVED_KEY) receivedKey: String,
    ) {
        withLoggingContext(
            "received-key" to receivedKey,
            "accountId" to fraudAlert.accountId
        ) {
            log.info("listenForFraudAlert received fraud alert: {}", kv("amount", fraudAlert.totalAmount))
            fraudAlertService.handleFraudAlert(fraudAlert)
        }
    }

    companion object {
        private val log = loggerFor<FraudAlertListener>()
    }
}