package no.nav.bidrag.person.hendelse.skedulering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import no.nav.bidrag.person.hendelse.integrasjon.pdl.domene.Identgruppe
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import no.nav.bidrag.person.hendelse.testdata.TeststøtteMeldingsmottak
import no.nav.bidrag.person.hendelse.testdata.generereIdenter
import no.nav.bidrag.person.hendelse.testdata.tilPersonidentDtoer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class PublisereEndringmeldingerTest {

    @Autowired
    lateinit var teststøtteMeldingsmottak: TeststøtteMeldingsmottak

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @MockK
    lateinit var meldingsprodusent: BidragKafkaMeldingsprodusent

    @MockK
    lateinit var bidragPersonklient: BidragPersonklient

    lateinit var publisereEndringsmeldinger: PublisereEndringsmeldinger

    @BeforeEach
    fun initialisere() {
        MockKAnnotations.init(this)
        clearAllMocks()
        databasetjeneste.hendelsemottakDao.deleteAll()
        databasetjeneste.kontoendringDao.deleteAll()
        databasetjeneste.aktorDao.deleteAll()
        publisereEndringsmeldinger = PublisereEndringsmeldinger(
            meldingsprodusent,
            bidragPersonklient,
            databasetjeneste,
            databasetjeneste.egenskaper
        )
        every { meldingsprodusent.publisereEndringsmelding(any()) }
    }

    @Test
    fun `skal publisere endringsmelding for kontoendring med utløpt venteperiode etter mottak`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        val mottattTidspunkt = LocalDateTime.now()
            .minusMinutes(databasetjeneste.egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 1)

        val tidspunktSistPublisert = LocalDateTime.now()
            .minusHours(databasetjeneste.egenskaper.generelt.antallTimerSidenForrigePublisering.toLong() - 1)

        teststøtteMeldingsmottak.oppretteOgLagreKontoendring(aktør!!.ident, mottattTidspunkt, tidspunktSistPublisert)

        every { aktør.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }
    }

    @Test
    fun `skal ikke publisere endringsmelding for kontoendring for person med ikke utløpt venteperiode mellom publiseringer`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        var mottattTidspunktIVenteperiode = LocalDateTime.now()
            .minusMinutes(databasetjeneste.egenskaper.generelt.antallMinutterForsinketVideresending.toLong() - 1)
        val publsertTidspunktEtterVenteperiode = LocalDateTime.now()
            .minusHours(databasetjeneste.egenskaper.generelt.antallTimerSidenForrigePublisering.toLong() + 1)

        teststøtteMeldingsmottak.oppretteOgLagreKontoendring(
            aktør!!.ident,
            mottattTidspunktIVenteperiode,
            publsertTidspunktEtterVenteperiode
        )

        every { aktør.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 0) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }
    }

    @Test
    fun `skal publisere endringsmeldinger for personer med nylig oppdaterte personopplysninger`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        teststøtteMeldingsmottak.oppretteOgLagreHendelsemottak(personidentDtoer.map { it.ident })
        every { aktør?.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }

        endringsmelding.asClue {
            it.captured.aktørid shouldBe aktør?.ident
            it.captured.personidenter.size shouldBe personidentDtoer.size
            it.captured.personidenter shouldBe personidenter
        }
    }

    @Test
    fun `skal ikke publisere endringsmelding for samme person mer enn én gang innenfor en bestemt periode`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        personidenter.find { it.length == 13 }?.let { teststøtteMeldingsmottak.oppretteOgLagreKontoendring(it) }
        teststøtteMeldingsmottak.oppretteOgLagreHendelsemottak(personidentDtoer.map { it.ident })
        every { aktør?.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }

        endringsmelding.asClue {
            it.captured.aktørid shouldBe aktør?.ident
            it.captured.personidenter.size shouldBe personidentDtoer.size
            it.captured.personidenter shouldBe personidenter
        }
    }
}
