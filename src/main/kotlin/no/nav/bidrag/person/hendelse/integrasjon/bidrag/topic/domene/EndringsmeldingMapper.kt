package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene

import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.transport.person.hendelse.Endringsmelding

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
