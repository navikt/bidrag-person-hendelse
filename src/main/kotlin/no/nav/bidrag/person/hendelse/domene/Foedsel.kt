package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Foedsel(
    val foedeland: String? = null,
    val foedselsdato: LocalDate? = null,
)
