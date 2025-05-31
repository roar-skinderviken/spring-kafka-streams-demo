package no.roar.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


inline fun <reified T : Any> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)

@SpringBootApplication
class KafkaStreamsDemoApplication

fun main(args: Array<String>) {
    runApplication<KafkaStreamsDemoApplication>(*args)
}
