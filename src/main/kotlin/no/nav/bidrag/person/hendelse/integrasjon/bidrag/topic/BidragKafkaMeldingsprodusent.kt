package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.EntityManager
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.HendelseMottakerForAktor
import no.nav.bidrag.person.hendelse.database.Hendelsemottak
import no.nav.bidrag.person.hendelse.database.erAdresseendring
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.exception.PubliseringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.tilHendelseOpplysningstype
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.hendelseOjectmapper
import org.apache.kafka.common.KafkaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BidragKafkaMeldingsprodusent(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val databasetjeneste: Databasetjeneste,
    private val entityManager: EntityManager,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    fun publisereEndringsmelding(
        aktørid: String,
        personidenter: Set<String>,
        hendelse: HendelseMottakerForAktor? = null,
        opplysningstype: Endringsmelding.Opplysningstype? = null,
    ) {
        publisereMelding(aktørid, personidenter, hendelse, opplysningstype)
    }

    private fun Hendelsemottak.hentSivilstandsendring(): Endringsmelding.Sivilstandendring? {
        try {
            if (this.endringstype == Endringstype.OPPRETTET && this.opplysningstype == Livshendelse.Opplysningstype.SIVILSTAND_V1) {
                val hendelse: Livshendelse = objectMapper.readValue(this.hendelse)
                return hendelse.sivilstand?.let {
                    Endringsmelding.Sivilstandendring(
                        it.sivilstand,
                        it.bekreftelsesdato,
                        it.gyldigFraOgMedDato,
                    )
                }
            }
        } catch (e: Exception) {
            log.warn("Feil ved henting av sivilstandsendringer for hendelse ${this.hendelseid}: ${e.message}")
        }
        return null
    }

    private fun Hendelsemottak.hentAdresseendring(): Endringsmelding.Adresseendring? {
        try {
            if (this.endringstype == Endringstype.OPPRETTET && this.opplysningstype.erAdresseendring()) {
                val hendelse: Livshendelse = objectMapper.readValue(this.hendelse)
                return Endringsmelding.Adresseendring(
                    flyttedato = hendelse.flyttedato,
                    utflytting = hendelse.utflytting,
                    innflytting = hendelse.innflytting,
                    type =
                        hendelse.opplysningstype.tilHendelseOpplysningstype(),
                )
            }
        } catch (e: Exception) {
            log.warn("Feil ved henting av adresseendring for hendelse ${this.hendelseid}: ${e.message}")
        }
        return null
    }

    private fun publisereMelding(
        aktørid: String,
        personidenter: Set<String>,
        hendelse: HendelseMottakerForAktor? = null,
        opplysningstype: Endringsmelding.Opplysningstype? = null,
    ) {
        val melding =
            tilJson(
                Endringsmelding(
                    aktørid,
                    personidenter,
                    hendelse?.hendelsemottaksliste?.map {
                        Endringsmelding.Endring(
                            it.hentAdresseendring(),
                            it.hentSivilstandsendring(),
                            it.opplysningstype.tilHendelseOpplysningstype(),
                        )
                    } ?: listOf(
                        Endringsmelding.Endring(
                            opplysningstype = opplysningstype ?: Endringsmelding.Opplysningstype.UKJENT,
                        ),
                    ),
                ),
            )
        slog.info("Publiserer endringsmelding $melding for aktørid $aktørid")
        try {
            val future = kafkaTemplate.send(BIDRAG_PERSONHENDELSE_TOPIC, aktørid, melding)

            future.whenComplete { result, ex ->
                if (ex != null) {
                    log.warn("Publisering av melding til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
                    slog.warn(
                        "Publisering av melding for aktørid ${result.producerRecord.key()} til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.",
                    )
                    throw ex
                }
            }

            databasetjeneste.oppdaterePubliseringstidspunkt(aktørid)
        } catch (e: KafkaException) {
            // Fanger exception for å unngå at meldingsinnhold logges i åpen logg.
            slog.error("Publisering av melding for aktørid $aktørid feilet med feilmelding: ${e.message}")
            throw PubliseringFeiletException("Publisering av melding med nøkkel $aktørid til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
        }
    }

    companion object {
        val BIDRAG_PERSONHENDELSE_TOPIC = "bidrag.personhendelse.v1"
        private val log = LoggerFactory.getLogger(this::class.java)
        private val slog: Logger = LoggerFactory.getLogger("secureLogger")

        fun tilJson(endringsmelding: Endringsmelding): String = hendelseOjectmapper.writeValueAsString(endringsmelding)
    }
}
