package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
open class SletteUtgåtteHendelser(
    open val databasetjeneste: Databasetjeneste,
    open val egenskaper: Egenskaper

) {
    private val MAKS_SETTSTØRRELSE: Int = 65000

    @Scheduled(cron = "\${kjøreplan.slette_hendelser}")
    @SchedulerLock(name = "slette_hendelser", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    open fun sletteUtgåtteHendelserFraDatabase() {
        var statusoppdateringFør = LocalDate.now().atStartOfDay().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong())

        log.info("Ser etter utgåtte livshendelser med siste statusoppdatering før $statusoppdateringFør som skal slettes fra databasen.")

        var kansellerteHendelser = databasetjeneste.henteHendelserider(Status.KANSELLERT, statusoppdateringFør)
        var overførteHendelser = databasetjeneste.henteHendelserider(Status.OVERFØRT, statusoppdateringFør)

        log.info("Fant ${kansellerteHendelser.size} kansellerte, og ${overførteHendelser.size} overførte hendelser som skal slettes fra databasen")

        if (kansellerteHendelser.size > MAKS_SETTSTØRRELSE) {
            log.info("Antall hendelser identifisert for sletting oversteg grensen på $MAKS_SETTSTØRRELSE.")
            var listeMedListeAvHendelseider = kansellerteHendelser.chunked(MAKS_SETTSTØRRELSE)
            listeMedListeAvHendelseider.forEach { databasetjeneste.sletteHendelser(it.toSet()) }
        } else {
            databasetjeneste.sletteHendelser(kansellerteHendelser)
        }

        log.info("Slettet ${kansellerteHendelser.size} kansellerte hendelser")

        databasetjeneste.sletteHendelser(overførteHendelser)
        log.info("Slettet ${overførteHendelser.size} overførte hendelser")
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(SletteUtgåtteHendelser::class.java)
    }
}
