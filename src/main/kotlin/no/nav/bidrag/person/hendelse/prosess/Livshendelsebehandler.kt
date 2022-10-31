package no.nav.bidrag.person.hendelse.prosess

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.integrasjon.distribuere.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class Livshendelsebehandler(
    val egenskaperWmq: Wmq,
    val meldingsprodusent: Meldingsprodusent
) {

    fun prosesserNyHendelse(livshendelse: Livshendelse) {
        when (livshendelse.opplysningstype) {
            OPPLYSNINGSTYPE_DOEDSFALL -> behandleDoedsfallHendelse(livshendelse)
            OPPLYSNINGSTYPE_FOEDSEL -> behandleFoedselsHendelse(livshendelse)
            OPPLYSNINGSTYPE_UTFLYTTING -> behandleUtflyttingHendelse(livshendelse)
            OPPLYSNINGSTYPE_SIVILSTAND -> behandleSivilstandHendelse(livshendelse)
        }
    }

    private fun behandleDoedsfallHendelse(livshendelse: Livshendelse) {

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                if (livshendelse.dødsdato == null) {
                    log.error("Mangler dødsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                }

                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
            }

            else -> {
                logHendelse(livshendelse)
                logHendelse(livshendelse, "Ikke av type OPPRETTET. Dødsdato: ${livshendelse.dødsdato}")
            }
        }
    }

    private fun behandleFoedselsHendelse(livshendelse: Livshendelse) {
        when (livshendelse.endringstype) {
            OPPRETTET, KORRIGERT -> {
                logHendelse(livshendelse, "fødselsdato: ${livshendelse.fødselsdato}")
                val fødselsdato = livshendelse.fødselsdato
                if (fødselsdato == null) {
                    log.warn("Mangler fødselsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                } else if (erUnder6mnd(fødselsdato)) {
                    if (erUtenforNorge(livshendelse.fødeland)) {
                        log.info("Fødeland er ikke Norge. Ignorerer hendelse ${livshendelse.hendelseid}")
                    }
                }

                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
            }

            ANNULLERT -> {
                if (livshendelse.tidligereHendelseid == null) {
                    log.warn("Mottatt annuller fødsel uten tidligereHendelseId, hendelseId ${livshendelse.hendelseid}")
                }
            }

            else -> {
                logHendelse(livshendelse)
            }
        }
    }

    private fun oppretteGson(): Gson {
        var gsonbuilder = GsonBuilder()
        gsonbuilder.registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter().nullSafe())
        var gson = gsonbuilder.create()
        return gson
    }

    private fun behandleUtflyttingHendelse(livshendelse: Livshendelse) {

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                logHendelse(livshendelse, "utflyttingsdato: ${livshendelse.utflyttingsdato}")
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
            }

            else -> {
                logHendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }
    }

    private fun behandleSivilstandHendelse(livshendelse: Livshendelse) {

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                logHendelse(livshendelse, "sivilstandDato: ${livshendelse.sivilstandDato}")
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
            }

            else -> {
                logHendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }
    }

    private fun logHendelse(livshendelse: Livshendelse, ekstraInfo: String = "") {
        log.info(
            "person-pdl-leesah melding mottatt: " +
                    "hendelseId: ${livshendelse.hendelseid} " +
                    "offset: ${livshendelse.offset}, " +
                    "opplysningstype: ${livshendelse.opplysningstype}, " +
                    "aktørid: ${livshendelse.gjeldendeAktørid}, " +
                    "endringstype: ${livshendelse.endringstype}, $ekstraInfo"
        )
    }

    private fun erUnder6mnd(fødselsDato: LocalDate): Boolean {
        return LocalDate.now().isBefore(fødselsDato.plusMonths(6))
    }

    private fun erUtenforNorge(fødeland: String?): Boolean {
        return when (fødeland) {
            null, "NOR" -> false
            else -> true
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Livshendelsebehandler::class.java)
        const val OPPRETTET = "OPPRETTET"
        const val KORRIGERT = "KORRIGERT"
        const val ANNULLERT = "ANNULLERT"
        const val OPPLYSNINGSTYPE_DOEDSFALL = "DOEDSFALL_V1"
        const val OPPLYSNINGSTYPE_FOEDSEL = "FOEDSEL_V1"
        const val OPPLYSNINGSTYPE_UTFLYTTING = "UTFLYTTING_FRA_NORGE"
        const val OPPLYSNINGSTYPE_SIVILSTAND = "SIVILSTAND_V1"
        const val OPPLYSNINGSTYPE_PERSONIDENT = "PERSONIDENT"
    }
}
