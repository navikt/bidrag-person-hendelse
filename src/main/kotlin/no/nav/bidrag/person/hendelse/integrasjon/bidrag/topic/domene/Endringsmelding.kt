package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene

import no.nav.bidrag.person.hendelse.domene.Livshendelse
import java.time.LocalDate

data class Endringsmelding(
    val aktørid: String,
    val personidenter: Set<String>,
    val endringer: List<Endring> = emptyList(),
) {
    data class Endring(
        val adresseendring: Adresseendring? = null,
        val sivilstandendring: Sivilstandendring? = null,
        val identendring: Identendring? = null,
        val opplysningstype: Opplysningstype = Opplysningstype.UKJENT,
        val endringstype: Endringstype = Endringstype.OPPRETTET,
    )

    data class Identendring(
        val identifikasjonsnummer: String? = null,
        val type: String? = null,
        val status: String? = null,
    )

    data class Sivilstandendring(
        val sivilstand: String? = null,
        val bekreftelsesdato: LocalDate? = null,
        val gyldigFraOgMedDato: LocalDate? = null,
    )

    data class Adresseendring(
        val type: Opplysningstype,
        val flyttedato: LocalDate? = null,
        val utflytting: Utflytting? = null,
        val innflytting: Innflytting? = null,
    )

    enum class Endringstype {
        OPPRETTET,
        KORRIGERT,
        ANNULLERT,
        OPPHOERT,
    }

    enum class Opplysningstype {
        ADRESSEBESKYTTELSE,
        BOSTEDSADRESSE,
        DOEDSFALL,
        FOEDSEL,
        FOLKEREGISTERIDENTIFIKATOR,
        INNFLYTTING_TIL_NORGE,
        KONTAKTADRESSE,
        NAVN,
        OPPHOLDSADRESSE,
        SIVILSTAND,
        UTFLYTTING_FRA_NORGE,
        VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
        KONTOENDRING,
        UKJENT,
    }

    data class Utflytting(
        val tilflyttingsland: String? = null,
        val tilflyttingsstedIUtlandet: String? = null,
        val utflyttingsdato: LocalDate? = null,
    )

    data class Innflytting(
        val fraflyttingsland: String? = null,
        val fraflyttingsstedIUtlandet: String? = null,
    )
}

fun Livshendelse.Opplysningstype.tilHendelseOpplysningstype() =
    when (this) {
        Livshendelse.Opplysningstype.ADRESSEBESKYTTELSE_V1 -> Endringsmelding.Opplysningstype.ADRESSEBESKYTTELSE
        Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1 -> Endringsmelding.Opplysningstype.BOSTEDSADRESSE
        Livshendelse.Opplysningstype.DOEDSFALL_V1 -> Endringsmelding.Opplysningstype.DOEDSFALL
        Livshendelse.Opplysningstype.FOEDSEL_V1 -> Endringsmelding.Opplysningstype.FOEDSEL
        Livshendelse.Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1 -> Endringsmelding.Opplysningstype.FOLKEREGISTERIDENTIFIKATOR
        Livshendelse.Opplysningstype.INNFLYTTING_TIL_NORGE -> Endringsmelding.Opplysningstype.INNFLYTTING_TIL_NORGE
        Livshendelse.Opplysningstype.KONTAKTADRESSE_V1 -> Endringsmelding.Opplysningstype.KONTAKTADRESSE
        Livshendelse.Opplysningstype.NAVN_V1 -> Endringsmelding.Opplysningstype.NAVN
        Livshendelse.Opplysningstype.OPPHOLDSADRESSE_V1 -> Endringsmelding.Opplysningstype.OPPHOLDSADRESSE
        Livshendelse.Opplysningstype.SIVILSTAND_V1 -> Endringsmelding.Opplysningstype.SIVILSTAND
        Livshendelse.Opplysningstype.UTFLYTTING_FRA_NORGE -> Endringsmelding.Opplysningstype.UTFLYTTING_FRA_NORGE
        Livshendelse.Opplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1 -> Endringsmelding.Opplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT
        else -> Endringsmelding.Opplysningstype.UKJENT
    }
