package no.nav.bidrag.person.hendelse.integrasjon.pdl

import no.nav.bidrag.person.hendelse.domene.Foedsel
import no.nav.bidrag.person.hendelse.domene.Foedselsdato
import no.nav.bidrag.person.hendelse.domene.Folkeregisteridentifikator
import no.nav.bidrag.person.hendelse.domene.Innflytting
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Navn
import no.nav.bidrag.person.hendelse.domene.OriginaltNavn
import no.nav.bidrag.person.hendelse.domene.Sivilstand
import no.nav.bidrag.person.hendelse.domene.Utflytting
import no.nav.bidrag.person.hendelse.domene.VergeEllerFremtidsfullmakt
import no.nav.bidrag.person.hendelse.domene.VergeEllerFullmektig
import no.nav.bidrag.person.hendelse.exception.HendelsemottakException
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.bostedsadresse.Bostedsadresse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.kontaktadresse.Kontaktadresse
import no.nav.person.pdl.leesah.oppholdsadresse.Oppholdsadresse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class Livshendelsemottak(
    val livshendelsebehandler: Livshendelsebehandler,
) {
    object MdcKonstanter {
        const val MDC_KALLID = "id-kall"
    }

    @KafkaListener(
        groupId = "leesah-v1.bidrag",
        topics = ["pdl.leesah-v1"],
        id = "bidrag-person-hendelse.leesah-v1",
        idIsGroup = false,
        containerFactory = "kafkaLeesahListenerContainerFactory",
    )
    fun listen(
        @Payload personhendelse: Personhendelse,
        cr: ConsumerRecord<String, Personhendelse>,
    ) {
        log.info("Livshendelse med hendelseid {} mottatt.", personhendelse.hendelseId)
        slog.info("Har mottatt leesah-hendelse $cr")

        val opplysningstype = konvertereOpplysningstype(personhendelse.opplysningstype)

        if (Livshendelse.Opplysningstype.IKKE_STØTTET == opplysningstype) {
            log.info("Mottok opplysningstype som ikke støttes av løsningen - avbryter videre prosessering.")
            slog.info("Mottok opplysningstype som ikke støttes av løsningen - avbryter videre prosessering. $personhendelse")
            return
        }

        if (personhendelse.personidenter.isNullOrEmpty()) {
            log.warn("Mottok hendelse uten personidenter - avbryter videre prosessering")
            return
        }

        try {
            personhendelse.personidenter?.first { it.length == 13 }
        } catch (nsee: NoSuchElementException) {
            log.warn("Mottok hendelse uten aktørid - avbryter videre prosessering")
            slog.warn(
                "Fant ikke aktørid i hendelse med hendelseid: " +
                    "${personhendelse.hendelseId} og personidenter: {${personhendelse.personidenter}}",
            )
            return
        }

        val livshendelse =
            Livshendelse(
                personhendelse.hendelseId.toString(),
                opplysningstype,
                konvertereEndringstype(personhendelse.endringstype),
                personhendelse.personidenter
                    ?.stream()
                    ?.map(CharSequence::toString)!!
                    .collect(Collectors.toList()),
                personhendelse.personidenter.first { it.length == 13 }.toString(),
                LocalDateTime.ofInstant(personhendelse.opprettet, ZoneId.systemDefault()),
                personhendelse.tidligereHendelseId?.toString(),
                henteDødsdato(personhendelse.doedsfall),
                henteFlyttedato(
                    personhendelse.bostedsadresse,
                    personhendelse.kontaktadresse,
                    personhendelse.oppholdsadresse,
                ),
                henteFolkeregisteridentifikator(personhendelse.folkeregisteridentifikator),
                henteFødsel(personhendelse.foedsel),
                henteFødselsdato(personhendelse.foedselsdato),
                henteInnflytting(personhendelse.innflyttingTilNorge),
                henteNavn(personhendelse.navn),
                henteUtflytting(personhendelse.utflyttingFraNorge),
                henteSivilstand(personhendelse.sivilstand),
                henteVerge(personhendelse.vergemaalEllerFremtidsfullmakt),
                henteAdressebeskyttelse(personhendelse.adressebeskyttelse),
                cr.offset(),
                personhendelse.master.toString(),
            )

        try {
            MDC.put(MdcKonstanter.MDC_KALLID, livshendelse.hendelseid)
            livshendelsebehandler.prosesserNyHendelse(livshendelse)
        } catch (e: RuntimeException) {
            slog.error("Feil i prosessering av leesah-hendelse", e)
            throw RuntimeException("Feil i prosessering av leesah-hendelse")
        } finally {
            MDC.clear()
        }
    }

    private fun konvertereOpplysningstype(pdlOpplysningstype: CharSequence?): Livshendelse.Opplysningstype =
        try {
            Livshendelse.Opplysningstype.valueOf(pdlOpplysningstype.toString())
        } catch (iae: IllegalArgumentException) {
            log.info(
                "Mottok livshendelse med opplysningstype ({}) fra PDL. Denne ignoreres av løsningen.",
                pdlOpplysningstype.toString(),
            )
            Livshendelse.Opplysningstype.IKKE_STØTTET
        }

    private fun konvertereEndringstype(pdlEndringstype: Endringstype?): no.nav.bidrag.person.hendelse.domene.Endringstype {
        if (pdlEndringstype == null) {
            log.error("Endringstype i mottatt melding var null. Avbryter prosessering")
            throw HendelsemottakException("Endringstype i mottatt melding var null!")
        } else {
            try {
                return no.nav.bidrag.person.hendelse.domene.Endringstype
                    .valueOf(pdlEndringstype.name)
            } catch (iae: IllegalArgumentException) {
                log.error("Mottok ukjent endringstype ({}) fra PDL", pdlEndringstype.name)
                iae.printStackTrace()
                throw HendelsemottakException("Ukjent endringstype: $pdlEndringstype")
            }
        }
    }

    private fun henteAdressebeskyttelse(adressebeskyttelse: Adressebeskyttelse?): Livshendelse.Gradering =
        if (adressebeskyttelse == null) {
            Livshendelse.Gradering.UGRADERT
        } else {
            Livshendelse.Gradering.valueOf(adressebeskyttelse.gradering.name)
        }

    private fun henteDødsdato(doedsfall: Doedsfall?): LocalDate? = doedsfall?.doedsdato

    private fun henteFlyttedato(
        bostedsadresse: Bostedsadresse?,
        kontaktadresse: Kontaktadresse?,
        oppholdsadresse: Oppholdsadresse?,
    ): LocalDate? =
        if (bostedsadresse == null && kontaktadresse == null && oppholdsadresse == null) {
            null
        } else {
            if (bostedsadresse?.angittFlyttedato != null) {
                bostedsadresse.angittFlyttedato
            } else if (erKontaktadresseEndret(kontaktadresse) || erOppholdsadresseEndret(oppholdsadresse)) {
                LocalDate.now()
            } else {
                null
            }
        }

    private fun erKontaktadresseEndret(kontaktadresse: Kontaktadresse?): Boolean =
        kontaktadresse != null &&
            (
                kontaktadresse.vegadresse != null ||
                    kontaktadresse.postadresseIFrittFormat != null ||
                    kontaktadresse.postboksadresse != null ||
                    kontaktadresse.postadresseIFrittFormat != null ||
                    kontaktadresse.utenlandskAdresse != null
            )

    private fun erOppholdsadresseEndret(oppholdsadresse: Oppholdsadresse?): Boolean =
        oppholdsadresse != null &&
            (
                oppholdsadresse.utenlandskAdresse != null ||
                    oppholdsadresse.matrikkeladresse != null ||
                    oppholdsadresse.vegadresse != null
            )

    private fun henteFolkeregisteridentifikator(
        folkeregisteridentifikator: no.nav.person.pdl.leesah.folkeregisteridentifikator.Folkeregisteridentifikator?,
    ): Folkeregisteridentifikator? =
        if (folkeregisteridentifikator == null) {
            null
        } else {
            Folkeregisteridentifikator(
                folkeregisteridentifikator.identifikasjonsnummer?.toString(),
                folkeregisteridentifikator.type?.toString(),
                folkeregisteridentifikator.status?.toString(),
            )
        }

    private fun henteFødsel(foedsel: no.nav.person.pdl.leesah.foedsel.Foedsel?): Foedsel? =
        if (foedsel == null) {
            null
        } else {
            Foedsel(foedsel.foedeland?.toString(), foedsel.foedselsdato)
        }

    private fun henteFødselsdato(foedselsdato: no.nav.person.pdl.leesah.foedselsdato.Foedselsdato?): Foedselsdato? =
        if (foedselsdato == null) {
            null
        } else {
            Foedselsdato(foedselsdato.foedselsdato, foedselsdato.foedselsaar)
        }

    private fun henteInnflytting(innflytting: no.nav.person.pdl.leesah.innflytting.InnflyttingTilNorge?): Innflytting? =
        if (innflytting == null) {
            null
        } else {
            Innflytting(innflytting.fraflyttingsland?.toString(), innflytting.fraflyttingsstedIUtlandet?.toString())
        }

    private fun henteNavn(navn: no.nav.person.pdl.leesah.navn.Navn?): Navn? =
        if (navn == null) {
            null
        } else {
            val originaltNavn =
                OriginaltNavn(
                    navn.originaltNavn?.fornavn?.toString(),
                    navn.originaltNavn?.mellomnavn?.toString(),
                    navn.originaltNavn?.etternavn?.toString(),
                )
            Navn(
                navn.fornavn?.toString(),
                navn.mellomnavn?.toString(),
                navn.etternavn?.toString(),
                originaltNavn,
                navn.gyldigFraOgMed,
            )
        }

    private fun henteUtflytting(utflytting: no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge?): Utflytting? =
        if (utflytting == null) {
            null
        } else {
            Utflytting(
                utflytting.tilflyttingsland?.toString(),
                utflytting.tilflyttingsstedIUtlandet?.toString(),
                utflytting.utflyttingsdato,
            )
        }

    private fun henteSivilstand(sivilstand: no.nav.person.pdl.leesah.sivilstand.Sivilstand?): Sivilstand? =
        if (sivilstand == null) {
            null
        } else {
            Sivilstand(sivilstand.type?.toString(), sivilstand.bekreftelsesdato, sivilstand.gyldigFraOgMed)
        }

    private fun henteVerge(verge: no.nav.person.pdl.leesah.verge.VergemaalEllerFremtidsfullmakt?): VergeEllerFremtidsfullmakt? =
        if (verge == null) {
            null
        } else {
            val vergeEllerFullmektig =
                VergeEllerFullmektig(
                    verge.vergeEllerFullmektig?.motpartsPersonident?.toString(),
                    verge.vergeEllerFullmektig?.omfang.toString(),
                    verge.vergeEllerFullmektig?.omfangetErInnenPersonligOmraade,
                )
            VergeEllerFremtidsfullmakt(verge.type?.toString(), verge.embete?.toString(), vergeEllerFullmektig)
        }

    companion object {
        val slog: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
