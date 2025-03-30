package no.nav.bidrag.person.hendelse.prosess

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.testdata.generereAktørid
import no.nav.bidrag.person.hendelse.testdata.generererFødselsnummer
import no.nav.bidrag.person.hendelse.testdata.tilPersonidentDtoer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class KontoendringsbehandlerTest {
    @MockK
    lateinit var bidragKafkaMeldingsprodusent: BidragKafkaMeldingsprodusent

    @MockK
    lateinit var mockBidragPersonklient: BidragPersonklient

    lateinit var kontoendringsbehandler: Kontoendringsbehandler

    @BeforeEach
    internal fun oppsett() {
        kontoendringsbehandler = Kontoendringsbehandler(mockBidragPersonklient, bidragKafkaMeldingsprodusent)
        clearAllMocks()
    }

    @Test
    fun `skal publisere endringsmeling ved mottak av kontoendring`() {
        // gitt
        val kontoeierAktørid = generereAktørid()
        val kontoeierFødselsnummer = generererFødselsnummer()

        val personidentDtoer = tilPersonidentDtoer(setOf(kontoeierAktørid, kontoeierFødselsnummer))

        every { mockBidragPersonklient.henteAlleIdenterForPerson(kontoeierFødselsnummer) } returns personidentDtoer
        every { bidragKafkaMeldingsprodusent.publisereEndringsmelding(any(), any(), any(), any()) } returns Unit

        // hvis
        kontoendringsbehandler.publisere(kontoeierFødselsnummer)

        // så
        val kontoeier = slot<String>()
        verify(exactly = 1) {
            bidragKafkaMeldingsprodusent.publisereEndringsmelding(
                capture(kontoeier),
                any(),
                any(),
                any(),
            )
        }
    }
}
