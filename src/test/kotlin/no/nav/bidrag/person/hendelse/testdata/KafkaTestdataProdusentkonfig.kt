package no.nav.bidrag.person.hendelse.testdata

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@Configuration
@Profile("lokal")
@ConfigurationPropertiesScan
open class KafkaTestdataProdusentkonfig(
  val kafkaegenskaper: Kafka
) {

    @ConstructorBinding
    @ConfigurationProperties("spring.kafka")
    data class Kafka(
        val bootstrapServers: String
    )

    @Bean
    open fun producerFactory(): ProducerFactory<String, String> {
        val konfig = HashMap<String, Object>()
        konfig[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaegenskaper.bootstrapServers as Object
        konfig[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java as Object
        konfig[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java as Object
        return DefaultKafkaProducerFactory(konfig as Map<String, Any>?)
    }

    @Bean
    @Qualifier("testdata")
    open fun kafkaTemplateTestdata(): KafkaTemplate<String?, String?>? {
        return KafkaTemplate(producerFactory())
    }
}