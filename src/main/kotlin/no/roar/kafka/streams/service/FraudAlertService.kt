package no.roar.kafka.streams.service

import no.roar.kafka.loggerFor
import no.roar.kafka.streams.model.FraudAlert
import org.springframework.stereotype.Service

@Service
class FraudAlertService {
    fun handleFraudAlert(fraudAlert: FraudAlert) {
        log.info("handleFraudAlert received: {}", fraudAlert)
    }

    companion object {
        private val log = loggerFor<FraudAlertService>()
    }
}