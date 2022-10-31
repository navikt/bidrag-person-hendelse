package no.nav.bidrag.person.hendelse.integrasjon.motta

import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.integrasjon.motta.Livshendelsemottak.Companion.SECURE_LOGGER
import no.nav.bidrag.person.hendelse.integrasjon.motta.Livshendelsemottak.Companion.log
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler.Companion.OPPLYSNINGSTYPE_PERSONIDENT
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class Identhendelsemottak(val livshendelsebehandler: Livshendelsebehandler) {

    @KafkaListener(
        groupId = "aktor-v2.bidrag",
        topics = ["pdl.aktor-v2"],
        id = "bidrag-person-hendelse.aktor-v2",
        idIsGroup = false,
        containerFactory = "kafkaIdenthendelseListenerContainerFactory"
    )
    fun listen(consumerRecord: ConsumerRecord<String, Aktor?>, ack: Acknowledgment) {
        try {
            log.info("Aktørhendelse mottatt")
            SECURE_LOGGER.info("Har mottatt ident-hendelse $consumerRecord")

            val aktør = consumerRecord.value()

            if (aktør == null) {
                log.warn("Tom aktør fra identhendelse")
                SECURE_LOGGER.warn("Tom aktør fra identhendelse med noekkel ${consumerRecord.key()}")
            }

            val gjeldendeAktørid: String =
                aktør?.identifikatorer?.filter {
                        ident -> ident.type == Type.FOLKEREGISTERIDENT && ident.gjeldende
                }?.single()?.idnummer.toString()

            val gjeldendeFolkeregisterident = aktør?.identifikatorer?.filter{
                ident -> ident.type == Type.AKTORID && ident.gjeldende
            }?.single()?.idnummer.toString()

            SECURE_LOGGER.info("Forbereder identhendelsemelding for person med gjeldende aktørid {} og gjeldende folkeregisterident {}",
            gjeldendeAktørid, gjeldendeFolkeregisterident);

            val livshendelse = Livshendelse.Builder()
                .hendelseid("pdl.aktor-v2.${consumerRecord.timestamp()}")
                .gjeldendeAktørid(gjeldendeAktørid)
                .gjeldendePersonident(gjeldendeFolkeregisterident)
                .opplysningstype(OPPLYSNINGSTYPE_PERSONIDENT)
                .build()

            livshendelsebehandler.prosesserNyHendelse(livshendelse);
        } catch (e: RuntimeException) {
            log.warn("Feil i prosessering av ident-hendelser", e)
            SECURE_LOGGER.warn("Feil i prosessering av ident-hendelser $consumerRecord", e)
            throw RuntimeException("Feil i prosessering av ident-hendelser")
        }
        ack.acknowledge()
    }

    companion object {

        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(Identhendelsemottak::class.java)
    }
}
