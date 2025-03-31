package no.nav.bidrag.person.hendelse.prosess

import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import no.nav.bidrag.transport.person.Identgruppe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Kontoendringsbehandler(
    val bidragPersonklient: BidragPersonklient,
    val bidragtopic: BidragKafkaMeldingsprodusent,
) {
    fun publisere(personidentKontoeier: String) {
        val alleIdenterKontoeier = bidragPersonklient.henteAlleIdenterForPerson(personidentKontoeier)
        val aktørid = alleIdenterKontoeier?.find { it.gruppe.equals(Identgruppe.AKTORID) }?.ident
        if (aktørid != null) {
            bidragtopic.publisereEndringsmelding(
                aktørid,
                alleIdenterKontoeier
                    .map {
                        it.ident
                    }.toSet(),
                opplysningstype = Endringsmelding.Opplysningstype.KONTOENDRING,
            )
        } else {
            log.warn("Aktørid null for kontoeier - kontoendring ble ikke publisert.")
            slog.warn("Aktørid null for kontoeier $personidentKontoeier - kontoendring ble ikke publisert")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        val slog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
