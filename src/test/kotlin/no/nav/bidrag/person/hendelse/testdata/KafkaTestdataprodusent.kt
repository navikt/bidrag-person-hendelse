package no.nav.bidrag.person.hendelse.testdata

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate

@Profile("lokal")
class KafkaTestdataprodusent(
    @Qualifier("testdata") val kafkaTemplateTestdata: KafkaTemplate<String, String>
) {

    val topicLivshendelser: String = "aapen-person-pdl-leesah-v1"
    fun sendeMelding(melding: String) {
        kafkaTemplateTestdata.send(topicLivshendelser, melding);
    }
}