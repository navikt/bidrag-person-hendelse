package no.nav.bidrag.person.hendelse.integrasjon.kontoregister

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.prosess.Kontoendringsbehandler
import no.nav.bidrag.person.hendelse.testdata.generererFødselsnummer
import no.nav.person.endringsmelding.v1.Endringsmelding
import no.nav.person.endringsmelding.v1.UtenlandskKontoInfo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.Optional

@ExtendWith(MockKExtension::class)
class KontoendringsmottakTest {
    @MockK
    lateinit var kontoendringsbehandler: Kontoendringsbehandler

    lateinit var kontoendringsmottak: Kontoendringsmottak

    @BeforeEach
    internal fun oppsett() {
        MockKAnnotations.init(this)
        clearAllMocks()
        kontoendringsmottak = Kontoendringsmottak(kontoendringsbehandler)
        every { kontoendringsbehandler.publisere(any()) } returns Unit
    }

    @Test
    fun `skal lagre kontoendring`() {
        // gitt
        val kontoeierFødselsnummer: String = generererFødselsnummer()
        val endringsmelding: Endringsmelding =
            Endringsmelding.newBuilder().setKontohaver(kontoeierFødselsnummer).setKontonummer("123")
                .setUtenlandskKontoInfo(
                    UtenlandskKontoInfo(),
                ).build()
        val cr =
            ConsumerRecord(
                "okonomi.kontoregister-person-endringsmelding.v2",
                1,
                229055,
                Instant.now().toEpochMilli(),
                TimestampType.CREATE_TIME,
                0,
                0,
                "2541031559331",
                endringsmelding,
                RecordHeaders(),
                Optional.of(0),
            )

        // hvis
        kontoendringsmottak.listen(endringsmelding, cr)

        // så
        val personident = slot<String>()
        verify(exactly = 1) {
            kontoendringsbehandler.publisere(capture(personident))
        }
    }
}
