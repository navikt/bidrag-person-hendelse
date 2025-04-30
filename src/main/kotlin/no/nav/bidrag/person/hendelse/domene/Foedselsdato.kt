package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Foedselsdato(
    val foedselsdato: LocalDate? = null,
    val foedselsaar: Int? = null,
)
