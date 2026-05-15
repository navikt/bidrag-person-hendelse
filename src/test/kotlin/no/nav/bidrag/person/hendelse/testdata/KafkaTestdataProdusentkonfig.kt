package no.nav.bidrag.person.hendelse.testdata

import no.nav.bidrag.person.hendelse.konfigurasjon.Kafkakonfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
@Profile("lokal")
class KafkaTestdataProdusentkonfig(
    val kafka: Kafkakonfig.Kafka,
) {
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val konfig = HashMap<String, Any>()
        konfig[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers as Any
        konfig[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java as Any
        konfig[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java as Any
        return DefaultKafkaProducerFactory((konfig as Map<String, Any>?)!!)
    }

    @Bean
    @Qualifier("testdata")
    fun kafkaTemplateTestdata(): KafkaTemplate<String, String>? = KafkaTemplate(producerFactory())
}
