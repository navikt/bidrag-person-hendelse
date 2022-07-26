package no.nav.bidrag.person.hendelse.konfigurasjon

import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@EnableKafka
@Configuration
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class KafkaConfig {

    @Bean
    fun kafkaIdenthendelseListenerContainerFactory(
        properties: KafkaProperties,
        kafkaRestartingErrorHandler: KafkaRestartingErrorHandler,
        environment: Environment
    ): ConcurrentKafkaListenerContainerFactory<String, Aktor> {
        properties.properties.put("specific.avro.reader", "true")
        val factory = ConcurrentKafkaListenerContainerFactory<String, Aktor>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.authExceptionRetryInterval = Duration.ofSeconds(2)
        factory.consumerFactory = DefaultKafkaConsumerFactory(
            properties.buildConsumerProperties().also {
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = OffsetResetStrategy.LATEST.toString().lowercase()
            }
        )
        factory.setCommonErrorHandler(kafkaRestartingErrorHandler)
        return factory
    }

    @Bean
    fun kafkaLeesahListenerContainerFactory(
        properties: KafkaProperties,
        kafkaRestartingErrorHandler: KafkaRestartingErrorHandler,
        environment: Environment
    ): ConcurrentKafkaListenerContainerFactory<Int, GenericRecord> {
        val factory = ConcurrentKafkaListenerContainerFactory<Int, GenericRecord>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.authExceptionRetryInterval = Duration.ofSeconds(2)
        factory.consumerFactory = DefaultKafkaConsumerFactory(
            properties.buildConsumerProperties().also {
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = OffsetResetStrategy.EARLIEST.toString().lowercase()
            }
        )
        factory.setCommonErrorHandler(kafkaRestartingErrorHandler)
        return factory
    }
}
