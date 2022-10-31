package no.nav.bidrag.person.hendelse.konsumere

import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.integrasjon.distribuere.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*
import kotlin.random.Random
import kotlin.random.nextUInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LivshendelsebehandlerTest {
    lateinit var mockMeldingsprodusent: Meldingsprodusent
    lateinit var service: Livshendelsebehandler
    lateinit var egenskaperWmq: Wmq

    @BeforeEach
    internal fun setUp() {
        mockMeldingsprodusent = mockk(relaxed = true)
        egenskaperWmq = mockk(relaxed = true)
        service = Livshendelsebehandler(egenskaperWmq, mockMeldingsprodusent)
        clearAllMocks()
    }

    @Test
    fun `Skal opprette VurderLivshendelseTask for dødsfallhendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse.Builder()
            .offset(Random.nextUInt().toLong())
            .gjeldendeAktørid("1234567890123")
            .hendelseid(hendelseId)
            .personidenter(listOf("12345678901", "1234567890123"))
            .endringstype(Livshendelsebehandler.OPPRETTET)
            .opplysningstype(Livshendelsebehandler.OPPLYSNINGSTYPE_DOEDSFALL)
            .dødsdato(LocalDate.now())
            .build()

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal opprette VurderLivshendelseTask for utflyttingshendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse.Builder()
            .offset(Random.nextUInt().toLong())
            .gjeldendeAktørid("1234567890123")
            .hendelseid(hendelseId)
            .personidenter(listOf("12345678901", "1234567890123"))
            .endringstype(Livshendelsebehandler.OPPRETTET)
            .opplysningstype(Livshendelsebehandler.OPPLYSNINGSTYPE_UTFLYTTING)
            .utflyttingsdato(LocalDate.now()).build()

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal opprette VurderLivshendelseTask for sivilstandhendelse GIFT`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse.Builder()
            .offset(Random.nextUInt().toLong())
            .gjeldendeAktørid("1234567890123")
            .hendelseid(hendelseId)
            .personidenter(listOf("12345678901", "1234567890123"))
            .endringstype(Livshendelsebehandler.OPPRETTET)
            .opplysningstype(Livshendelsebehandler.OPPLYSNINGSTYPE_SIVILSTAND)
            .sivilstand("GIFT")
            .sivilstandDato(LocalDate.of(2022, 2, 22))
            .build()

        service.prosesserNyHendelse(livshendelse)
        service.prosesserNyHendelse(livshendelse.copy(sivilstand = "UOPPGITT"))
    }

    @Test
    fun `Skal opprette MottaFødselshendelseTask med fnr på payload`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse.Builder()
            .offset(Random.nextUInt().toLong())
            .gjeldendeAktørid("1234567890123")
            .hendelseid(hendelseId)
            .personidenter(listOf("12345678901", "1234567890123"))
            .endringstype(Livshendelsebehandler.OPPRETTET)
            .opplysningstype(Livshendelsebehandler.OPPLYSNINGSTYPE_FOEDSEL)
            .fødselsdato(LocalDate.now())
            .fødeland("NOR")
            .build()

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal ignorere fødselshendelser utenfor norge`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse = Livshendelse.Builder()
            .offset(Random.nextUInt().toLong())
            .gjeldendeAktørid("1234567890123")
            .hendelseid(hendelseId)
            .personidenter(listOf("12345678901", "1234567890123"))
            .endringstype(Livshendelsebehandler.OPPRETTET)
            .opplysningstype(Livshendelsebehandler.OPPLYSNINGSTYPE_FOEDSEL)
            .fødselsdato(LocalDate.now())
            .fødeland("PDL")
            .build()

        service.prosesserNyHendelse(livshendelse)
        service.prosesserNyHendelse(livshendelse.copy(fødeland = "NOR"))
        service.prosesserNyHendelse(livshendelse.copy(fødeland = null))
    }

    @Test
    fun `Skal opprette MottaAnnullerFødselTask når endringstype er ANNULLERT`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse = Livshendelse.Builder()
            .offset(Random.nextUInt().toLong())
            .gjeldendeAktørid("1234567890123")
            .hendelseid(hendelseId)
            .personidenter(listOf("12345678901", "1234567890123"))
            .endringstype(Livshendelsebehandler.ANNULLERT)
            .opplysningstype(Livshendelsebehandler.OPPLYSNINGSTYPE_FOEDSEL)
            .fødselsdato(LocalDate.now())
            .fødeland("NOR")
            .tidligereHendelseid("ukjent")
            .build()

        service.prosesserNyHendelse(livshendelse)
    }
}
