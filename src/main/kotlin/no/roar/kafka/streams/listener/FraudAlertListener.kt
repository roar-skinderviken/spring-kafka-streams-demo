package no.roar.kafka.streams.listener

import no.roar.kafka.loggerFor
import no.roar.kafka.streams.model.FraudAlert
import no.roar.kafka.streams.service.FraudAlertService
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
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
    fun listenForFraudAlert(fraudAlert: FraudAlert) {
        log.info("listenForFraudAlert received: {}", fraudAlert)
        fraudAlertService.handleFraudAlert(fraudAlert)
    }

    companion object {
        private val log = loggerFor<FraudAlertListener>()
    }
}