package no.roar.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

inline fun <reified T : Any> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)

@SpringBootApplication
class KafkaStreamsDemoApplication

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    runApplication<KafkaStreamsDemoApplication>(*args)
}
