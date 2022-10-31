package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Livshendelse private constructor(

    val hendelseid: String?,
    val gjeldendeAktørid: String?,
    val offset: Long,
    val opplysningstype: String?,
    val endringstype: String?,
    val personidenter: List<String>?,
    val gjeldendePersonident: String?,
    val dødsdato: LocalDate?,
    val fødselsdato: LocalDate?,
    val fødeland: String?,
    val utflyttingsdato: LocalDate?,
    val tidligereHendelseid: String?,
    val sivilstand: String?,
    val sivilstandDato: LocalDate?
) {
    fun hentPersonidenter() = personidenter?.filter { it.length == 11 }

    data class Builder(
        var hendelseid: String? = null,
        var gjeldendeAktørid: String? = null,
        var offset: Long = 0L,
        var opplysningstype: String? = null,
        var endringstype: String? = null,
        var personidenter: List<String>? = emptyList(),
        var gjeldendePersonident: String? = null,
        var dødsdato: LocalDate? = null,
        var fødselsdato: LocalDate? = null,
        var fødeland: String? = null,
        var utflyttingsdato: LocalDate? = null,
        var tidligereHendelseid: String? = null,
        var sivilstand: String? = null,
        var sivilstandDato: LocalDate? = null
    ) {

        fun hendelseid(hendelseid: String) = apply { this.hendelseid = hendelseid }
        fun gjeldendeAktørid(aktørid: String) = apply { this.gjeldendeAktørid = aktørid }
        fun offset(offset: Long) = apply { this.offset = offset }
        fun opplysningstype(opplysningstype: String) = apply { this.opplysningstype = opplysningstype }
        fun endringstype(endringstype: String) = apply { this.endringstype = endringstype }
        fun personidenter(personidenter: List<String>?) = apply { this.personidenter = personidenter }
        fun gjeldendePersonident(ident: String?) = apply { this.gjeldendePersonident = ident }
        fun dødsdato(dødsdato: LocalDate?) = apply { this.dødsdato = dødsdato }
        fun fødselsdato(fødselsdato: LocalDate?) = apply { this.fødselsdato = fødselsdato }
        fun fødeland(fødeland: String?) = apply { this.fødeland = fødeland }
        fun utflyttingsdato(utflyttingsdato: LocalDate?) = apply { this.utflyttingsdato = utflyttingsdato }
        fun tidligereHendelseid(hendelseid: String?) = apply { this.tidligereHendelseid = hendelseid }
        fun sivilstand(sivilstand: String?) = apply { this.sivilstand = sivilstand }
        fun sivilstandDato(dato: LocalDate?) = apply { this.sivilstandDato = dato }
        fun build() = Livshendelse(
            hendelseid,
            gjeldendeAktørid,
            offset,
            opplysningstype,
            endringstype,
            personidenter,
            gjeldendePersonident,
            dødsdato,
            fødselsdato,
            fødeland,
            utflyttingsdato,
            tidligereHendelseid,
            sivilstand,
            sivilstandDato
        )

    }

}
