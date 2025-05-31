package no.roar.kafka.streams.config

import no.roar.kafka.loggerFor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer

@EnableKafkaStreams
@EnableKafka
@Configuration(proxyBeanMethods = false)
class KafkaConfig {

    @Bean
    fun configurer() = StreamsBuilderFactoryBeanConfigurer { builderFactoryBean ->
        builderFactoryBean.setStateListener { newState, oldState ->
            log.info("State transition from {} to {}", oldState, newState)
        }
    }

    companion object {
        private val log = loggerFor<KafkaConfig>()
    }
}