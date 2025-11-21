package no.nav.bidrag.person.hendelse.integrasjon.kontoregister

import no.nav.bidrag.person.hendelse.prosess.Kontoendringsbehandler
import no.nav.person.endringsmelding.v1.Endringsmelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class Kontoendringsmottak(
    val kontoendringsbehandler: Kontoendringsbehandler,
) {
    @KafkaListener(
        groupId = "kontoregister-person-endringsmelding-v2.bidrag",
        topics = ["okonomi.kontoregister-person-endringsmelding.v2"],
        id = "bidrag-person-hendelse-kontoregister-person-endringsmelding-v2",
        idIsGroup = false,
    )
    fun listen(
        @Payload(required = false) endringsmelding: Endringsmelding?,
        cr: ConsumerRecord<String, Endringsmelding?>,
    ) {
        slog.info(
            "Kontoregisterendringsmelding mottatt: Record key={}, value={}, value={}",
            cr.key(),
            cr.value(),
            cr.offset(),
        )

        if (harGyldigFormat(endringsmelding)) {
            kontoendringsbehandler.publisere(endringsmelding?.kontohaver.toString())
            slog.info("Kontoendring publisert for kontoeier {}", endringsmelding?.kontohaver)
        }
    }

    fun harGyldigFormat(endringsmelding: Endringsmelding?): Boolean {
        if (endringsmelding == null) {
            log.warn("Innhold mangler i mottatt endringsmelding.")
            return false
        } else if (endringsmelding.kontohaver.isNullOrEmpty()) {
            log.warn("Kontohaver mangler i mottatt endringsmelding.")
            return false
        } else if (!harGylidgFormat(endringsmelding.kontohaver.toString())) {
            log.warn("Kontohavers personident har ikke gyldig format.")
            slog.warn("Kontohavers personident (${endringsmelding.kontohaver}) har ikke gyldig format.")
            return false
        }

        return true
    }

    fun harGylidgFormat(personident: String): Boolean = personident.isNotEmpty() && (personident.length == 11 || personident.length == 13)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        val slog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
