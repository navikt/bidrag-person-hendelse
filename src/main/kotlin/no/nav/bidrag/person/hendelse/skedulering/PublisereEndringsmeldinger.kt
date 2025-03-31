package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PublisereEndringsmeldinger(
    val bidragtopic: BidragKafkaMeldingsprodusent,
    val databasetjeneste: Databasetjeneste,
    val egenskaper: Egenskaper,
) {
    @Scheduled(cron = "\${publisere_personhendelser.kjøreplan}")
    @SchedulerLock(
        name = "publisere_personhendelser",
        lockAtLeastFor = "\${publisere_personhendelser.lås.min}",
        lockAtMostFor = "\${publisere_personhendelser.lås.max}",
    )
    fun identifisereOgPublisere() {
        // Hente aktør med personidenter til til personer med nylige endringer i personopplysninger
        val aktørerPersonopplysninger = databasetjeneste.hentePubliseringsklareHendelser()
        log.info("Fant ${aktørerPersonopplysninger.size} unike personer med nylige endringer i personopplysninger.")

        val subsetMedAktørider =
            aktørerPersonopplysninger.keys
                .take(
                    egenskaper.generelt.maksAntallMeldingerSomSendesTilBidragTopicOmGangen,
                ).toSet()

        if (subsetMedAktørider.size < aktørerPersonopplysninger.size) {
            log.info(
                "Begrenser antall meldinger som skal publiseres til ${subsetMedAktørider.size}",
            )
        }

        // Publisere melding til intern topic for samtlige personer med endringer
        subsetMedAktørider.forEach {
            val opplysninger = aktørerPersonopplysninger.getValue(it)
            bidragtopic.publisereEndringsmelding(it.aktorid, opplysninger.first, opplysninger.second)
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
